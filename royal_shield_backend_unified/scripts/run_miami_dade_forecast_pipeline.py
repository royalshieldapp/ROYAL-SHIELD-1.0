"""Run an explicitly configured Miami-Dade Forecast V2 ingestion and training job."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Sequence

BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from config.settings import settings
from services.forecast.pipeline import run_miami_dade_training
from services.geospatial.database.connection import SessionLocal, init_db


def parse_horizons(value: str) -> tuple[int, ...]:
    """Parse a comma-separated, positive-day horizon list."""

    try:
        horizons = tuple(int(part.strip()) for part in value.split(",") if part.strip())
    except ValueError as error:
        raise argparse.ArgumentTypeError("horizons must be comma-separated integers") from error
    if not horizons or any(horizon < 1 for horizon in horizons):
        raise argparse.ArgumentTypeError("horizons must contain positive day counts")
    return horizons


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Fetch one approved public PCB ArcGIS layer, persist privacy-minimised "
            "H3 aggregates, and train/publish Forecast V2 only if it passes its gates."
        )
    )
    parser.add_argument(
        "--execute",
        action="store_true",
        help="Required acknowledgement that the command writes durable observations and training records.",
    )
    parser.add_argument("--layer-id", type=int, required=True, help="Approved PCB ArcGIS layer id.")
    parser.add_argument(
        "--event-type",
        required=True,
        help="Verified event category represented by the selected layer.",
    )
    parser.add_argument(
        "--min-samples",
        type=int,
        required=True,
        help="Minimum chronological samples required for a candidate model.",
    )
    parser.add_argument(
        "--horizons",
        type=parse_horizons,
        default=(7, 30),
        help="Comma-separated forecast horizons in days; defaults to 7,30.",
    )
    parser.add_argument(
        "--h3-resolution",
        type=int,
        default=settings.default_h3_resolution,
        help="H3 resolution persisted into the artifact; defaults to the API configuration.",
    )
    parser.add_argument(
        "--source-dataset",
        default="PCB_Report",
        help="Approved source dataset identifier; defaults to PCB_Report.",
    )
    return parser


def _summary(result) -> dict[str, object]:
    training = result.training
    return {
        "layer_id": result.layer_id,
        "event_type": result.event_type,
        "h3_resolution": result.resolution,
        "aggregation": {
            "total_records": result.aggregation.total_records,
            "valid": result.aggregation.valid,
            "invalid": result.aggregation.invalid,
            "missing_police_grid": result.aggregation.missing_police_grid,
            "observations_prepared": result.aggregation.observations_prepared,
        },
        "inserted_observations": result.inserted_observations,
        "model_version": training.version,
        "promoted": training.promoted,
        "safe_reason": training.safe_reason,
        "sample_count": training.sample_count,
        "source_start": training.source_start.isoformat() if training.source_start else None,
        "source_end": training.source_end.isoformat() if training.source_end else None,
    }


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if not args.execute:
        parser.error("--execute is required; this job writes durable data and training records")

    init_db()
    session = SessionLocal()
    try:
        result = run_miami_dade_training(
            session=session,
            layer_id=args.layer_id,
            event_type=args.event_type,
            resolution=args.h3_resolution,
            min_samples=args.min_samples,
            horizons=args.horizons,
            source_dataset=args.source_dataset,
        )
        print(json.dumps(_summary(result), sort_keys=True))
        return 0 if result.training.promoted else 2
    finally:
        session.close()


if __name__ == "__main__":
    raise SystemExit(main())
