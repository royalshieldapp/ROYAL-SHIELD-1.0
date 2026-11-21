"""Train Forecast Engine 2 from durable, normalized Royal Shield events."""
import argparse
import json
import logging
import sys
from datetime import datetime, timezone
from pathlib import Path

BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from api.routes import threat_map as _threat_map_models  # Registers ORM tables.
from services.geospatial.database.connection import SessionLocal, init_db
from services.ml.risk_predictor import RiskPredictor
from services.ml.cyber_forecast import CyberForecastEngine
from services.ml.training_data import load_cyber_training_data, load_training_data


def main() -> int:
    parser = argparse.ArgumentParser(description="Train Royal Shield Forecast Engine 2")
    parser.add_argument("--output", default="models/risk_predictor_latest.pkl")
    parser.add_argument("--lookback-days", type=int, default=90)
    parser.add_argument("--min-events", type=int, default=100)
    args = parser.parse_args()

    if args.lookback_days < 14 or args.min_events < 10:
        parser.error("lookback-days must be >= 14 and min-events must be >= 10")

    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    init_db()
    with SessionLocal() as db:
        events, pois = load_training_data(db)
        cyber_observations = load_cyber_training_data(db)

    if len(events) < args.min_events and len(cyber_observations) < args.min_events:
        logging.error(
            "Training aborted: only %d spatial events and %d cyber observations found",
            len(events), len(cyber_observations),
        )
        return 2

    output = Path(args.output)
    if not output.is_absolute():
        output = BACKEND_ROOT / output
    output.parent.mkdir(parents=True, exist_ok=True)
    spatial_metrics = {"status": "skipped", "message": "Insufficient spatial events"}
    if len(events) >= args.min_events:
        predictor = RiskPredictor()
        spatial_metrics = predictor.train(events=events, pois=pois, lookback_days=args.lookback_days)
        if spatial_metrics.get("status") == "success":
            predictor.save_model(str(output))

    cyber_engine = CyberForecastEngine()
    cyber_metrics = cyber_engine.train(cyber_observations)
    if cyber_metrics.get("status") == "success":
        cyber_engine.save_model(str(output.with_name("cyber_forecast_latest.pkl")))

    if spatial_metrics.get("status") != "success" and cyber_metrics.get("status") != "success":
        logging.error("Neither forecast model had sufficient trainable history")
        return 3

    report = {
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "event_count": len(events),
        "poi_count": len(pois),
        "model_path": str(output),
        "cyber_observation_count": len(cyber_observations),
        "metrics": {"spatial": spatial_metrics, "cyber": cyber_metrics},
    }
    report_path = output.with_suffix(".metrics.json")
    report_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
    logging.info("Model and metrics saved successfully")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
