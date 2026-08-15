from datetime import datetime, timezone

from api.contracts.status import degraded, ready, unavailable


def test_unavailable_has_no_fabricated_data():
    result = unavailable("NO_E2E_EVIDENCE")

    assert result.status == "UNAVAILABLE"
    assert result.reason == "NO_E2E_EVIDENCE"
    assert result.data is None
    assert result.confidence is None
    assert result.sources == []
    assert result.generatedAt.tzinfo == timezone.utc


def test_ready_requires_real_source_freshness_and_data():
    freshness = datetime(2026, 8, 15, 14, 0, tzinfo=timezone.utc)

    result = ready(
        data={"sentCount": 1},
        sources=["twilio"],
        data_freshness=freshness,
        confidence=1.0,
    )

    assert result.status == "READY"
    assert result.data == {"sentCount": 1}
    assert result.sources == ["twilio"]
    assert result.dataFreshness == freshness
    assert result.confidence == 1.0


def test_degraded_keeps_limitation_visible():
    result = degraded(
        "PARTIAL_PROVIDER_RESPONSE",
        data={"sentCount": 1, "failedCount": 1},
        sources=["twilio"],
    )

    assert result.status == "DEGRADED"
    assert result.reason == "PARTIAL_PROVIDER_RESPONSE"
    assert result.data == {"sentCount": 1, "failedCount": 1}


def test_default_source_lists_are_independent():
    first = unavailable("FIRST")
    second = unavailable("SECOND")

    first.sources.append("unexpected")

    assert second.sources == []
