from datetime import datetime, timedelta, timezone

from services.ml.cisa_kev_training import evaluate_cisa_kev_dataset, run_cisa_kev_training


def test_single_class_cisa_kev_dataset_is_unavailable_after_temporal_split():
    """A missing negative class must not produce a classifier or performance score."""
    start = datetime(2026, 7, 1, tzinfo=timezone.utc)
    records = [
        {
            "cve_id": f"CVE-2026-{index:04d}",
            "event_time": (start + timedelta(days=index)).isoformat(),
            "label": "exploited",
            "prediction_target": "exploit_likelihood",
        }
        for index in range(5)
    ]

    report = evaluate_cisa_kev_dataset(records, dataset_sha256="abc123")

    assert report["status"] == "UNAVAILABLE"
    assert report["reason"] == "INSUFFICIENT_LABEL_DIVERSITY"
    assert report["temporalValidation"] == {
        "strategy": "chronological_holdout",
        "trainRows": 4,
        "validationRows": 1,
        "trainEnd": "2026-07-04T00:00:00+00:00",
        "validationStart": "2026-07-05T00:00:00+00:00",
    }
    assert report["metrics"] == {"rocAuc": None, "prAuc": None, "brierScore": None}
    assert report["modelArtifact"] is None
    assert report["artifactVersion"] == "cisa-kev-training-run-v1+abc123"


def test_run_writes_a_versioned_report_but_no_model_for_single_class_csv(tmp_path):
    dataset = tmp_path / "kev.csv"
    dataset.write_text(
        "cve_id,event_time,label,prediction_target\n"
        "CVE-2026-0001,2026-07-01T00:00:00Z,exploited,exploit_likelihood\n"
        "CVE-2026-0002,2026-07-02T00:00:00Z,exploited,exploit_likelihood\n"
        "CVE-2026-0003,2026-07-03T00:00:00Z,exploited,exploit_likelihood\n"
        "CVE-2026-0004,2026-07-04T00:00:00Z,exploited,exploit_likelihood\n"
        "CVE-2026-0005,2026-07-05T00:00:00Z,exploited,exploit_likelihood\n",
        encoding="utf-8",
    )

    report_path, report = run_cisa_kev_training(dataset, tmp_path / "artifacts")

    assert report["status"] == "UNAVAILABLE"
    assert report_path.exists()
    assert report_path.name.startswith("cisa-kev-training-run-")
    assert not list((tmp_path / "artifacts").glob("*.pkl"))


def test_temporal_holdout_never_splits_records_with_the_same_catalog_date():
    records = [
        {"cve_id": "CVE-1", "event_time": "2026-07-01T00:00:00Z", "label": "exploited", "prediction_target": "exploit_likelihood"},
        {"cve_id": "CVE-2", "event_time": "2026-07-02T00:00:00Z", "label": "exploited", "prediction_target": "exploit_likelihood"},
        {"cve_id": "CVE-3", "event_time": "2026-07-02T00:00:00Z", "label": "exploited", "prediction_target": "exploit_likelihood"},
        {"cve_id": "CVE-4", "event_time": "2026-07-03T00:00:00Z", "label": "exploited", "prediction_target": "exploit_likelihood"},
        {"cve_id": "CVE-5", "event_time": "2026-07-03T00:00:00Z", "label": "exploited", "prediction_target": "exploit_likelihood"},
    ]

    report = evaluate_cisa_kev_dataset(records, dataset_sha256="date-boundary")

    assert report["temporalValidation"]["trainEnd"] < report["temporalValidation"]["validationStart"]
