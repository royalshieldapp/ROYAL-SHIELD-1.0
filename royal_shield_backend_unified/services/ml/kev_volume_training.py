"""Reproducible, leakage-safe forecasting of CISA KEV catalog additions.

This module forecasts catalog additions, not exploitation likelihood for a CVE.
Only the CISA catalog-addition timestamp is used as an input signal.
"""
from __future__ import annotations

from datetime import date, datetime, timezone
import hashlib
import json
from pathlib import Path
from typing import Any, Iterable

import joblib
import pandas as pd
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error


KEV_VOLUME_MODEL_VERSION = "kev-volume-forecast-2026.08.09.1"


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def build_daily_supervised_frame(
    rows: Iterable[dict[str, Any]], horizon_days: int = 7
) -> pd.DataFrame:
    """Build a daily prospective target using only the catalog-addition time."""
    if horizon_days < 1:
        raise ValueError("horizon_days must be at least one")
    frame = pd.DataFrame(rows)
    if frame.empty or "event_time" not in frame:
        return pd.DataFrame(columns=["forecast_date", "count_lag_1", "count_rolling_7", "day_of_week", "target_next_horizon"])
    event_time = pd.to_datetime(frame["event_time"], utc=True, errors="coerce").dropna()
    if event_time.empty:
        return pd.DataFrame(columns=["forecast_date", "count_lag_1", "count_rolling_7", "day_of_week", "target_next_horizon"])
    daily = event_time.dt.floor("D").value_counts().sort_index().rename("catalog_addition_count")
    index = pd.date_range(daily.index.min(), daily.index.max(), freq="D", tz="UTC")
    daily = daily.reindex(index, fill_value=0).astype(float)
    dataset = pd.DataFrame(index=index)
    dataset["count_lag_1"] = daily.shift(1)
    dataset["count_rolling_7"] = daily.shift(1).rolling(7, min_periods=1).mean()
    dataset["day_of_week"] = dataset.index.dayofweek
    dataset["target_next_horizon"] = sum(daily.shift(-offset) for offset in range(1, horizon_days + 1))
    dataset = dataset.dropna().reset_index(names="forecast_date")
    dataset["forecast_date"] = dataset["forecast_date"].dt.date
    return dataset


def temporal_train_validation_split(
    dataset: pd.DataFrame, validation_days: int
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Split chronologically; no row in validation may precede training."""
    if validation_days < 1:
        raise ValueError("validation_days must be at least one")
    if len(dataset) <= validation_days:
        return dataset.iloc[0:0].copy(), dataset.copy()
    return dataset.iloc[:-validation_days].copy(), dataset.iloc[-validation_days:].copy()


def run_training(
    rows: Iterable[dict[str, Any]],
    output_directory: Path,
    min_history_days: int = 60,
    validation_days: int = 14,
    horizon_days: int = 7,
) -> dict[str, Any]:
    """Train only after a chronological holdout can be formed."""
    source_rows = list(rows)
    source_times = pd.to_datetime(
        [row.get("event_time") for row in source_rows], utc=True, errors="coerce"
    )
    valid_source_times = source_times.dropna()
    dataset = build_daily_supervised_frame(source_rows, horizon_days=horizon_days)
    history_days = len(dataset) + horizon_days + 1 if not dataset.empty else 0
    result = {
        "status": "UNAVAILABLE",
        "reason": "INSUFFICIENT_TEMPORAL_HISTORY",
        "modelVersion": KEV_VOLUME_MODEL_VERSION,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "dataFreshness": None if valid_source_times.empty else valid_source_times.max().date().isoformat(),
        "sources": ["CISA Known Exploited Vulnerabilities Catalog"],
        "objective": "Forecast the next seven days of CISA KEV catalog additions; not CVE exploit likelihood.",
        "historyDays": history_days,
        "minimumHistoryDays": min_history_days,
        "validationDays": validation_days,
        "horizonDays": horizon_days,
    }
    if history_days < min_history_days:
        return result
    train, validation = temporal_train_validation_split(dataset, validation_days)
    if train.empty or validation.empty:
        result["reason"] = "INSUFFICIENT_TEMPORAL_VALIDATION"
        return result

    feature_names = ["count_lag_1", "count_rolling_7", "day_of_week"]
    model = HistGradientBoostingRegressor(max_iter=100, learning_rate=0.05, random_state=42)
    model.fit(train[feature_names], train["target_next_horizon"])
    predictions = model.predict(validation[feature_names])
    validation_mae = float(mean_absolute_error(validation["target_next_horizon"], predictions))
    validation_rmse = float(mean_squared_error(validation["target_next_horizon"], predictions) ** 0.5)
    naive_mae = float(mean_absolute_error(validation["target_next_horizon"], validation["count_lag_1"]))
    status = "READY" if validation_mae <= naive_mae else "DEGRADED"

    output_directory.mkdir(parents=True, exist_ok=True)
    artifact_path = output_directory / "kev_volume_forecaster.joblib"
    joblib.dump({"model": model, "featureNames": feature_names, "modelVersion": KEV_VOLUME_MODEL_VERSION}, artifact_path)
    artifact_sha256 = _sha256_file(artifact_path)
    result.update({
        "status": status,
        "reason": None if status == "READY" else "MODEL_DID_NOT_BEAT_NAIVE_BASELINE",
        "metrics": {
            "validation_mae": validation_mae,
            "validation_rmse": validation_rmse,
            "naive_validation_mae": naive_mae,
        },
        "artifact": artifact_path.name,
        "artifactSha256": artifact_sha256,
        "trainSamples": len(train),
        "validationSamples": len(validation),
    })
    manifest_path = output_directory / "kev_volume_forecaster.manifest.json"
    manifest_path.write_text(json.dumps(result, indent=2, sort_keys=True), encoding="utf-8")
    return result
