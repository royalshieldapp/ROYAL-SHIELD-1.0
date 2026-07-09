# Royal Shield Unified Backend v2.0.0

🛡️ **Backend unificado para la app Royal Shield Android**

Fusiona el **Risk Prediction Engine** (Python/FastAPI) y los **App Services** (Node.js/Express) en un solo servicio.

## 🏗️ Arquitectura

**Stack:**
- **Framework:** FastAPI 0.104+
- **Database:** PostgreSQL 15 + PostGIS 3.4
- **Caching:** Redis 7
- **ML:** XGBoost, LightGBM, Scikit-learn
- **Geospatial:** H3, Shapely, GeoAlchemy2
- **SMS:** Twilio
- **Scanning:** VirusTotal API
- **Task Queue:** Celery + Redis

## 📡 API Endpoints

### Core
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Root / info |
| GET | `/health` | Health check |
| GET | `/api/v1/info` | Feature flags y configuración |

### Risk Prediction (Motor de predicción de riesgo)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/risk-map` | Mapa de riesgo por bounding box |
| GET | `/api/v1/risk-zones/{h3_cell}` | Detalles de zona de riesgo |
| GET | `/api/v1/risk-history` | Historial de riesgo |
| GET | `/api/v1/hotspots` | Hotspots actuales (DBSCAN) |
| GET | `/api/v1/hotspots/predict` | Predicción de hotspots futuros |
| GET | `/api/v1/hotspots/nearby` | Hotspots cercanos |
| POST | `/api/v1/predict/risk` | Predicción de riesgo por ubicación |
| GET | `/api/v1/predict/explain` | Explicación SHAP de predicción |
| GET | `/api/v1/predict/trends` | Pronóstico de tendencias |

### App Services (Servicios de la app)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/phone/check?number=` | Verificar número de teléfono (scam) |
| POST | `/api/v1/sos/alert` | Alerta SOS con SMS a contactos |
| POST | `/api/v1/scan/url` | Escanear URL con VirusTotal |
| GET | `/api/v1/scan/result/{id}` | Resultado de escaneo |
| POST | `/api/v1/scan/file-hash` | Buscar hash de archivo |
| GET | `/api/v1/threats?lat=&lng=` | Amenazas cyber por ubicación |
| GET | `/api/v1/loyalty/status` | Estado de puntos de lealtad |
| POST | `/api/v1/loyalty/points` | Agregar puntos |
| POST | `/api/v1/business/quote` | Solicitud de cotización B2B |
| GET | `/api/v1/courses` | Catálogo de cursos |
| GET | `/api/v1/system/status` | Estado del sistema |

## 🚀 Quick Start

### Docker (Recomendado)
```bash
cd royal_shield_backend_unified

# 1. Configurar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales

# 2. Levantar todos los servicios
docker-compose up -d

# 3. Ver logs
docker-compose logs -f api

# 4. Acceder
# API: http://localhost:8000
# Docs: http://localhost:8000/docs
```

### Local Development
```bash
# 1. Crear virtualenv
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate   # Windows

# 2. Instalar dependencias
pip install -r requirements.txt

# 3. Configurar .env
cp .env.example .env

# 4. Iniciar FastAPI
uvicorn api.main:app --reload --port 8000
```

### Desde el emulador Android
La app se conecta al backend usando `http://10.0.2.2:8000` (localhost del host desde el emulador).

## 🔑 Credenciales necesarias

| Servicio | Variable | Registro |
|----------|----------|----------|
| VirusTotal | `VIRUSTOTAL_API_KEY` | [virustotal.com](https://www.virustotal.com/gui/join-us) |
| Twilio SMS | `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` | [twilio.com](https://www.twilio.com/try-twilio) |
| FBI Crime | `FBI_CRIME_API_KEY` | [api.data.gov](https://api.data.gov/signup/) |
| NASA FIRMS | `NASA_FIRMS_API_KEY` | [firms.modaps.eosdis.nasa.gov](https://firms.modaps.eosdis.nasa.gov/api/area/) |
| News API | `NEWS_API_KEY` | [newsapi.org](https://newsapi.org/register) |

## 📁 Estructura del proyecto

```
royal_shield_backend_unified/
├── api/
│   ├── main.py              # FastAPI app principal
│   └── routes/
│       ├── risk_maps.py      # Risk heatmaps (ML)
│       ├── hotspots.py       # DBSCAN hotspot detection
│       ├── predictions.py    # XGBoost risk prediction
│       ├── phone.py          # Phone scam check
│       ├── sos.py            # SOS emergency SMS
│       ├── scan.py           # VirusTotal scanning
│       ├── threats.py        # Threat intelligence
│       ├── loyalty.py        # Loyalty points
│       ├── business.py       # B2B quotes
│       ├── courses.py        # Education catalog
│       └── system.py         # System status
├── config/
│   ├── settings.py           # Pydantic settings
│   └── .env.example
├── services/
│   ├── data_ingestion/       # FBI, Miami-Dade, NASA collectors
│   ├── geospatial/           # PostGIS, H3 grid
│   │   └── database/
│   │       ├── connection.py
│   │       └── schema.sql    # Full PostGIS schema (583 lines)
│   └── ml/                   # XGBoost, DBSCAN, Feature Engineering
├── .env.example
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

**Royal Shield — Unified Backend v2.0.0** 🛡️
