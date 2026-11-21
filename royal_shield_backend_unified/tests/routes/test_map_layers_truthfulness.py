"""Truthfulness checks for Royal Map provider-backed layers."""

from fastapi import FastAPI
from fastapi.testclient import TestClient

from api.routes import map_layers


def test_map_layers_never_invent_forecast_points_when_no_provider_returns_data() -> None:
    """Catch regressions that turn an empty provider result into synthetic pins."""

    app = FastAPI()
    app.include_router(map_layers.router)

    with TestClient(app) as client:
        response = client.get(
            "/api/v1/map-layers",
            params={"layers": "protest,theft,cyber"},
        )

    assert response.status_code == 200
    payload = response.json()
    assert payload["layers"] == []
    assert payload["total_count"] == 0
    assert payload["metadata"]["sources"] == []

