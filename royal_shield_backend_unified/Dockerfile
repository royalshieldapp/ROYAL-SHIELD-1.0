# Royal Shield Unified Backend
FROM python:3.10-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    libpq-dev \
    libgdal-dev \
    gdal-bin \
    libgeos-dev \
    libproj-dev \
    postgresql-client \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copy production requirements
COPY requirements-render.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements-render.txt

# Copy application code
COPY . .

# Create logs directory
RUN mkdir -p logs

# Expose API port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8000/health').raise_for_status()"

# Start application
CMD uvicorn api.main:app --host 0.0.0.0 --port ${PORT:-8000}
