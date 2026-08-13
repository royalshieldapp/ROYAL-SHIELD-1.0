"""Train the CISA KEV catalog-addition volume model from a local CSV."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from services.ml.kev_volume_training import run_training


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        if not reader.fieldnames or "event_time" not in reader.fieldnames:
            raise ValueError("CSV must contain an event_time column")
        return list(reader)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Train a reproducible forecast of CISA KEV catalog-addition volume."
    )
    parser.add_argument("--input", required=True, type=Path, help="Local CISA KEV CSV")
    parser.add_argument("--output-dir", required=True, type=Path, help="Artifact/report directory")
    parser.add_argument("--min-history-days", type=int, default=60)
    parser.add_argument("--validation-days", type=int, default=14)
    parser.add_argument("--horizon-days", type=int, default=7)
    args = parser.parse_args()
    if args.min_history_days < 1 or args.validation_days < 1 or args.horizon_days < 1:
        parser.error("history, validation, and horizon values must be positive")
    if not args.input.is_file():
        parser.error(f"input does not exist: {args.input}")

    try:
        rows = load_rows(args.input)
    except ValueError as error:
        parser.error(str(error))
    result = run_training(
        rows,
        output_directory=args.output_dir,
        min_history_days=args.min_history_days,
        validation_days=args.validation_days,
        horizon_days=args.horizon_days,
    )
    result["inputFile"] = args.input.name
    result["inputSha256"] = sha256_file(args.input)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    report_path = args.output_dir / "kev_volume_training_report.json"
    report_path.write_text(json.dumps(result, indent=2, sort_keys=True), encoding="utf-8")
    print(json.dumps(result, sort_keys=True))
    return 0 if result["status"] in {"READY", "DEGRADED"} else 2


if __name__ == "__main__":
    raise SystemExit(main())
