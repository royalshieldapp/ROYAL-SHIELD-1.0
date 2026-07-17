"""
Configuration management for Royal Shield Backend
Loads environment variables and provides typed access to settings
"""
import os
from typing import Optional
from urllib.parse import urlparse
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # =============================================================================
    # DATABASE
    # =============================================================================
    database_url_env: Optional[str] = Field(default=None, alias="DATABASE_URL")
    postgres_host: str = Field(default="localhost", alias="POSTGRES_HOST")
    postgres_port: int = Field(default=5432, alias="POSTGRES_PORT")
    postgres_db: str = Field(default="royal_shield_risk_db", alias="POSTGRES_DB")
    postgres_user: str = Field(default="postgres", alias="POSTGRES_USER")
    postgres_password: str = Field(default="", alias="POSTGRES_PASSWORD")

    @property
    def database_url(self) -> str:
        """PostgreSQL connection URL or SQLite fallback for local development"""
        db_url_env = self.database_url_env or os.getenv("DATABASE_URL")
        if db_url_env:
            if db_url_env.startswith("postgres://"):
                return db_url_env.replace("postgres://", "postgresql://", 1)
            return db_url_env

        if self.is_production:
            raise ValueError(
                "DATABASE_URL is required in production. Configure it in Render "
                "with the Postgres connectionString instead of using localhost."
            )

        if self.postgres_host == "localhost":
            return "sqlite:///./dev.db"

        return (
            f"postgresql://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )

    @property
    def database_host(self) -> str:
        """Return the configured database host for safe diagnostics."""
        if self.database_url_env:
            parsed = urlparse(self.database_url)
            return parsed.hostname or "unknown"
        return self.postgres_host

    # =============================================================================
    # CRIME DATA APIS
    # =============================================================================
    fbi_crime_api_key: Optional[str] = Field(default=None, alias="FBI_CRIME_API_KEY")
    miami_dade_app_token: Optional[str] = Field(default=None, alias="MIAMI_DADE_APP_TOKEN")

    # =============================================================================
    # ENVIRONMENTAL DATA APIS
    # =============================================================================
    nasa_firms_api_key: Optional[str] = Field(default=None, alias="NASA_FIRMS_API_KEY")
    noaa_cdo_token: Optional[str] = Field(default=None, alias="NOAA_CDO_TOKEN")
    overpass_api_url: str = Field(
        default="https://overpass-api.de/api/interpreter",
        alias="OVERPASS_API_URL",
    )

    # =============================================================================
    # OSINT FEEDS
    # =============================================================================
    reddit_client_id: Optional[str] = Field(default=None, alias="REDDIT_CLIENT_ID")
    reddit_client_secret: Optional[str] = Field(default=None, alias="REDDIT_CLIENT_SECRET")
    reddit_user_agent: str = Field(default="RoyalShield/1.0", alias="REDDIT_USER_AGENT")

    news_api_key: Optional[str] = Field(default=None, alias="NEWS_API_KEY")

    # Cyber threat intelligence. CISA KEV is public and requires no key.
    otx_api_key: Optional[str] = Field(default=None, alias="OTX_API_KEY")
    abuseipdb_api_key: Optional[str] = Field(default=None, alias="ABUSEIPDB_API_KEY")
    cyber_feed_limit: int = Field(default=500, alias="CYBER_FEED_LIMIT")

    twitter_bearer_token: Optional[str] = Field(default=None, alias="TWITTER_BEARER_TOKEN")
    twitter_api_key: Optional[str] = Field(default=None, alias="TWITTER_API_KEY")
    twitter_api_secret: Optional[str] = Field(default=None, alias="TWITTER_API_SECRET")

    # =============================================================================
    # VECTOR DATABASE
    # =============================================================================
    pinecone_api_key: Optional[str] = Field(default=None, alias="PINECONE_API_KEY")
    pinecone_environment: str = Field(default="us-west1-gcp", alias="PINECONE_ENVIRONMENT")
    pinecone_index_name: str = Field(default="royal-shield-zones", alias="PINECONE_INDEX_NAME")

    # =============================================================================
    # AI/ML SERVICES
    # =============================================================================
    openai_api_key: Optional[str] = Field(default=None, alias="OPENAI_API_KEY")
    use_local_embeddings: bool = Field(default=True, alias="USE_LOCAL_EMBEDDINGS")
    embedding_model: str = Field(
        default="sentence-transformers/all-MiniLM-L6-v2",
        alias="EMBEDDING_MODEL",
    )

    # =============================================================================
    # API SECURITY
    # =============================================================================
    api_internal_secret: str = Field(default="royal_shield_internal_secret_CHANGE_THIS_IN_PRODUCTION", alias="API_INTERNAL_SECRET")
    jwt_secret: str = Field(
        default="CHANGE_THIS_SECRET_KEY_IN_PRODUCTION",
        alias="JWT_SECRET",
    )
    jwt_algorithm: str = Field(default="HS256", alias="JWT_ALGORITHM")
    jwt_expire_minutes: int = Field(default=60, alias="JWT_EXPIRE_MINUTES")
    rate_limit_per_minute: int = Field(default=60, alias="RATE_LIMIT_PER_MINUTE")

    # =============================================================================
    # APPLICATION
    # =============================================================================
    environment: str = Field(default="development", alias="ENVIRONMENT")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    log_file: str = Field(default="logs/royal_shield_backend.log", alias="LOG_FILE")

    data_collection_interval_hours: int = Field(default=6, alias="DATA_COLLECTION_INTERVAL_HOURS")
    model_retrain_interval_days: int = Field(default=7, alias="MODEL_RETRAIN_INTERVAL_DAYS")

    # Cache
    redis_host: str = Field(default="localhost", alias="REDIS_HOST")
    redis_port: int = Field(default=6379, alias="REDIS_PORT")
    redis_password: Optional[str] = Field(default=None, alias="REDIS_PASSWORD")
    cache_ttl_seconds: int = Field(default=3600, alias="CACHE_TTL_SECONDS")

    # =============================================================================
    # MIAMI-DADE CONFIGURATION
    # =============================================================================
    bbox_min_lat: float = Field(default=25.1398, alias="BBOX_MIN_LAT")
    bbox_min_lng: float = Field(default=-80.8738, alias="BBOX_MIN_LNG")
    bbox_max_lat: float = Field(default=25.9740, alias="BBOX_MAX_LAT")
    bbox_max_lng: float = Field(default=-80.1194, alias="BBOX_MAX_LNG")

    default_h3_resolution: int = Field(default=9, alias="DEFAULT_H3_RESOLUTION")

    # =============================================================================
    # FEATURE FLAGS
    # =============================================================================
    enable_crime_collection: bool = Field(default=True, alias="ENABLE_CRIME_COLLECTION")
    enable_environmental_collection: bool = Field(default=True, alias="ENABLE_ENVIRONMENTAL_COLLECTION")
    enable_osint_collection: bool = Field(default=True, alias="ENABLE_OSINT_COLLECTION")
    enable_cyber_collection: bool = Field(default=True, alias="ENABLE_CYBER_COLLECTION")
    enable_camera_integration: bool = Field(default=False, alias="ENABLE_CAMERA_INTEGRATION")
    enable_ml_predictions: bool = Field(default=True, alias="ENABLE_ML_PREDICTIONS")
    enable_vector_search: bool = Field(default=True, alias="ENABLE_VECTOR_SEARCH")

    # =============================================================================
    # UNIFIED INTEGRATIONS
    # =============================================================================
    gemini_api_key: Optional[str] = Field(default=None, alias="GEMINI_API_KEY")
    google_play_service_account_json: Optional[str] = Field(default=None, alias="GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    openclaw_base_url: Optional[str] = Field(default=None, alias="OPENCLAW_BASE_URL")
    openclaw_secret: Optional[str] = Field(default=None, alias="OPENCLAW_SECRET")
    vpn_provider: Optional[str] = Field(default=None, alias="VPN_PROVIDER")
    vpn_servers: str = Field(default="", alias="VPN_SERVERS")
    vpn_port: int = Field(default=51820, alias="VPN_PORT")
    vpn_dns: str = Field(default="1.1.1.1, 1.0.0.1", alias="VPN_DNS")
    vpn_server_public_key: Optional[str] = Field(default=None, alias="VPN_SERVER_PUBLIC_KEY")
    vpn_server_private_key: Optional[str] = Field(default=None, alias="VPN_SERVER_PRIVATE_KEY")
    vpn_allow_static_config: bool = Field(default=False, alias="VPN_ALLOW_STATIC_CONFIG")

    @field_validator("environment", mode="before")
    @classmethod
    def validate_environment(cls, v: str) -> str:
        """Validate environment value"""
        allowed = ["development", "staging", "production"]
        if v not in allowed:
            raise ValueError(f"Environment must be one of {allowed}")
        return v

    @property
    def is_production(self) -> bool:
        """Check if running in production"""
        return self.environment == "production"

    @property
    def is_development(self) -> bool:
        """Check if running in development"""
        return self.environment == "development"

    @property
    def miami_dade_bbox(self) -> tuple:
        """Get Miami-Dade bounding box as tuple"""
        return (
            self.bbox_min_lat,
            self.bbox_min_lng,
            self.bbox_max_lat,
            self.bbox_max_lng,
        )


@lru_cache()
def get_settings() -> Settings:
    """
    Get cached settings instance
    Call this function to access settings throughout the application
    """
    return Settings()


# Convenience exports
settings = get_settings()
