"""Create a reproducible CISA KEV training-readiness report from a local CSV."""
import argparse
import logging
import sys
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[1]
if str(BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(BACKEND_ROOT))

from services.ml.cisa_kev_training import run_cisa_kev_training


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Evaluate a CISA KEV-derived CSV for leakage-safe temporal model training."
    )
    parser.add_argument("--dataset", required=True, type=Path, help="Local CSV to evaluate")
    parser.add_argument(
        "--output-dir", type=Path, default=BACKEND_ROOT / "artifacts" / "cisa-kev",
        help="Directory for the versioned report (default: artifacts/cisa-kev)",
    )
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    if not args.dataset.is_file():
        logging.error("Dataset not found: %s", args.dataset)
        return 2
    try:
        report_path, report = run_cisa_kev_training(args.dataset, args.output_dir)
    except ValueError as exc:
        logging.error("Dataset rejected: %s", exc)
        return 2
    logging.info("Training readiness: %s (%s)", report["status"], report["reason"])
    logging.info("Versioned report: %s", report_path)
    return 0 if report["status"] == "READY" else 3


if __name__ == "__main__":
    raise SystemExit(main())
