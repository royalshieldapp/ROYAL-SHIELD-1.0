from dataclasses import dataclass

import h3

from services.forecast.pipeline import run_miami_dade_training


@dataclass(frozen=True)
class FakeTrainingResult:
    version: str
    promoted: bool
    safe_reason: str | None


def test_pipeline_aggregates_real_source_shape_persists_then_trains() -> None:
    class SourceClient:
        closed = False

        def fetch_layer(self, layer_id: int):
            assert layer_id == 4
            return iter(
                (
                    {
                        "attributes": {
                            "Incident_Date": "2026-10-14",
                            "District": "MIAMI-DADE",
                            "Grid": "1361",
                        },
                        "geometry": {"y": 25.7617, "x": -80.1918},
                    },
                )
            )

        def close(self) -> None:
            self.closed = True

    client = SourceClient()
    persisted: dict[str, object] = {}
    trained: dict[str, object] = {}
    training_result = FakeTrainingResult(
        version="forecast-verified",
        promoted=True,
        safe_reason=None,
    )

    def persist(_session, aggregates, *, source_dataset: str) -> int:
        rows = list(aggregates)
        persisted["rows"] = rows
        persisted["source_dataset"] = source_dataset
        return len(rows)

    def train(_session, *, min_samples: int, horizons: tuple[int, ...]):
        trained["min_samples"] = min_samples
        trained["horizons"] = horizons
        return training_result

    result = run_miami_dade_training(
        session=object(),
        layer_id=4,
        event_type="verified_pcb_record",
        resolution=9,
        min_samples=12,
        horizons=(7, 30),
        client=client,
        persist=persist,
        train=train,
    )

    rows = persisted["rows"]
    assert len(rows) == 1
    assert rows[0].event_type == "verified_pcb_record"
    assert h3.h3_is_valid(rows[0].h3_cell)
    assert persisted["source_dataset"] == "PCB_Report"
    assert trained == {"min_samples": 12, "horizons": (7, 30)}
    assert result.inserted_observations == 1
    assert result.training is training_result
    assert client.closed is True
