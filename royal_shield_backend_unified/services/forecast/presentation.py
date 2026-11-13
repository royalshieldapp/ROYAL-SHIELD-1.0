"""Verified map and hotspot projections over durable Forecast observations."""

from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import date

import h3
from sqlalchemy import select
from sqlalchemy.orm import Session

from services.forecast.artifact_store import LoadedArtifact, PostgresArtifactStore
from services.forecast.inference import (
    CellPrediction,
    ForecastInferenceService,
    InferenceUnavailableError,
)
from services.forecast.models import ForecastObservation


@dataclass(frozen=True)
class ForecastCellPresentation:
    prediction: CellPrediction
    center_lat: float
    center_lng: float
    boundary: tuple[tuple[float, float], ...]
    total_events: int
    recent_7d: int
    recent_30d: int
    previous_7d: int
    previous_30d: int
    window_events: int
    event_types: dict[str, int]

    @property
    def risk_level(self) -> str:
        score = self.prediction.risk_score
        if score >= 75:
            return "CRITICAL"
        if score >= 50:
            return "HIGH"
        if score >= 25:
            return "MEDIUM"
        return "LOW"


@dataclass(frozen=True)
class ForecastPresentation:
    artifact: LoadedArtifact
    cells: tuple[ForecastCellPresentation, ...]

    @property
    def status(self) -> str:
        return "DEGRADED" if any(cell.prediction.status == "DEGRADED" for cell in self.cells) else "READY"

    @property
    def sources(self) -> tuple[str, ...]:
        return tuple(
            sorted(
                {
                    source
                    for cell in self.cells
                    for source in cell.prediction.sources
                }
            )
        )

    @property
    def data_freshness(self) -> date:
        return min(cell.prediction.data_freshness for cell in self.cells)


def load_forecast_presentation(
    session: Session,
    *,
    bbox_min_lat: float,
    bbox_min_lng: float,
    bbox_max_lat: float,
    bbox_max_lng: float,
    resolution: int,
    horizon_days: int,
    as_of: date,
    time_window_days: int = 30,
) -> ForecastPresentation:
    """Load only cells backed by durable observations and a verified model artifact."""

    if bbox_min_lat >= bbox_max_lat or bbox_min_lng >= bbox_max_lng:
        raise ValueError("bounding box minimums must be below maximums")

    try:
        artifact = PostgresArtifactStore(session).load_active()
    except Exception as error:
        raise InferenceUnavailableError("active forecast artifact is unavailable") from error
    artifact_resolution = artifact.metrics.get("h3_resolution")
    if artifact_resolution != resolution:
        raise InferenceUnavailableError("artifact H3 resolution does not match the request")

    try:
        rows = tuple(
            session.scalars(
                select(ForecastObservation)
                .where(
                    ForecastObservation.observed_date <= as_of,
                    ForecastObservation.h3_cell.not_in(("UNKNOWN", "")),
                )
                .order_by(
                    ForecastObservation.h3_cell,
                    ForecastObservation.observed_date,
                    ForecastObservation.id,
                )
            )
        )
    except Exception as error:
        raise InferenceUnavailableError("durable forecast observations are unavailable") from error

    rows_by_cell: dict[str, list[ForecastObservation]] = defaultdict(list)
    centers: dict[str, tuple[float, float]] = {}
    for row in rows:
        cell = row.h3_cell
        try:
            if not h3.h3_is_valid(cell) or h3.h3_get_resolution(cell) != resolution:
                continue
            lat, lng = h3.h3_to_geo(cell)
        except (TypeError, ValueError):
            continue
        if bbox_min_lat <= lat <= bbox_max_lat and bbox_min_lng <= lng <= bbox_max_lng:
            rows_by_cell[cell].append(row)
            centers[cell] = (float(lat), float(lng))

    valid_rows_by_cell = {
        cell: cell_rows
        for cell, cell_rows in rows_by_cell.items()
        if 0 <= (as_of - max(r.observed_date for r in cell_rows)).days <= 365
    }
    cells = tuple(sorted(valid_rows_by_cell))
    if not cells:
        raise InferenceUnavailableError("no durable forecast observations cover the requested area")

    selected_rows = tuple(row for cell in cells for row in valid_rows_by_cell[cell])
    predictions = ForecastInferenceService(session).predict_cells(
        cells,
        horizon_days=horizon_days,
        as_of=as_of,
        artifact=artifact,
        observations=selected_rows,
    )

    presentations: list[ForecastCellPresentation] = []
    for prediction in predictions:
        cell_rows = valid_rows_by_cell[prediction.h3_cell]
        event_types: Counter[str] = Counter()
        total_events = recent_7d = recent_30d = 0
        previous_7d = previous_30d = window_events = 0
        for row in cell_rows:
            age_days = (as_of - row.observed_date).days
            total_events += row.event_count
            event_types[row.event_type] += row.event_count
            if 0 <= age_days < 7:
                recent_7d += row.event_count
            if 0 <= age_days < 30:
                recent_30d += row.event_count
            if 7 <= age_days < 14:
                previous_7d += row.event_count
            if 30 <= age_days < 60:
                previous_30d += row.event_count
            if 0 <= age_days < time_window_days:
                window_events += row.event_count

        boundary = tuple((float(lat), float(lng)) for lat, lng in h3.h3_to_geo_boundary(prediction.h3_cell))
        presentations.append(
            ForecastCellPresentation(
                prediction=prediction,
                center_lat=centers[prediction.h3_cell][0],
                center_lng=centers[prediction.h3_cell][1],
                boundary=boundary,
                total_events=total_events,
                recent_7d=recent_7d,
                recent_30d=recent_30d,
                previous_7d=previous_7d,
                previous_30d=previous_30d,
                window_events=window_events,
                event_types=dict(sorted(event_types.items())),
            )
        )

    return ForecastPresentation(artifact=artifact, cells=tuple(presentations))

