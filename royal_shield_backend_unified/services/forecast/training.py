"""Leakage-safe chronological training for durable geospatial observations."""

from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import date, datetime, timezone
import hashlib
import math
from typing import Iterable

import numpy as np
import xgboost as xgb
import h3
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sqlalchemy import select
from sqlalchemy.orm import Session

from services.forecast.artifact_store import PostgresArtifactStore
from services.forecast.models import ForecastObservation, ForecastTrainingRun
from services.forecast.quality import DataQualityReport, audit_observations


FEATURE_SCHEMA = (
    "events_total", "events_7d", "events_30d", "frequency", "recency", "horizon_days"
)


@dataclass(frozen=True)
class TemporalSample:
    h3_cell: str
    cutoff: date
    horizon_days: int
    feature_end: date
    target_start: date
    target_end: date
    features: dict[str, float]
    target: float


@dataclass(frozen=True)
class TemporalDataset:
    samples: tuple[TemporalSample, ...]
    samples_by_cutoff: dict[date, tuple[TemporalSample, ...]]
    source_start: date | None
    source_end: date | None


@dataclass(frozen=True)
class ChronologicalSplit:
    train: tuple[TemporalSample, ...]
    validation: tuple[TemporalSample, ...]
    test: tuple[TemporalSample, ...]


@dataclass(frozen=True)
class MetricSet:
    mae: float
    rmse: float
    r2: float
    sample_count: int

    def as_dict(self) -> dict[str, float | int]:
        return {"mae": self.mae, "rmse": self.rmse, "r2": self.r2, "sample_count": self.sample_count}


@dataclass(frozen=True)
class CandidateFit:
    predictions: tuple[float, ...]
    model_bytes: bytes


@dataclass(frozen=True)
class TrainingResult:
    version: str
    source_start: date | None
    source_end: date | None
    sample_count: int
    train_samples: int
    validation_samples: int
    test_samples: int
    baseline_metrics: MetricSet | None
    candidate_metrics: MetricSet | None
    promoted: bool
    safe_reason: str | None
    model_bytes: bytes | None
    feature_schema: tuple[str, ...]


def build_temporal_samples(
    observations: Iterable[ForecastObservation], horizons: tuple[int, ...] = (7, 30)
) -> TemporalDataset:
    rows = sorted(observations, key=lambda row: (row.observed_date, row.h3_cell))
    if not rows:
        return TemporalDataset((), {}, None, None)
    if not horizons or any(horizon < 1 for horizon in horizons):
        raise ValueError("horizons must contain positive day counts")

    by_cell: dict[str, list[ForecastObservation]] = {}
    for row in rows:
        by_cell.setdefault(row.h3_cell, []).append(row)
    samples: list[TemporalSample] = []
    for h3_cell, cell_rows in sorted(by_cell.items()):
        for cutoff_row in cell_rows[:-1]:
            cutoff = cutoff_row.observed_date
            history = [row for row in cell_rows if row.observed_date <= cutoff]
            elapsed = max(1, (cutoff - history[0].observed_date).days + 1)
            total = float(sum(row.event_count for row in history))
            last_event = max((row.observed_date for row in history if row.event_count > 0), default=cutoff)
            for horizon in sorted(set(horizons)):
                target_end = cutoff.fromordinal(cutoff.toordinal() + horizon)
                future = [
                    row for row in cell_rows
                    if cutoff < row.observed_date <= target_end
                ]
                if not future:
                    continue
                features = {
                    "events_total": total,
                    "events_7d": float(sum(row.event_count for row in history if (cutoff-row.observed_date).days < 7)),
                    "events_30d": float(sum(row.event_count for row in history if (cutoff-row.observed_date).days < 30)),
                    "frequency": total / elapsed,
                    "recency": float((cutoff - last_event).days),
                    "horizon_days": float(horizon),
                }
                samples.append(TemporalSample(
                    h3_cell=h3_cell, cutoff=cutoff, horizon_days=horizon,
                    feature_end=cutoff, target_start=cutoff.fromordinal(cutoff.toordinal()+1),
                    target_end=target_end, features=features,
                    target=float(sum(row.event_count for row in future)),
                ))
    samples.sort(key=lambda sample: (sample.cutoff, sample.h3_cell, sample.horizon_days))
    grouped: dict[date, list[TemporalSample]] = {}
    for sample in samples:
        grouped.setdefault(sample.cutoff, []).append(sample)
    return TemporalDataset(
        tuple(samples), {key: tuple(value) for key, value in grouped.items()},
        rows[0].observed_date, rows[-1].observed_date,
    )


def chronological_split(dataset: TemporalDataset) -> ChronologicalSplit:
    cutoffs = sorted(dataset.samples_by_cutoff)
    if len(cutoffs) < 3:
        return ChronologicalSplit(tuple(dataset.samples), (), ())
    train_end = max(1, int(len(cutoffs) * 0.6))
    validation_end = max(train_end + 1, int(len(cutoffs) * 0.8))
    validation_end = min(validation_end, len(cutoffs) - 1)
    train_dates = set(cutoffs[:train_end])
    validation_dates = set(cutoffs[train_end:validation_end])
    test_dates = set(cutoffs[validation_end:])
    return ChronologicalSplit(
        tuple(sample for sample in dataset.samples if sample.cutoff in train_dates),
        tuple(sample for sample in dataset.samples if sample.cutoff in validation_dates),
        tuple(sample for sample in dataset.samples if sample.cutoff in test_dates),
    )


def _matrix(samples: tuple[TemporalSample, ...], schema: tuple[str, ...]) -> np.ndarray:
    return np.asarray([[sample.features[name] for name in schema] for sample in samples], dtype=float)


def fit_xgboost_candidate(train, validation, test, feature_schema) -> CandidateFit:
    del validation
    model = xgb.XGBRegressor(
        objective="reg:squarederror", n_estimators=32, max_depth=3,
        learning_rate=0.05, subsample=1.0, colsample_bytree=1.0,
        random_state=42, n_jobs=1,
    )
    model.fit(_matrix(tuple(train), tuple(feature_schema)), np.asarray([s.target for s in train]))
    predictions = tuple(float(value) for value in model.predict(_matrix(tuple(test), tuple(feature_schema))))
    raw = model.get_booster().save_raw(raw_format="json")
    return CandidateFit(predictions, bytes(raw))


def _metrics(actual: tuple[float, ...], predicted: tuple[float, ...]) -> MetricSet:
    r2 = float(r2_score(actual, predicted)) if len(actual) >= 2 else 0.0
    return MetricSet(
        mae=float(mean_absolute_error(actual, predicted)),
        rmse=float(mean_squared_error(actual, predicted) ** 0.5),
        r2=r2,
        sample_count=len(actual),
    )


def decide_promotion(quality: DataQualityReport, baseline: MetricSet, candidate: MetricSet, model_bytes: bytes):
    if quality.valid_rows < 1 or not quality.sources:
        return False, "Durable source observations are unavailable."
    if quality.h3_cells < 1:
        return False, "Geographic coverage is unavailable."
    if quality.latest_observation is None or quality.freshness is None:
        return False, "Forecast observation freshness is unavailable."
    if quality.freshness < 0:
        return False, "Forecast observation freshness is invalid."
    if quality.freshness > 365:
        return False, "Forecast observations are stale."
    if not all(math.isfinite(value) for metric in (baseline, candidate) for value in (metric.mae, metric.rmse, metric.r2)):
        return False, "Candidate metrics are not finite."
    if not model_bytes:
        return False, "Model serialization failed."
    if candidate.mae >= baseline.mae:
        return False, "Candidate did not outperform the baseline."
    return True, None


def _persist_run(session: Session, result: TrainingResult, horizons: tuple[int, ...]) -> None:
    now = datetime.now(timezone.utc)
    candidate = result.candidate_metrics.as_dict() if result.candidate_metrics else {}
    baseline = result.baseline_metrics.as_dict() if result.baseline_metrics else {}
    session.add(ForecastTrainingRun(
        version=result.version, started_at=now, completed_at=now,
        status="COMPLETED" if result.promoted else "UNAVAILABLE",
        sample_count=result.sample_count, source_start=result.source_start,
        source_end=result.source_end, horizons=list(horizons), metrics=candidate,
        baseline_metrics=baseline, promoted=result.promoted,
        artifact_sha256=hashlib.sha256(result.model_bytes).hexdigest() if result.model_bytes else None,
        safe_reason=result.safe_reason,
    ))
    session.flush()


def train_forecast(session: Session, min_samples: int, horizons=(7, 30), dry_run: bool = False) -> TrainingResult:
    del dry_run
    rows = tuple(session.scalars(select(ForecastObservation).order_by(ForecastObservation.observed_date, ForecastObservation.id)))
    quality = audit_observations(session)
    dataset = build_temporal_samples(rows, tuple(horizons))
    version = f"forecast-{datetime.now(timezone.utc):%Y%m%dT%H%M%SZ}"
    valid_h3_cells = tuple(
        row.h3_cell
        for row in rows
        if isinstance(row.h3_cell, str) and h3.h3_is_valid(row.h3_cell)
    )
    h3_resolutions = {
        h3.h3_get_resolution(h3_cell) for h3_cell in valid_h3_cells
    }
    reason = None
    if not rows:
        reason = "Insufficient durable observations."
    elif quality.h3_cells < 1:
        reason = "Geographic coverage is unavailable."
    elif not valid_h3_cells:
        reason = "Geographic coverage contains no valid H3 cells."
    elif len(h3_resolutions) != 1:
        reason = "Forecast observations use multiple H3 resolutions."
    elif len(dataset.samples) < min_samples:
        reason = "Insufficient temporal samples."
    if reason:
        result = TrainingResult(version, dataset.source_start, dataset.source_end, len(dataset.samples), 0, 0, 0, None, None, False, reason, None, FEATURE_SCHEMA)
        _persist_run(session, result, tuple(horizons))
        session.commit()
        return result

    split = chronological_split(dataset)
    if not split.validation or not split.test:
        result = TrainingResult(version, dataset.source_start, dataset.source_end, len(dataset.samples), len(split.train), len(split.validation), len(split.test), None, None, False, "Insufficient chronological partitions.", None, FEATURE_SCHEMA)
        _persist_run(session, result, tuple(horizons)); session.commit(); return result
    actual = tuple(sample.target for sample in split.test)
    baseline_predictions = tuple(max(0.0, sample.features["frequency"] * sample.horizon_days) for sample in split.test)
    baseline = _metrics(actual, baseline_predictions)
    try:
        fit = fit_xgboost_candidate(split.train, split.validation, split.test, FEATURE_SCHEMA)
        candidate = _metrics(actual, fit.predictions)
        promoted, reason = decide_promotion(quality, baseline, candidate, fit.model_bytes)
        model_bytes = fit.model_bytes if promoted else None
    except Exception:
        candidate, promoted, reason, model_bytes = None, False, "Forecast training failed.", None
    result = TrainingResult(version, dataset.source_start, dataset.source_end, len(dataset.samples), len(split.train), len(split.validation), len(split.test), baseline, candidate, promoted, reason, model_bytes, FEATURE_SCHEMA)
    _persist_run(session, result, tuple(horizons))
    if result.promoted and result.model_bytes is not None:
        target_p95 = float(np.percentile([sample.target for sample in split.train], 95))
        artifact_metrics = {
            **result.candidate_metrics.as_dict(),
            "feature_schema": list(result.feature_schema),
            "h3_resolution": h3_resolutions.pop(),
            "data_quality": {
                "valid_rows": quality.valid_rows,
                "rejected_rows": quality.rejected_rows,
                "deduplicated_rows": quality.deduplicated_rows,
                "h3_cells": quality.h3_cells,
                "freshness_days": quality.freshness,
                "sources": list(quality.sources),
            },
            "calibration": {
                "target_p95": target_p95,
                "source_partition": "train",
            },
        }
        try:
            PostgresArtifactStore(session).publish(
                version=result.version,
                artifact_bytes=result.model_bytes,
                model_format="xgboost-json",
                metrics=artifact_metrics,
                source_start=result.source_start,
                source_end=result.source_end,
                activate=True,
                commit=False,
            )
        except Exception:
            session.rollback()
            result = replace(
                result,
                promoted=False,
                safe_reason="Forecast artifact publication failed.",
                model_bytes=None,
            )
            _persist_run(session, result, tuple(horizons))
            session.commit()
            return result
    session.commit()
    return result
