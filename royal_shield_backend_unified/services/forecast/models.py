"""Database records for forecast ingestion and training audit trails."""

from datetime import date, datetime, timezone

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    Integer,
    JSON,
    LargeBinary,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, reconstructor, validates

from services.geospatial.database.connection import Base


ForecastBase = Base


def _normalize_audit_datetime(field: str, value: datetime | None) -> datetime | None:
    if value is None:
        return None
    if value.tzinfo is None or value.utcoffset() is None:
        raise ValueError(f"{field} must be timezone-aware")
    return value.astimezone(timezone.utc)


class ForecastObservation(Base):
    """An approved-source aggregate; it intentionally contains no person-level data."""

    __tablename__ = "forecast_observations"
    __table_args__ = (
        UniqueConstraint(
            "source",
            "source_dataset",
            "observed_date",
            "district",
            "police_grid",
            "h3_cell",
            "event_type",
            name="uq_forecast_observation_source_key",
        ),
        CheckConstraint("event_count >= 0", name="ck_forecast_observation_nonnegative_count"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    source: Mapped[str] = mapped_column(String(200), nullable=False)
    source_dataset: Mapped[str] = mapped_column(String(200), nullable=False)
    observed_date: Mapped[date] = mapped_column(Date, nullable=False)
    district: Mapped[str] = mapped_column(String(100), nullable=False, default="UNKNOWN")
    police_grid: Mapped[str] = mapped_column(String(32), nullable=False, default="UNKNOWN")
    h3_cell: Mapped[str] = mapped_column(String(32), nullable=False, default="UNKNOWN")
    event_type: Mapped[str] = mapped_column(String(64), nullable=False)
    event_count: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=lambda: datetime.now(timezone.utc),
    )


class ForecastModelArtifact(Base):
    """A durable, checksum-protected forecast model stored in PostgreSQL."""

    __tablename__ = "forecast_model_artifacts"

    id: Mapped[int] = mapped_column(primary_key=True)
    version: Mapped[str] = mapped_column(String(100), nullable=False, unique=True)
    model_format: Mapped[str] = mapped_column(String(50), nullable=False)
    artifact_blob: Mapped[bytes] = mapped_column(LargeBinary, nullable=False)
    sha256: Mapped[str] = mapped_column(String(64), nullable=False)
    metrics: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    source_start: Mapped[date | None] = mapped_column(Date)
    source_end: Mapped[date | None] = mapped_column(Date)
    trained_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=lambda: datetime.now(timezone.utc),
    )
    active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False, index=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=lambda: datetime.now(timezone.utc),
    )


class IngestionRun(Base):
    """Privacy-minimised audit record for one durable source ingestion."""

    __tablename__ = "forecast_ingestion_runs"
    __table_args__ = (
        CheckConstraint(
            "accepted_count >= 0",
            name="ck_forecast_ingestion_run_nonnegative_accepted_count",
        ),
        CheckConstraint(
            "rejected_count >= 0",
            name="ck_forecast_ingestion_run_nonnegative_rejected_count",
        ),
        CheckConstraint(
            "deduplicated_count >= 0",
            name="ck_forecast_ingestion_run_nonnegative_deduplicated_count",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    source: Mapped[str] = mapped_column(String(200), nullable=False)
    source_dataset: Mapped[str] = mapped_column(String(200), nullable=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    source_sha256: Mapped[str] = mapped_column(String(64), nullable=False)
    accepted_count: Mapped[int] = mapped_column(Integer, nullable=False)
    rejected_count: Mapped[int] = mapped_column(Integer, nullable=False)
    deduplicated_count: Mapped[int] = mapped_column(Integer, nullable=False)
    safe_reason: Mapped[str | None] = mapped_column(String(1000))

    @validates("started_at", "completed_at")
    def validate_audit_datetime(
        self,
        field: str,
        value: datetime | None,
    ) -> datetime | None:
        return _normalize_audit_datetime(field, value)


class ForecastTrainingRun(Base):
    """Privacy-minimised audit record for one forecast training attempt."""

    __tablename__ = "forecast_training_runs"
    __table_args__ = (
        CheckConstraint(
            "sample_count >= 0",
            name="ck_forecast_training_run_nonnegative_sample_count",
        ),
        CheckConstraint(
            "source_start <= source_end",
            name="ck_forecast_training_run_source_date_order",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    version: Mapped[str] = mapped_column(String(100), nullable=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(32), nullable=False)
    sample_count: Mapped[int] = mapped_column(Integer, nullable=False)
    source_start: Mapped[date | None] = mapped_column(Date)
    source_end: Mapped[date | None] = mapped_column(Date)
    horizons: Mapped[list] = mapped_column(JSON, nullable=False)
    metrics: Mapped[dict] = mapped_column(JSON, nullable=False)
    baseline_metrics: Mapped[dict] = mapped_column(JSON, nullable=False)
    promoted: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    artifact_sha256: Mapped[str | None] = mapped_column(String(64))
    safe_reason: Mapped[str | None] = mapped_column(String(1000))

    @validates("started_at", "completed_at")
    def validate_audit_datetime(
        self,
        field: str,
        value: datetime | None,
    ) -> datetime | None:
        return _normalize_audit_datetime(field, value)

    @reconstructor
    def normalize_loaded_datetimes(self) -> None:
        """Restore explicit UTC when SQLite returns naive audit timestamps."""
        if self.started_at.tzinfo is None:
            self.started_at = self.started_at.replace(tzinfo=timezone.utc)
        if self.completed_at is not None and self.completed_at.tzinfo is None:
            self.completed_at = self.completed_at.replace(tzinfo=timezone.utc)
