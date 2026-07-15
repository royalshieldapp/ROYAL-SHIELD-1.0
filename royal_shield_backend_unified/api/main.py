"""
Royal Shield Unified Backend - FastAPI Application
Merges Risk Prediction Engine + App Services into a single API

Version: 2.0.0
"""
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import logging
from datetime import datetime
from pathlib import Path

from config.settings import settings
from services.geospatial.database.connection import init_db, close_db

log_path = Path(settings.log_file)
if log_path.parent != Path("."):
    log_path.parent.mkdir(parents=True, exist_ok=True)

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.log_level),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler(settings.log_file),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    # Startup
    logger.info("Starting Royal Shield Unified Backend v2.0.0...")
    logger.info(f"Environment: {settings.environment}")
    init_db()
    logger.info("Backend started successfully")

    yield

    # Shutdown
    logger.info("Shutting down Royal Shield Backend...")
    close_db()
    logger.info("Backend shutdown complete")


# Create FastAPI app
app = FastAPI(
    title="Royal Shield Unified API",
    description=(
        "Unified backend for Royal Shield Android app.\n\n"
        "**Risk Prediction Engine:** Crime/hazard forecasting with ML models, "
        "geospatial H3 indexing, and multi-source data ingestion.\n\n"
        "**App Services:** Phone scan, SOS alerts, VirusTotal scanning, "
        "threat intelligence, loyalty program, courses, and more."
    ),
    version="2.0.0",
    docs_url="/docs" if settings.is_development else None,
    redoc_url="/redoc" if settings.is_development else None,
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if settings.is_development else ["https://royalshield.app"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# GZip compression
app.add_middleware(GZipMiddleware, minimum_size=1000)


# Exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "message": str(exc) if settings.is_development else "An error occurred",
            "timestamp": datetime.utcnow().isoformat()
        }
    )


# ==============================================================================
# CORE ROUTES
# ==============================================================================

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "Royal Shield Unified API",
        "version": "2.0.0",
        "status": "operational",
        "environment": settings.environment,
        "timestamp": datetime.utcnow().isoformat()
    }


@app.get("/health")
async def health_check():
    """Health check endpoint for monitoring / container orchestration"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "environment": settings.environment
    }


@app.get("/api/v1/info")
async def api_info():
    """API information and feature flags"""
    return {
        "version": "2.0.0",
        "features": {
            # Risk Prediction features
            "crime_collection": settings.enable_crime_collection,
            "environmental_collection": settings.enable_environmental_collection,
            "osint_collection": settings.enable_osint_collection,
            "camera_integration": settings.enable_camera_integration,
            "ml_predictions": settings.enable_ml_predictions,
            "vector_search": settings.enable_vector_search,
            # App Service features
            "phone_check": True,
            "sos_alerts": True,
            "virustotal_scan": True,
            "threat_intelligence": True,
            "loyalty_program": True,
            "courses": True,
            "business_quotes": True,
        },
        "miami_dade_bbox": {
            "min_lat": settings.bbox_min_lat,
            "min_lng": settings.bbox_min_lng,
            "max_lat": settings.bbox_max_lat,
            "max_lng": settings.bbox_max_lng
        },
        "default_h3_resolution": settings.default_h3_resolution
    }


# ==============================================================================
# REGISTER ALL ROUTERS
# ==============================================================================

# Risk Prediction Engine routes (from original Python backend)
from api.routes import risk_maps, hotspots, predictions, map_layers
app.include_router(risk_maps.router)
app.include_router(hotspots.router)
app.include_router(predictions.router)
app.include_router(map_layers.router)

# App Service routes (ported from Node.js backend)
from api.routes import phone, sos, scan, threats, loyalty, business, courses, system, threat_map, cyber_news, xr, vpn
app.include_router(phone.router)
app.include_router(sos.router)
app.include_router(scan.router)
app.include_router(threats.router)
app.include_router(loyalty.router)
app.include_router(business.router)
app.include_router(courses.router)
app.include_router(system.router)
app.include_router(threat_map.router)
app.include_router(cyber_news.router)
app.include_router(xr.router)
app.include_router(vpn.router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.is_development,
        log_level=settings.log_level.lower()
    )
