"""Reproducible, leakage-aware local training for a CVE outcome dataset."""
import csv
import hashlib
import json
import math
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib
from sklearn.feature_extraction import DictVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import balanced_accuracy_score, precision_score, recall_score


LEAKAGE_COLUMNS = {"cisa_date_added", "cisa_due_date", "cisa_required_action"}
FEATURE_COLUMNS = ("attack_vector", "cwe_id")
REQUIRED_COLUMNS = {"cve_id", "event_time", "label"}
MINIMUM_RECORDS = 20


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _unavailable(reason: str, valid_records: int = 0) -> dict[str, Any]:
    return {
        "status": "UNAVAILABLE",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "dataFreshness": None,
        "sources": ["CISA Known Exploited Vulnerabilities Catalog"],
        "modelVersion": None,
        "confidence": None,
        "reason": reason,
        "validRecords": valid_records,
    }


def _read_rows(source: Path) -> tuple[list[dict[str, str]], str | None]:
    with source.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if not reader.fieldnames or not REQUIRED_COLUMNS.issubset(reader.fieldnames):
            return [], "MISSING_REQUIRED_COLUMNS"
        if LEAKAGE_COLUMNS.intersection(reader.fieldnames):
            return [], "LEAKAGE_PRONE_COLUMNS_PRESENT"
        rows = []
        for row in reader:
            try:
                parsed = datetime.fromisoformat(row["event_time"].replace("Z", "+00:00"))
            except (KeyError, ValueError, AttributeError):
                continue
            if row.get("label") not in {"exploited", "not_exploited"}:
                continue
            row["_event_time"] = parsed.astimezone(timezone.utc).isoformat()
            rows.append(row)
    return sorted(rows, key=lambda row: row["_event_time"]), None


def evaluate_cisa_kev_dataset(records: list[dict[str, Any]], dataset_sha256: str) -> dict[str, Any]:
    """Evaluate training eligibility without manufacturing a model or metrics.

    The chronological split is recorded even for an unavailable run so the
    limitation is auditable and cannot be mistaken for random validation.
    """
    valid_rows = []
    for record in records:
        try:
            event_time = datetime.fromisoformat(str(record["event_time"]).replace("Z", "+00:00")).astimezone(timezone.utc)
        except (KeyError, TypeError, ValueError):
            continue
        if record.get("label") not in {"exploited", "not_exploited"}:
            continue
        valid_rows.append({**record, "_event_time": event_time.isoformat()})
    valid_rows.sort(key=lambda row: row["_event_time"])
    unique_dates = sorted({row["_event_time"][:10] for row in valid_rows})
    validation_date_count = max(1, math.ceil(len(unique_dates) * 0.2)) if unique_dates else 0
    validation_start_date = unique_dates[-validation_date_count] if validation_date_count else None
    train_rows = [row for row in valid_rows if row["_event_time"][:10] < validation_start_date]
    validation_rows = [row for row in valid_rows if row["_event_time"][:10] >= validation_start_date]
    temporal_validation = {
        "strategy": "chronological_holdout",
        "trainRows": len(train_rows),
        "validationRows": len(validation_rows),
        "trainEnd": train_rows[-1]["_event_time"] if train_rows else None,
        "validationStart": validation_rows[0]["_event_time"] if validation_rows else None,
    }
    version = f"cisa-kev-training-run-v1+{dataset_sha256}"
    if len({row["label"] for row in valid_rows}) < 2:
        return {
            "status": "UNAVAILABLE", "reason": "INSUFFICIENT_LABEL_DIVERSITY",
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "dataFreshness": valid_rows[-1]["_event_time"] if valid_rows else None,
            "sources": ["CISA Known Exploited Vulnerabilities Catalog"],
            "objective": "CVE exploit-likelihood classification requires positive and negative outcome labels.",
            "validRecords": len(valid_rows), "datasetSha256": dataset_sha256,
            "artifactVersion": version, "modelArtifact": None,
            "temporalValidation": temporal_validation,
            "metrics": {"rocAuc": None, "prAuc": None, "brierScore": None},
        }
    return {
        "status": "DEGRADED", "reason": "BINARY_TRAINING_NOT_IMPLEMENTED",
        "generatedAt": datetime.now(timezone.utc).isoformat(), "dataFreshness": valid_rows[-1]["_event_time"] if valid_rows else None,
        "sources": ["CISA Known Exploited Vulnerabilities Catalog"], "validRecords": len(valid_rows),
        "datasetSha256": dataset_sha256, "artifactVersion": version, "modelArtifact": None,
        "temporalValidation": temporal_validation, "metrics": {"rocAuc": None, "prAuc": None, "brierScore": None},
    }


def run_cisa_kev_training(source: Path, output_dir: Path) -> tuple[Path, dict[str, Any]]:
    """Persist a versioned eligibility report; never persist a false model."""
    source = Path(source)
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    source_hash = _sha256(source)
    rows, error = _read_rows(source)
    report = evaluate_cisa_kev_dataset(rows, source_hash) if error is None else _unavailable(error)
    report_path = output_dir / f"cisa-kev-training-run-{source_hash[:12]}.json"
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True), encoding="utf-8")
    return report_path, report


def train_cisa_kev_classifier(source: Path, output_dir: Path) -> dict[str, Any]:
    """Train only from temporally ordered, pre-outcome CVE rows.

    A one-class CISA KEV snapshot is intentionally rejected: KEV confirms
    exploitation and cannot alone estimate exploitation likelihood.
    """
    source = Path(source)
    if not source.is_file():
        return _unavailable("DATASET_NOT_FOUND")
    rows, error = _read_rows(source)
    if error:
        return _unavailable(error)
    if len({row["label"] for row in rows}) < 2:
        return _unavailable("INSUFFICIENT_LABEL_DIVERSITY", len(rows))
    if len(rows) < MINIMUM_RECORDS:
        return _unavailable("INSUFFICIENT_VALID_RECORDS", len(rows))

    split_index = int(len(rows) * 0.8)
    train_rows, validation_rows = rows[:split_index], rows[split_index:]
    if not validation_rows or len({row["label"] for row in train_rows}) < 2:
        return _unavailable("TEMPORAL_SPLIT_NOT_TRAINABLE", len(rows))

    vectorizer = DictVectorizer(sparse=True)
    train_features = vectorizer.fit_transform(
        [{key: row.get(key, "unknown") for key in FEATURE_COLUMNS} for row in train_rows]
    )
    validation_features = vectorizer.transform(
        [{key: row.get(key, "unknown") for key in FEATURE_COLUMNS} for row in validation_rows]
    )
    train_labels = [row["label"] for row in train_rows]
    validation_labels = [row["label"] for row in validation_rows]
    model = LogisticRegression(random_state=42, max_iter=1000, class_weight="balanced")
    model.fit(train_features, train_labels)
    predictions = model.predict(validation_features)

    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    artifact = output_dir / "cisa_kev_exploit_outcome.joblib"
    joblib.dump({"model": model, "vectorizer": vectorizer, "featureColumns": FEATURE_COLUMNS}, artifact)
    artifact_hash = _sha256(artifact)
    model_version = f"cisa-kev-{artifact_hash[:12]}"
    report = {
        "status": "READY",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "dataFreshness": rows[-1]["_event_time"],
        "sources": ["CISA Known Exploited Vulnerabilities Catalog"],
        "modelVersion": model_version,
        "confidence": None,
        "predictionTarget": "cve_exploitation_outcome",
        "metrics": {"validation": {
            "balancedAccuracy": float(balanced_accuracy_score(validation_labels, predictions)),
            "precisionExploited": float(precision_score(validation_labels, predictions, pos_label="exploited", zero_division=0)),
            "recallExploited": float(recall_score(validation_labels, predictions, pos_label="exploited", zero_division=0)),
            "samples": len(validation_rows),
        }},
        "split": {
            "strategy": "chronological_80_20",
            "trainEnd": train_rows[-1]["_event_time"],
            "validationStart": validation_rows[0]["_event_time"],
            "trainSamples": len(train_rows),
            "validationSamples": len(validation_rows),
        },
        "artifact": {"path": artifact.name, "sha256": artifact_hash},
        "inputSha256": _sha256(source),
    }
    (output_dir / "manifest.json").write_text(json.dumps(report, indent=2, sort_keys=True), encoding="utf-8")
    return report
