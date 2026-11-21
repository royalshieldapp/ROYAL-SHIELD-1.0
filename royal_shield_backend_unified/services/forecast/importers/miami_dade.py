"""Strict importer for approved Miami-Dade aggregate observation files."""

import csv
import hashlib
from dataclasses import dataclass
from datetime import date, datetime, timezone
from pathlib import Path

import h3
from sqlalchemy import insert, select
from sqlalchemy.dialects.postgresql import insert as postgresql_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from services.forecast.models import ForecastObservation, IngestionRun


APPROVED_COLUMNS = {
    "observed_date",
    "district",
    "police_grid",
    "h3_cell",
    "event_type",
    "event_count",
    "source",
    "source_dataset",
    "source_record_type",
}
REQUIRED_COLUMNS = {"observed_date", "event_type", "event_count"}
APPROVED_SOURCE_DATASETS = {
    ("miami_dade", "pcb_aggregate"),
    ("Miami-Dade Sheriff Office Internal Affairs", "PCB_Report"),
}
SAFE_VALIDATION_REASON = "One or more aggregate rows failed validation."
SAFE_CONFLICT_REASON = "One or more aggregate rows failed validation or conflicted."
SAFE_PROVENANCE_REASON = "Embedded source provenance did not match the approved import."
OBSERVATION_IDENTITY_COLUMNS = (
    "source",
    "source_dataset",
    "observed_date",
    "district",
    "police_grid",
    "h3_cell",
    "event_type",
)


class DisallowedColumnError(ValueError):
    """Raised before reading rows when a file contains non-aggregate columns."""


class UnsupportedSourceError(ValueError):
    """Raised when provenance is outside the approved source allowlist."""


class SourceProvenanceError(UnsupportedSourceError):
    """Raised when file provenance conflicts with the approved CLI contract."""


@dataclass(frozen=True)
class ImportResult:
    run_id: int
    accepted: int
    rejected: int
    deduplicated: int
    source_sha256: str

    @property
    def accepted_rows(self) -> int:
        """Compatibility alias for the original importer callers."""

        return self.accepted

    @property
    def rejected_rows(self) -> int:
        """Compatibility alias for the original importer callers."""

        return self.rejected

    @property
    def updated_rows(self) -> int:
        """Duplicates are intentionally never applied as updates."""

        return 0


def validate_source_dataset(source: str, dataset: str) -> tuple[str, str]:
    """Return stripped approved provenance without exposing caller input."""

    normalized = (source.strip(), dataset.strip())
    if normalized not in APPROVED_SOURCE_DATASETS:
        raise UnsupportedSourceError("source and dataset are not approved")
    return normalized


def is_valid_h3_cell(value: str) -> bool:
    """Validate one H3 index using the installed H3 implementation."""

    try:
        return bool(h3.h3_is_valid(value))
    except (TypeError, ValueError):
        return False


def _resolve_provenance(
    row: dict[str, str | None], source: str | None, dataset: str | None
) -> tuple[str, str]:
    if source is not None and "source" in row:
        if (row.get("source") or "").strip() != source.strip():
            raise SourceProvenanceError(SAFE_PROVENANCE_REASON)
    if dataset is not None and "source_dataset" in row:
        if (row.get("source_dataset") or "").strip() != dataset.strip():
            raise SourceProvenanceError(SAFE_PROVENANCE_REASON)
    row_source = source if source is not None else (row.get("source") or "")
    row_dataset = dataset if dataset is not None else (row.get("source_dataset") or "")
    return validate_source_dataset(row_source, row_dataset)


def _insert_observation_atomic(
    session: Session,
    key: dict[str, object],
    event_count: int,
) -> tuple[str, int]:
    """Atomically insert one identity or classify the committed conflicting row."""

    values = {**key, "event_count": event_count}
    dialect = session.get_bind().dialect
    inserted = False
    if dialect.name == "postgresql":
        statement = (
            postgresql_insert(ForecastObservation)
            .values(**values)
            .on_conflict_do_nothing(index_elements=OBSERVATION_IDENTITY_COLUMNS)
            .returning(ForecastObservation.id)
        )
        inserted = session.scalar(statement) is not None
    elif dialect.name == "sqlite":
        statement = (
            sqlite_insert(ForecastObservation)
            .values(**values)
            .on_conflict_do_nothing(index_elements=OBSERVATION_IDENTITY_COLUMNS)
        )
        if dialect.insert_returning:
            inserted = (
                session.scalar(statement.returning(ForecastObservation.id)) is not None
            )
        else:
            inserted = session.execute(statement).rowcount == 1
    else:
        try:
            with session.begin_nested():
                session.execute(insert(ForecastObservation).values(**values))
            inserted = True
        except IntegrityError:
            inserted = False

    if inserted:
        return "accepted", event_count
    stored_count = session.scalar(
        select(ForecastObservation.event_count).filter_by(**key)
    )
    if stored_count is None:
        raise RuntimeError("observation conflict could not be resolved")
    if stored_count == event_count:
        return "deduplicated", stored_count
    return "conflict", stored_count


def _persist_failed_provenance_run(
    session: Session,
    provenance: tuple[str, str],
    started_at: datetime,
    source_sha256: str,
) -> None:
    session.add(
        IngestionRun(
            source=provenance[0],
            source_dataset=provenance[1],
            started_at=started_at,
            completed_at=datetime.now(timezone.utc),
            status="FAILED",
            source_sha256=source_sha256,
            accepted_count=0,
            rejected_count=0,
            deduplicated_count=0,
            safe_reason=SAFE_PROVENANCE_REASON,
        )
    )
    session.commit()


def import_aggregate_csv(
    session: Session,
    path: Path,
    source: str | None = None,
    dataset: str | None = None,
    *,
    dry_run: bool = False,
) -> ImportResult:
    """Import approved aggregates and persist a privacy-safe ingestion audit."""

    path = Path(path)
    if (source is None) != (dataset is None):
        raise UnsupportedSourceError("source and dataset must be provided together")
    explicit_provenance = (
        validate_source_dataset(source, dataset)
        if source is not None and dataset is not None
        else None
    )
    source_bytes = path.read_bytes()
    source_sha256 = hashlib.sha256(source_bytes).hexdigest()
    started_at = datetime.now(timezone.utc)

    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        headers = set(reader.fieldnames or [])
        unsupported = headers - APPROVED_COLUMNS
        missing = REQUIRED_COLUMNS - headers
        if unsupported:
            raise DisallowedColumnError("unsupported columns are present")
        if missing:
            raise ValueError("file is missing required aggregate columns")
        if explicit_provenance is None and not {"source", "source_dataset"} <= headers:
            raise UnsupportedSourceError("source and dataset provenance is required")

        transaction = session.begin_nested() if dry_run else None
        accepted = 0
        rejected = 0
        deduplicated = 0
        conflicted = False
        run_provenance = explicit_provenance
        batch_counts: dict[tuple[object, ...], int] = {}
        try:
            for row in reader:
                try:
                    if None in row:
                        raise ValueError("aggregate row contains extra values")
                    row_provenance = _resolve_provenance(row, source, dataset)
                    if run_provenance is None:
                        run_provenance = row_provenance
                    if row_provenance != run_provenance:
                        raise SourceProvenanceError(SAFE_PROVENANCE_REASON)

                    observed_date = date.fromisoformat((row.get("observed_date") or "").strip())
                    event_count = int((row.get("event_count") or "").strip())
                    if event_count < 0:
                        raise ValueError("event_count must be non-negative")
                    event_type = (row.get("event_type") or "").strip()
                    if not event_type:
                        raise ValueError("event_type is required")

                    h3_cell = (row.get("h3_cell") or "").strip().lower() or "UNKNOWN"
                    if h3_cell != "UNKNOWN" and not is_valid_h3_cell(h3_cell):
                        raise ValueError("h3_cell is invalid")
                    key = {
                        "source": row_provenance[0],
                        "source_dataset": row_provenance[1],
                        "observed_date": observed_date,
                        "district": (row.get("district") or "").strip() or "UNKNOWN",
                        "police_grid": (row.get("police_grid") or "").strip() or "UNKNOWN",
                        "h3_cell": h3_cell,
                        "event_type": event_type,
                    }
                    identity = tuple(key.values())
                    batch_count = batch_counts.get(identity)
                    if batch_count is not None:
                        if batch_count == event_count:
                            deduplicated += 1
                        else:
                            rejected += 1
                            conflicted = True
                        continue
                    outcome, stored_count = _insert_observation_atomic(
                        session, key, event_count
                    )
                    batch_counts[identity] = stored_count
                    if outcome == "accepted":
                        accepted += 1
                    elif outcome == "deduplicated":
                        deduplicated += 1
                    else:
                        rejected += 1
                        conflicted = True
                except UnsupportedSourceError:
                    raise
                except (TypeError, ValueError):
                    rejected += 1

            if run_provenance is None:
                raise UnsupportedSourceError("source and dataset provenance is required")
            completed_at = datetime.now(timezone.utc)
            run = IngestionRun(
                source=run_provenance[0],
                source_dataset=run_provenance[1],
                started_at=started_at,
                completed_at=completed_at,
                status="COMPLETED",
                source_sha256=source_sha256,
                accepted_count=accepted,
                rejected_count=rejected,
                deduplicated_count=deduplicated,
                safe_reason=(
                    SAFE_CONFLICT_REASON
                    if conflicted
                    else SAFE_VALIDATION_REASON if rejected else None
                ),
            )
            session.add(run)
            session.flush()
            run_id = run.id
            if transaction is not None:
                transaction.rollback()
                run_id = 0
            else:
                session.commit()
        except UnsupportedSourceError:
            session.rollback()
            failed_provenance = explicit_provenance or run_provenance
            if not dry_run and failed_provenance is not None:
                try:
                    _persist_failed_provenance_run(
                        session,
                        failed_provenance,
                        started_at,
                        source_sha256,
                    )
                except Exception:
                    session.rollback()
            raise
        except Exception:
            session.rollback()
            raise

    return ImportResult(
        run_id=run_id,
        accepted=accepted,
        rejected=rejected,
        deduplicated=deduplicated,
        source_sha256=source_sha256,
    )
