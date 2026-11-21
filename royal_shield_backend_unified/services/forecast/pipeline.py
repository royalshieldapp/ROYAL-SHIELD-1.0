"""Explicit ingestion and training pipeline for approved Miami-Dade data."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Iterable, Protocol

from sqlalchemy.orm import Session

from services.forecast.importers.miami_dade_h3 import (
    AggregationMetrics,
    ArcGisPcbClient,
    H3DailyAggregate,
    aggregate_arcgis_features,
    persist_h3_aggregates,
)
from services.forecast.training import TrainingResult, train_forecast


class MiamiDadeSourceClient(Protocol):
    """The limited source contract needed by the training pipeline."""

    def fetch_layer(self, layer_id: int) -> Iterable[dict]: ...

    def close(self) -> None: ...


@dataclass(frozen=True)
class MiamiDadeTrainingResult:
    """Auditable result derived from ingestion and durable training only."""

    layer_id: int
    event_type: str
    resolution: int
    aggregation: AggregationMetrics
    inserted_observations: int
    training: TrainingResult


def run_miami_dade_training(
    *,
    session: Session,
    layer_id: int,
    event_type: str,
    resolution: int,
    min_samples: int,
    horizons: tuple[int, ...],
    source_dataset: str = "PCB_Report",
    client: MiamiDadeSourceClient | None = None,
    persist: Callable[..., int] = persist_h3_aggregates,
    train: Callable[..., TrainingResult] = train_forecast,
) -> MiamiDadeTrainingResult:
    """Fetch one approved layer, persist H3 aggregates, then train V2.

    Callers must explicitly supply the approved ArcGIS layer and event type.  This
    function neither supplies a default layer nor reports a model as available
    unless ``train_forecast`` promotes a durable artifact.
    """

    if isinstance(layer_id, bool) or not isinstance(layer_id, int) or layer_id < 0:
        raise ValueError("layer_id must be a non-negative integer")
    if not isinstance(event_type, str) or not event_type.strip():
        raise ValueError("event_type is required")
    if not 0 <= resolution <= 15:
        raise ValueError("resolution must be between 0 and 15")
    if min_samples < 1:
        raise ValueError("min_samples must be positive")
    if not horizons or any(not isinstance(horizon, int) or horizon < 1 for horizon in horizons):
        raise ValueError("horizons must contain positive day counts")

    source_client = client or ArcGisPcbClient()
    metrics = AggregationMetrics()
    try:
        aggregates = aggregate_arcgis_features(
            source_client.fetch_layer(layer_id),
            event_type=event_type.strip(),
            resolution=resolution,
            metrics=metrics,
        )
        inserted = persist(
            session,
            aggregates,
            source_dataset=source_dataset,
        )
        training = train(
            session,
            min_samples=min_samples,
            horizons=tuple(horizons),
        )
        return MiamiDadeTrainingResult(
            layer_id=layer_id,
            event_type=event_type.strip(),
            resolution=resolution,
            aggregation=metrics,
            inserted_observations=inserted,
            training=training,
        )
    finally:
        source_client.close()
