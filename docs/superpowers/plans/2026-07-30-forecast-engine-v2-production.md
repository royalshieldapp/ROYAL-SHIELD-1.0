# Forecast Engine v2 Production Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move Forecast Engine v2 from the unified feature branch into the actual Render backend with real data, durable storage, honest API states, and Android end-to-end evidence.

**Architecture:** The production repository is `royalshieldapp/server-beckend`, checked out locally at `D:\APPS\ROYAL SHIELD\server-beckend`. Reuse reviewed FastAPI modules from `royal_shield_backend_unified`, while PostgreSQL stores source records and a persistent artifact store keeps trained models. Android consumes typed `READY`, `DEGRADED`, and `UNAVAILABLE` responses only.

**Tech Stack:** Python, FastAPI, SQLAlchemy, PostgreSQL, XGBoost, pytest, Kotlin, Jetpack Compose, Retrofit/OkHttp, Render.

## Global Constraints

- Never return a fabricated score, hotspot, confidence, timeline, source timestamp, or success result.
- Keep physical/geospatial and cyber intelligence data in independent pipelines.
- FBI data is regional/historical only; do not fabricate point coordinates.
- News/OSINT requires reliable geocoding before entering spatial training.
- Secrets remain in Render environment variables and are never committed, logged, or copied to Android.
- Use versioned migrations; inspect the live schema before adding tables.
- A result is complete only with source → PostgreSQL → model/service → API → Android evidence.
- Do not modify Registration, Splash, Dashboard, or Cyber screens without a separate approved UI scope.

---

## File map

| Path | Responsibility |
|---|---|
| `D:\APPS\ROYAL SHIELD\server-beckend\api\main.py` | Production FastAPI route registration. |
| `D:\APPS\ROYAL SHIELD\server-beckend\api\routes\predictions.py` | Honest spatial prediction/status API. |
| `D:\APPS\ROYAL SHIELD\server-beckend\api\routes\hotspots.py` | Real hotspot API or explicit unavailable response. |
| `D:\APPS\ROYAL SHIELD\server-beckend\services\data_ingestion\` | Isolated source collectors and persistence. |
| `D:\APPS\ROYAL SHIELD\server-beckend\services\ml\` | Feature creation, training, model metadata and inference. |
| `D:\APPS\ROYAL SHIELD\server-beckend\migrations\` | Versioned PostgreSQL migration files, using the repository's existing migration tool. |
| `D:\APPS\ROYAL SHIELD\server-beckend\tests\` | Backend unit/integration/API tests. |
| `app\src\main\java\com\royalshield\app\features\riskprediction\data\remote\RiskApiModels.kt` | Forecast API DTOs. |
| `app\src\main\java\com\royalshield\app\features\riskprediction\data\remote\RiskPredictionApi.kt` | Retrofit Forecast endpoints. |
| `app\src\main\java\com\royalshield\app\features\riskprediction\data\repository\RiskPredictionRepository.kt` | Backend result mapping. |
| `app\src\main\java\com\royalshield\app\features\riskprediction\ui\RiskMapViewModel.kt` | Removes simulated fallback hotspots and exposes honest UI state. |
| `app\src\main\java\com\royalshield\app\features\trackingshield\TrackingShieldViewModel.kt` | Removes simulated fallback hotspots in Tracking Shield. |
| `ROYAL_SHIELD_SIMULATION_WORKLOG_ENTRY_*.md` | Non-sensitive work evidence and validation results. |

## Task 1: Establish the production backend baseline

**Files:**
- Create: `docs/forecast-engine-v2-production-backend-matrix.md`
- Modify: `ROYAL_SHIELD_SIMULATION_WORKLOG_ENTRY_006_FORECAST_GIT_DESIGN.md`

**Produces:** A committed file-by-file matrix of `server-beckend` versus `royal_shield_backend_unified`, including exact route registrations, ORM/migration mechanism, dependency versions, Android client paths, and every simulated Forecast response.

- [ ] Clone `https://github.com/royalshieldapp/server-beckend.git` into `D:\APPS\ROYAL SHIELD\server-beckend` using the user's existing authenticated Git identity; do not request or print credentials.
- [ ] Run `git rev-parse HEAD`, `git status --short`, `rg -n "include_router|APIRouter|predict|hotspot|forecast|model" api services tests`, and inspect `requirements-render.txt`, `api/main.py`, and the migration configuration.
- [ ] Compare each production path with `royal_shield_backend_unified/api`, `services/data_ingestion`, `services/ml`, and `scripts/train_forecast_model.py` using `git diff --no-index` or a file matrix.
- [ ] Write the matrix with columns: capability, unified source file, production destination file, production status, simulated behavior, migration action, test command.
- [ ] Append the commit IDs, no-secret command results, and blockers to the worklog.
- [ ] Commit only the matrix and worklog update: `git add docs/forecast-engine-v2-production-backend-matrix.md ROYAL_SHIELD_SIMULATION_WORKLOG_ENTRY_006_FORECAST_GIT_DESIGN.md && git commit -m "docs: map forecast production backend"`.

## Task 2: Define the typed availability contract

**Files:**
- Create: `D:\APPS\ROYAL SHIELD\server-beckend\api\schemas\forecast.py`
- Modify: `D:\APPS\ROYAL SHIELD\server-beckend\api\routes\predictions.py`
- Test: `D:\APPS\ROYAL SHIELD\server-beckend\tests\api\test_forecast_contract.py`

**Interfaces:**
- Produces `ForecastStatus` enum: `READY`, `DEGRADED`, `UNAVAILABLE`.
- Produces `ForecastMetadata(status, generated_at, data_freshness, sources, model_version, confidence, degradation_reasons)`.
- Produces `unavailable_forecast(detail: str) -> HTTPException` with HTTP 503.

- [ ] Write failing tests asserting `READY` validates metadata, `DEGRADED` requires at least one reason, invalid input returns 422, and unavailable prediction returns 503 without a score.
- [ ] Run `pytest tests/api/test_forecast_contract.py -v`; expected initial failure is missing `api.schemas.forecast`.
- [ ] Implement immutable Pydantic models and error factory. Do not expose filesystem paths, provider errors, or secrets in `detail`.
- [ ] Replace broad `except Exception` response bodies in prediction routes with sanitized errors and the new contract.
- [ ] Re-run the test file; expected PASS.
- [ ] Commit: `git add api/schemas/forecast.py api/routes/predictions.py tests/api/test_forecast_contract.py && git commit -m "feat: add honest forecast availability contract"`.

## Task 3: Add durable, versioned forecast storage

**Files:**
- Create: one migration under `D:\APPS\ROYAL SHIELD\server-beckend\migrations\` using the exact naming convention discovered in Task 1.
- Create: `D:\APPS\ROYAL SHIELD\server-beckend\services\data_ingestion\storage.py`
- Test: `D:\APPS\ROYAL SHIELD\server-beckend\tests\services\test_forecast_storage.py`

**Interfaces:**
- Produces `upsert_forecast_event(session, event: ForecastEventInput) -> UpsertResult`.
- Produces `upsert_forecast_poi(session, poi: ForecastPoiInput) -> UpsertResult`.
- Produces `upsert_cyber_observation(session, observation: CyberObservationInput) -> UpsertResult`.
- Unique identities are `(source, external_id)`; coordinates must be finite and within latitude `[-90,90]`, longitude `[-180,180]`.

- [ ] Write failing tests for duplicate upsert, invalid coordinates, UTC normalization, missing source identifier, and independent cyber storage.
- [ ] Create migration tables only after Task 1 confirms no equivalent production tables: `forecast_events`, `forecast_pois`, `cyber_threat_observations`, and `forecast_model_runs`; use foreign keys and indexes needed by source/time queries.
- [ ] Implement SQLAlchemy/Pydantic storage types and nested transaction behavior modeled on `royal_shield_backend_unified/services/data_ingestion/storage.py`.
- [ ] Run the repository migration command against a disposable PostgreSQL database and `pytest tests/services/test_forecast_storage.py -v`.
- [ ] Commit migration, storage and tests: `git commit -m "feat: persist normalized forecast observations"`.

## Task 4: Port real collectors with source-specific validation

**Files:**
- Create/Modify: `D:\APPS\ROYAL SHIELD\server-beckend\services\data_ingestion\collectors\miami_dade_collector.py`
- Create/Modify: `...\environmental_collector.py`, `...\osm_collector.py`, `...\crime_collector.py`, `...\cyber_collectors.py`
- Create: `D:\APPS\ROYAL SHIELD\server-beckend\tests\services\test_collectors.py`
- Modify: `D:\APPS\ROYAL SHIELD\server-beckend\.env.example`

**Interfaces:** Each collector exposes `collect(since: datetime | None) -> CollectionResult`, where `CollectionResult` contains actual `inserted`, `updated`, `rejected`, `source`, and `collected_at` counts.

- [ ] Write mocked-HTTP tests using recorded non-sensitive provider fixtures for successful normalization, malformed provider record rejection, timeout, rate limit, and missing configuration.
- [ ] Port only the validated adapters from the unified checkout. FBI output must be stored as regional aggregate records without fabricated `latitude`/`longitude`.
- [ ] Require a configured provider key before calling key-protected sources; missing key produces a skipped collector result with a safe reason, not fake rows.
- [ ] Add variable names and descriptions to `.env.example` only; never add values.
- [ ] Run `pytest tests/services/test_collectors.py -v` and a single controlled collector run against a non-production database; query persisted row counts directly.
- [ ] Commit: `git commit -m "feat: ingest validated forecast source data"`.

## Task 5: Train and persist verifiable models

**Files:**
- Create/Modify: `D:\APPS\ROYAL SHIELD\server-beckend\services\ml\training_data.py`
- Create/Modify: `...\feature_engineering.py`, `...\risk_predictor.py`, `...\cyber_forecast.py`
- Create: `...\services\ml\artifact_store.py`
- Create/Modify: `...\scripts\train_forecast_model.py`
- Test: `...\tests\services\test_forecast_training.py`

**Interfaces:**
- `load_spatial_training_data(session) -> tuple[list[Event], list[Poi]]`
- `train_spatial_model(events, pois, lookback_days) -> TrainingResult`
- `ArtifactStore.publish(bytes, metadata) -> ModelArtifact`
- `ModelArtifact` contains version, sha256, storage URI, trained_at, source window and metrics.

- [ ] Write failing tests for chronological split, zero durable records, insufficient volume, metric creation, artifact checksum, and artifact retrieval failure.
- [ ] Port leakage-safe feature engineering from the unified backend; forbid a feature timestamp on or after its prediction cutoff.
- [ ] Add a persistent artifact adapter selected by environment configuration. When no persistent store is configured, training exits nonzero and no `READY` model is published.
- [ ] Persist model-run metadata and metrics to `forecast_model_runs`; do not treat a local `models/*.pkl` file as production state.
- [ ] Run `pytest tests/services/test_forecast_training.py -v` and `python scripts/train_forecast_model.py --min-events 100` on durable non-production data; document actual outcome.
- [ ] Commit: `git commit -m "feat: train and persist forecast models"`.

## Task 6: Replace simulated prediction, explanation and hotspot routes

**Files:**
- Modify: `D:\APPS\ROYAL SHIELD\server-beckend\api\routes\predictions.py`
- Modify: `D:\APPS\ROYAL SHIELD\server-beckend\api\routes\hotspots.py`
- Create: `D:\APPS\ROYAL SHIELD\server-beckend\services\ml\forecast_service.py`
- Test: `D:\APPS\ROYAL SHIELD\server-beckend\tests\api\test_predictions.py`

**Interfaces:** `ForecastService.predict(locations, prediction_date) -> ForecastResponse`; it loads current persisted data and a verified model artifact, then returns the Task 2 contract.

- [ ] Write failing API tests that verify an unavailable model returns 503, a stale source produces `DEGRADED`, a valid artifact/data pair produces `READY`, and no route contains a fixed confidence, fixed forecast date or sample factor list.
- [ ] Implement model loading through `ArtifactStore`; verify version/checksum before inference.
- [ ] Query current events and POIs from PostgreSQL for every requested H3 cell; remove the empty `current_events = []`, empty `current_pois = []`, fixed `0.75`, sample factors, and sample trend arrays present in the unified prototype.
- [ ] Return explanation factors only when generated from the real model. Otherwise return `UNAVAILABLE`, never a template explanation.
- [ ] Run `pytest tests/api/test_predictions.py -v` and `pytest tests/api -v`.
- [ ] Commit: `git commit -m "feat: serve real forecast predictions"`.

## Task 7: Add controlled collection/training operation and health evidence

**Files:**
- Create: `D:\APPS\ROYAL SHIELD\server-beckend\scripts\run_forecast_pipeline.py`
- Modify: `D:\APPS\ROYAL SHIELD\server-beckend\api\routes\system.py`
- Test: `D:\APPS\ROYAL SHIELD\server-beckend\tests\scripts\test_forecast_pipeline.py`

**Interfaces:** `run_forecast_pipeline.py --since ISO-8601 --min-events N` writes a redacted JSON run summary with collection counts, model state and exit code; system status exposes only aggregate non-sensitive health.

- [ ] Write failing tests for a provider failure, insufficient volume, successful durable training, and safe redaction of configuration values.
- [ ] Implement sequential source execution, direct database count verification, train invocation and summary persistence.
- [ ] Make the command return `0` only if the configured pipeline reaches a real, persisted result; return nonzero for missing source configuration, data or artifact persistence.
- [ ] Run the tests and one non-production end-to-end execution; save evidence in the worklog without keys or payloads.
- [ ] Commit: `git commit -m "feat: add verified forecast pipeline operation"`.

## Task 8: Connect Android to the availability contract

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/features/riskprediction/data/remote/RiskApiModels.kt`
- Modify: `app/src/main/java/com/royalshield/app/features/riskprediction/data/remote/RiskPredictionApi.kt`
- Modify: `app/src/main/java/com/royalshield/app/features/riskprediction/data/repository/RiskPredictionRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/features/riskprediction/ui/RiskMapViewModel.kt`
- Modify: `app/src/main/java/com/royalshield/app/features/trackingshield/TrackingShieldViewModel.kt`
- Create: `app/src/test/java/com/royalshield/app/features/riskprediction/RiskPredictionRepositoryTest.kt`
- Create: `app/src/test/java/com/royalshield/app/features/riskprediction/RiskMapViewModelTest.kt`

**Interfaces:** Kotlin `ForecastAvailability` maps backend `READY`, `DEGRADED`, `UNAVAILABLE` to UI state; `UNAVAILABLE` has no risk score field.

- [ ] Write failing tests for JSON parsing, `UNAVAILABLE` screen state without a score, `DEGRADED` warning plus freshness, and a backend 503 mapped to a safe user message.
- [ ] Implement typed DTOs and mapper in the existing repository pattern; preserve existing navigation and protected screen structure.
- [ ] Display source/freshness/status only in the current feature's existing UI surface. Do not fabricate defaults on null response fields.
- [ ] Run the focused unit tests and `./gradlew.bat :app:compileDebugKotlin --console=plain`.
- [ ] Commit: `git commit -m "feat: show verified forecast availability in Android"`.

## Task 9: Deploy safely and collect production proof

**Files:**
- Modify: `D:\APPS\ROYAL SHIELD\server-beckend\RENDER_DEPLOYMENT.md` or the repository's deployment document found in Task 1.
- Modify: `ROYAL_SHIELD_SIMULATION_WORKLOG_ENTRY_006_FORECAST_GIT_DESIGN.md`

- [ ] Run backend tests, static validation, dependency lock validation and migration dry run in the production repository; record exact commands/results.
- [ ] Confirm Render has required non-secret variable names and persistent artifact configuration. Do not reveal variable values or change paid services without action-time confirmation.
- [ ] Push only reviewed commits to the Render-tracked branch, trigger the approved Render deploy, and watch logs for migration/startup errors.
- [ ] Verify public `/health`, Forecast system status, one `UNAVAILABLE` case and one real `READY` or `DEGRADED` case. A health check alone is insufficient.
- [ ] Install the built APK on an emulator/device and document Android → public API behavior with no secrets.
- [ ] Update the worklog with deploy commit, timestamps, endpoint statuses, database/model evidence, outstanding provider gaps and recovery instructions.
- [ ] Commit deployment documentation: `git commit -m "docs: record forecast production verification"`.

## Completion checklist

- [ ] All simulated Forecast responses identified in Task 1 are either replaced by real behavior or return `UNAVAILABLE`.
- [ ] No fixed confidence, sample events, mock explanation factors or fake training success remains in the Forecast path.
- [ ] Production PostgreSQL has validated, source-attributed records.
- [ ] A persistent, checksummed model artifact and metrics exist for any `READY` response.
- [ ] Backend and Android tests pass; public and device E2E evidence is recorded.
- [ ] Every unresolved external provider is reported honestly in the worklog.
