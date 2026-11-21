"""Quality audit derived only from persisted forecast observations and runs."""

from dataclasses import dataclass
from datetime import date, datetime, timezone

from sqlalchemy import distinct, func, select
from sqlalchemy.orm import Session

from services.forecast.models import ForecastObservation, IngestionRun


@dataclass(frozen=True)
class DataQualityReport:
    sources: tuple[str, ...]
    source_start: date | None
    source_end: date | None
    valid_rows: int
    rejected_rows: int
    deduplicated_rows: int
    h3_cells: int
    latest_observation: date | None
    freshness: int | None


def audit_observations(session: Session) -> DataQualityReport:
    """Summarize durable source coverage without guessing absent values."""

    observation_sources = set(
        session.scalars(
            select(distinct(ForecastObservation.source)).order_by(ForecastObservation.source)
        ).all()
    )
    completed_run_filter = (
        (IngestionRun.status == "COMPLETED")
        & IngestionRun.completed_at.is_not(None)
    )
    run_sources = set(
        session.scalars(
            select(distinct(IngestionRun.source)).where(completed_run_filter)
        ).all()
    )
    sources = tuple(sorted(observation_sources | run_sources))
    source_start, source_end = session.execute(
        select(
            func.min(ForecastObservation.observed_date),
            func.max(ForecastObservation.observed_date),
        )
    ).one()
    valid_rows = int(
        session.scalar(select(func.count(ForecastObservation.id))) or 0
    )
    rejected_rows, deduplicated_rows = session.execute(
        select(
            func.coalesce(func.sum(IngestionRun.rejected_count), 0),
            func.coalesce(func.sum(IngestionRun.deduplicated_count), 0),
        ).where(completed_run_filter)
    ).one()
    h3_cells = int(
        session.scalar(
            select(func.count(distinct(ForecastObservation.h3_cell))).where(
                ForecastObservation.h3_cell.not_in(("UNKNOWN", ""))
            )
        )
        or 0
    )
    latest_observation = source_end
    freshness = (
        (datetime.now(timezone.utc).date() - latest_observation).days
        if latest_observation is not None
        else None
    )
    return DataQualityReport(
        sources=sources,
        source_start=source_start,
        source_end=source_end,
        valid_rows=valid_rows,
        rejected_rows=int(rejected_rows),
        deduplicated_rows=int(deduplicated_rows),
        h3_cells=h3_cells,
        latest_observation=latest_observation,
        freshness=freshness,
    )
