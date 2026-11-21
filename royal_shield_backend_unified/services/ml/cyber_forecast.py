"""Leakage-safe global cyber threat-volume forecasting."""
import logging
import pickle
from pathlib import Path
from typing import Any, Dict, List

import pandas as pd
import xgboost as xgb
from sklearn.metrics import mean_absolute_error, mean_squared_error

logger = logging.getLogger(__name__)


class CyberForecastEngine:
    def __init__(self):
        self.model = None
        self.feature_names: List[str] = []

    @staticmethod
    def build_dataset(observations: List[Dict[str, Any]], horizon_days: int = 7) -> pd.DataFrame:
        if not observations:
            return pd.DataFrame()
        frame = pd.DataFrame(observations)
        frame["observed_at"] = pd.to_datetime(frame["observed_at"], utc=True, errors="coerce")
        frame = frame.dropna(subset=["observed_at"])
        if frame.empty:
            return pd.DataFrame()
        frame["date"] = frame["observed_at"].dt.floor("D")
        severity_weight = {"low": 1.0, "medium": 3.0, "high": 6.0, "critical": 10.0}
        frame["weighted_risk"] = frame["severity"].str.lower().map(severity_weight).fillna(1.0)
        frame["weighted_risk"] *= frame["confidence"].clip(0, 100).fillna(0) / 100.0

        daily = frame.groupby("date").agg(
            event_count=("date", "size"),
            weighted_risk=("weighted_risk", "sum"),
            avg_confidence=("confidence", "mean"),
        )
        index = pd.date_range(daily.index.min(), daily.index.max(), freq="D", tz="UTC")
        daily = daily.reindex(index, fill_value=0.0)
        for severity in ("critical", "high", "medium", "low"):
            series = frame[frame["severity"].str.lower() == severity].groupby("date").size()
            daily[f"severity_{severity}"] = series.reindex(index, fill_value=0)
        for lag in (1, 7, 14):
            daily[f"risk_lag_{lag}"] = daily["weighted_risk"].shift(lag)
            daily[f"count_lag_{lag}"] = daily["event_count"].shift(lag)
        daily["risk_rolling_7"] = daily["weighted_risk"].shift(1).rolling(7).mean()
        daily["risk_rolling_30"] = daily["weighted_risk"].shift(1).rolling(30).mean()
        daily["day_of_week"] = daily.index.dayofweek
        daily["target_next_7d"] = sum(
            daily["weighted_risk"].shift(-offset) for offset in range(1, horizon_days + 1)
        )
        return daily.dropna().reset_index(names="forecast_date")

    def train(self, observations: List[Dict[str, Any]], validation_split: float = 0.2) -> Dict[str, Any]:
        dataset = self.build_dataset(observations)
        if len(dataset) < 20:
            return {"status": "error", "message": "Insufficient cyber history", "samples": len(dataset)}
        excluded = {"forecast_date", "target_next_7d"}
        self.feature_names = [column for column in dataset.columns if column not in excluded]
        split = int(len(dataset) * (1 - validation_split))
        X_train = dataset[self.feature_names].iloc[:split]
        X_val = dataset[self.feature_names].iloc[split:]
        y_train = dataset["target_next_7d"].iloc[:split]
        y_val = dataset["target_next_7d"].iloc[split:]
        self.model = xgb.XGBRegressor(
            objective="reg:squarederror", n_estimators=150, max_depth=5,
            learning_rate=0.05, subsample=0.8, colsample_bytree=0.8, random_state=42,
        )
        self.model.fit(X_train, y_train, eval_set=[(X_val, y_val)], verbose=False)
        predictions = self.model.predict(X_val)
        return {
            "status": "success", "n_samples_train": len(X_train), "n_samples_val": len(X_val),
            "mae_val": float(mean_absolute_error(y_val, predictions)),
            "rmse_val": float(mean_squared_error(y_val, predictions) ** 0.5),
            "r2_val": float(self.model.score(X_val, y_val)),
        }

    def save_model(self, path: str) -> None:
        if self.model is None:
            raise RuntimeError("No cyber model has been trained")
        output = Path(path)
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("wb") as handle:
            pickle.dump({"model": self.model, "feature_names": self.feature_names}, handle)
