from datetime import date
import json
from pathlib import Path
import subprocess
import sys

from services.ml.kev_volume_training import (
    KEV_VOLUME_MODEL_VERSION,
    build_daily_supervised_frame,
    run_training,
    temporal_train_validation_split,
)


def test_builds_future_seven_day_volume_without_using_catalog_outcome_fields():
    rows = [
        {"event_time": "2026-01-01T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-02T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-03T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-04T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-05T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-06T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-07T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
        {"event_time": "2026-01-08T00:00:00Z", "label": "exploited", "features": '{"cisa_due_date":"2026-02-01"}'},
    ]

    dataset = build_daily_supervised_frame(rows, horizon_days=2)

    assert dataset.loc[dataset["forecast_date"] == date(2026, 1, 2), "target_next_horizon"].item() == 2
    assert set(dataset.columns) == {"forecast_date", "count_lag_1", "count_rolling_7", "day_of_week", "target_next_horizon"}


def test_temporal_split_keeps_all_training_dates_before_validation_dates():
    rows = [{"event_time": f"2026-01-{day:02d}T00:00:00Z"} for day in range(1, 31)]
    dataset = build_daily_supervised_frame(rows, horizon_days=7)

    train, validation = temporal_train_validation_split(dataset, validation_days=7)

    assert train["forecast_date"].max() < validation["forecast_date"].min()
    assert len(validation) == 7


def test_insufficient_history_returns_unavailable_without_model_artifact(tmp_path):
    rows = [{"event_time": f"2026-01-{day:02d}T00:00:00Z"} for day in range(1, 28)]

    result = run_training(rows, output_directory=tmp_path, min_history_days=60, validation_days=7)

    assert result["status"] == "UNAVAILABLE"
    assert result["reason"] == "INSUFFICIENT_TEMPORAL_HISTORY"
    assert result["modelVersion"] == KEV_VOLUME_MODEL_VERSION
    assert result["dataFreshness"] == "2026-01-27"
    assert not (tmp_path / "kev_volume_forecaster.joblib").exists()


def test_sufficient_history_writes_a_versioned_artifact_and_honest_metrics(tmp_path):
    rows = [
        {"event_time": f"2026-01-{day:02d}T00:00:00Z"}
        for day in range(1, 29)
        for _ in range((day % 4) + 1)
    ] + [
        {"event_time": f"2026-02-{day:02d}T00:00:00Z"}
        for day in range(1, 29)
        for _ in range((day % 4) + 1)
    ] + [
        {"event_time": f"2026-03-{day:02d}T00:00:00Z"}
        for day in range(1, 29)
        for _ in range((day % 4) + 1)
    ]

    result = run_training(rows, output_directory=tmp_path, min_history_days=60, validation_days=14)

    assert result["status"] in {"READY", "DEGRADED"}
    assert result["metrics"]["validation_mae"] >= 0
    assert result["metrics"]["naive_validation_mae"] >= 0
    assert (tmp_path / "kev_volume_forecaster.joblib").exists()
    manifest = json.loads((tmp_path / "kev_volume_forecaster.manifest.json").read_text(encoding="utf-8"))
    assert manifest["modelVersion"] == KEV_VOLUME_MODEL_VERSION
    assert manifest["status"] == result["status"]
    assert len(manifest["artifactSha256"]) == 64


def test_cli_writes_a_checksumed_unavailable_report_for_short_history(tmp_path):
    source = tmp_path / "kev.csv"
    source.write_text("event_time,label\n2026-01-01T00:00:00Z,exploited\n", encoding="utf-8")
    output = tmp_path / "artifacts"
    script = Path(__file__).parents[1] / "scripts" / "train_kev_volume_model.py"

    completed = subprocess.run(
        [sys.executable, str(script), "--input", str(source), "--output-dir", str(output)],
        check=False,
        capture_output=True,
        text=True,
    )

    assert completed.returncode == 2
    report = json.loads((output / "kev_volume_training_report.json").read_text(encoding="utf-8"))
    assert report["status"] == "UNAVAILABLE"
    assert len(report["inputSha256"]) == 64
