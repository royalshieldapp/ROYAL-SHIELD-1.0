"""
Database connection manager for Royal Shield Backend
Provides SQLAlchemy engine and session management
"""
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from contextlib import contextmanager
from typing import Generator
import logging

from config.settings import settings

logger = logging.getLogger(__name__)

database_url = settings.database_url
engine_kwargs = {
    "pool_pre_ping": True,
    "echo": settings.is_development,
}

if database_url.startswith("sqlite"):
    engine_kwargs["connect_args"] = {"check_same_thread": False}
else:
    engine_kwargs.update(
        {
            "pool_size": 10,
            "max_overflow": 20,
        }
    )

# SQLAlchemy engine
engine = create_engine(database_url, **engine_kwargs)

# Session factory
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for models
Base = declarative_base()


def get_db() -> Generator[Session, None, None]:
    """
    Dependency for FastAPI routes to get database session

    Usage:
        @app.get("/items")
        def read_items(db: Session = Depends(get_db)):
            items = db.query(Item).all()
            return items
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@contextmanager
def get_db_context() -> Generator[Session, None, None]:
    """
    Context manager for database sessions outside of FastAPI

    Usage:
        with get_db_context() as db:
            items = db.query(Item).all()
    """
    db = SessionLocal()
    try:
        yield db
        db.commit()
    except Exception as e:
        db.rollback()
        logger.error(f"Database error: {e}")
        raise
    finally:
        db.close()


def init_db():
    """Initialize database (create tables if not exist)"""
    logger.info("Initializing database at host: %s", settings.database_host)
    Base.metadata.create_all(bind=engine)
    logger.info("Database initialized successfully")


def close_db():
    """Close database connection pool"""
    logger.info("Closing database connections...")
    engine.dispose()
    logger.info("Database connections closed")
