"""Durable storage and integrity checks for trained forecast models."""

from dataclasses import dataclass
from datetime import date
from hashlib import sha256

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from services.forecast.models import ForecastModelArtifact


MAX_ARTIFACT_BYTES = 10 * 1024 * 1024


class ArtifactStoreError(RuntimeError):
    """Base error that is safe to convert to an unavailable forecast state."""


class ArtifactNotFoundError(ArtifactStoreError):
    pass


class ArtifactChecksumError(ArtifactStoreError):
    pass


@dataclass(frozen=True)
class LoadedArtifact:
    id: int
    version: str
    model_format: str
    artifact_bytes: bytes
    sha256: str
    metrics: dict
    source_start: date
    source_end: date


class PostgresArtifactStore:
    """Store small XGBoost JSON artifacts in the application's database."""

    def __init__(self, session: Session):
        self.session = session

    def publish(
        self,
        *,
        version: str,
        artifact_bytes: bytes,
        model_format: str,
        metrics: dict,
        source_start: date,
        source_end: date,
        activate: bool,
        commit: bool = True,
    ) -> ForecastModelArtifact:
        if not artifact_bytes:
            raise ValueError("artifact_bytes must not be empty")
        if len(artifact_bytes) > MAX_ARTIFACT_BYTES:
            raise ValueError("artifact exceeds the 10 MiB database safety limit")
        if source_end < source_start:
            raise ValueError("source_end must not precede source_start")

        if activate:
            self.session.execute(
                update(ForecastModelArtifact)
                .where(ForecastModelArtifact.active.is_(True))
                .values(active=False)
            )

        record = ForecastModelArtifact(
            version=version,
            model_format=model_format,
            artifact_blob=artifact_bytes,
            sha256=sha256(artifact_bytes).hexdigest(),
            metrics=metrics,
            source_start=source_start,
            source_end=source_end,
            active=activate,
        )
        self.session.add(record)
        if commit:
            self.session.commit()
            self.session.refresh(record)
        else:
            self.session.flush()
        return record

    def load_active(self) -> LoadedArtifact:
        records = list(
            self.session.scalars(
                select(ForecastModelArtifact)
                .where(ForecastModelArtifact.active.is_(True))
                .order_by(ForecastModelArtifact.created_at.desc())
            )
        )
        if not records:
            raise ArtifactNotFoundError("no active forecast model artifact")
        if len(records) != 1:
            raise ArtifactStoreError("multiple active forecast model artifacts")

        record = records[0]
        calculated = sha256(record.artifact_blob).hexdigest()
        if calculated != record.sha256:
            raise ArtifactChecksumError("active forecast artifact checksum mismatch")

        return LoadedArtifact(
            id=record.id,
            version=record.version,
            model_format=record.model_format,
            artifact_bytes=record.artifact_blob,
            sha256=record.sha256,
            metrics=dict(record.metrics),
            source_start=record.source_start,
            source_end=record.source_end,
        )
