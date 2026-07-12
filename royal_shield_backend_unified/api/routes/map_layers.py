"""
Royal Map layer endpoints.
Provides normalized map pins for the Android Royal Map screen.
"""
from datetime import datetime
from typing import List, Optional

import httpx
from fastapi import APIRouter, Query
from pydantic import BaseModel, Field

from config.settings import settings

router = APIRouter(prefix="/api/v1", tags=["Map Layers"])


class MapLayerPoint(BaseModel):
    id: str
    type: str = Field(..., description="camera | police | speed | protest | theft | cyber")
    title: str
    snippet: str
    lat: float
    lng: float
    severity: str = "LOW"
    source: str
    updated_at: str


class MapLayersResponse(BaseModel):
    layers: List[MapLayerPoint]
    total_count: int
    metadata: dict


OSM_LAYER_FILTERS = {
    "police": '["amenity"="police"]',
    "cameras": '["man_made"="surveillance"]',
    "speed": '["highway"="speed_camera"]',
}


@router.get("/map-layers", response_model=MapLayersResponse)
async def get_map_layers(
    bbox_min_lat: float = Query(settings.bbox_min_lat, ge=-90, le=90),
    bbox_min_lng: float = Query(settings.bbox_min_lng, ge=-180, le=180),
    bbox_max_lat: float = Query(settings.bbox_max_lat, ge=-90, le=90),
    bbox_max_lng: float = Query(settings.bbox_max_lng, ge=-180, le=180),
    layers: str = Query("cameras,police,speed,protest,theft,cyber"),
):
    requested = {
        item.strip().lower()
        for item in layers.split(",")
        if item.strip()
    }
    bbox = f"{bbox_min_lat},{bbox_min_lng},{bbox_max_lat},{bbox_max_lng}"
    points: List[MapLayerPoint] = []
    sources = []

    for layer_name, osm_filter in OSM_LAYER_FILTERS.items():
        if layer_name in requested:
            osm_points = await _fetch_osm_layer(layer_name, bbox, osm_filter)
            points.extend(osm_points)
            if osm_points:
                sources.append(f"osm:{layer_name}")

    points.extend(_forecast_seed_layers(requested, bbox_min_lat, bbox_min_lng, bbox_max_lat, bbox_max_lng))

    return MapLayersResponse(
        layers=points[:250],
        total_count=min(len(points), 250),
        metadata={
            "bbox": {
                "min_lat": bbox_min_lat,
                "min_lng": bbox_min_lng,
                "max_lat": bbox_max_lat,
                "max_lng": bbox_max_lng,
            },
            "requested_layers": sorted(requested),
            "sources": sources + ["forecast_seed"],
            "generated_at": datetime.utcnow().isoformat(),
        },
    )


async def _fetch_osm_layer(layer_name: str, bbox: str, osm_filter: str) -> List[MapLayerPoint]:
    query = f"""
    [out:json][timeout:25];
    (
      node{osm_filter}({bbox});
      way{osm_filter}({bbox});
      relation{osm_filter}({bbox});
    );
    out center tags;
    """
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(settings.overpass_api_url, data={"data": query})
            response.raise_for_status()
            elements = response.json().get("elements", [])
    except Exception:
        return []

    points: List[MapLayerPoint] = []
    for element in elements:
        lat = element.get("lat") or element.get("center", {}).get("lat")
        lng = element.get("lon") or element.get("center", {}).get("lon")
        if lat is None or lng is None:
            continue
        tags = element.get("tags", {})
        points.append(
            MapLayerPoint(
                id=f"osm-{layer_name}-{element.get('id')}",
                type=_android_type(layer_name),
                title=tags.get("name") or _default_title(layer_name),
                snippet=tags.get("operator") or tags.get("description") or "OpenStreetMap signal",
                lat=float(lat),
                lng=float(lng),
                severity="LOW" if layer_name == "police" else "MEDIUM",
                source="openstreetmap",
                updated_at=datetime.utcnow().isoformat(),
            )
        )
    return points


def _forecast_seed_layers(requested: set, min_lat: float, min_lng: float, max_lat: float, max_lng: float) -> List[MapLayerPoint]:
    center_lat = (min_lat + max_lat) / 2
    center_lng = (min_lng + max_lng) / 2
    seeds = [
        ("protest", "Forecast Protest", "News and OSINT watch area", center_lat + 0.006, center_lng - 0.006, "MEDIUM"),
        ("theft", "Theft Hotspot", "Crime forecast watch area", center_lat - 0.005, center_lng + 0.007, "HIGH"),
        ("cyber", "Cyber Threat", "Threat intelligence signal", center_lat + 0.003, center_lng + 0.009, "MEDIUM"),
    ]
    return [
        MapLayerPoint(
            id=f"forecast-{layer_type}-seed",
            type=layer_type,
            title=title,
            snippet=snippet,
            lat=lat,
            lng=lng,
            severity=severity,
            source="forecast_engine",
            updated_at=datetime.utcnow().isoformat(),
        )
        for layer_type, title, snippet, lat, lng, severity in seeds
        if layer_type in requested
    ]


def _android_type(layer_name: str) -> str:
    return "camera" if layer_name == "cameras" else layer_name


def _default_title(layer_name: str) -> str:
    return {
        "cameras": "Surveillance Camera",
        "police": "Police Point",
        "speed": "Speed Zone",
    }.get(layer_name, "Map Signal")
