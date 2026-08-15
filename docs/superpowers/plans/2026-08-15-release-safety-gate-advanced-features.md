# Release Safety Gate for Advanced Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure VPN, Forecast Engine, Threat Map, automations, reports, and policies cannot present simulated, unconfigured, or unverified behavior as working protection.

**Architecture:** FastAPI exposes a shared availability contract and feature-status endpoint derived from actual configuration and evidence, never UI assumptions. Android maps those statuses through a focused registry and renders existing Dashboard/Cyber entry points as enabled, limited, or unavailable without restructuring either screen.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines, Navigation Compose, FastAPI, Pydantic, pytest, JUnit, PowerShell, Markdown.

## Global Constraints

- Preserve current uncommitted Antigravity changes and avoid unrelated edits.
- Do not restructure Registration, Splash, Dashboard, or Cyber.
- Use only `READY`, `DEGRADED`, or `UNAVAILABLE` for provider-backed availability.
- Invalid input returns HTTP 422; unavailable provider or action returns HTTP 503.
- Compilation, `/health`, code presence, delay animations, and local state are not end-to-end evidence.
- Never fabricate confidence, events, reports, files, policies, actions, connections, or success.
- Keep physical/geospatial risk separate from cyber intelligence.
- Do not expose tokens, phone numbers, precise locations, account identifiers, provider bodies, or secret values.
- Do not commit, push, deploy, purchase services, or change secrets without explicit authorization.

---

### Task 1: Establish the daily evidence report

**Files:**
- Create: `docs/daily-reports/TEMPLATE.md`
- Create: `docs/daily-reports/2026-08-15-royal-shield-daily-report.md`
- Create: `scripts/validate-daily-report.ps1`

**Interfaces:**
- Produces: `scripts/validate-daily-report.ps1 -Path <report>` with exit code 0 only when the report has all required headings, timeline timestamps, and no obvious sensitive patterns.

- [ ] **Step 1: Create the report template**

```markdown
# Royal Shield Daily Report — YYYY-MM-DD
## Objetivo del día
## Estado inicial
## Línea de tiempo
## Archivos modificados
## Validaciones realizadas
## Estado de funciones
## Riesgos o bloqueadores
## Próximo paso recomendado
```

- [ ] **Step 2: Write a failing validator fixture and validator checks**

The validator must require every heading and at least one timeline heading matching `### HH:mm:ss EDT|EST`. It must reject lines matching obvious Bearer tokens, JWT shapes, API-key assignments, E.164 phone numbers, and coordinate pairs while printing only the line number and rule name.

- [ ] **Step 3: Run the validator against the incomplete fixture and confirm nonzero exit**

Run: `powershell -ExecutionPolicy Bypass -File scripts/validate-daily-report.ps1 -Path <temporary-fixture>`.

- [ ] **Step 4: Create today's report from real evidence**

Record branch/commit, preserved Antigravity changes, approved decisions, Android baseline (`testDebugUnitTest` and `assembleDebug` passed), Forecast data research, and current classifications. Do not paste raw provider responses or secret values.

- [ ] **Step 5: Run validator and `git diff --check`**

- [ ] **Step 6: Update this report after every following task; do not commit**

### Task 2: Add the shared backend availability contract

**Files:**
- Create: `royal_shield_backend_unified/api/contracts/__init__.py`
- Create: `royal_shield_backend_unified/api/contracts/status.py`
- Create: `royal_shield_backend_unified/tests/test_status_contract.py`

**Interfaces:**
- Produces: `AvailabilityStatus`, `AvailabilityResponse`, `ready()`, `degraded()`, and `unavailable()`.

- [ ] **Step 1: Write failing contract tests**

```python
from api.contracts.status import AvailabilityResponse, unavailable

def test_unavailable_has_no_fabricated_data():
    result = unavailable("NO_E2E_EVIDENCE")
    assert isinstance(result, AvailabilityResponse)
    assert result.status == "UNAVAILABLE"
    assert result.data is None
    assert result.confidence is None
    assert result.sources == []
```

- [ ] **Step 2: Run `python -m pytest tests/test_status_contract.py -q` and confirm the module is missing**

- [ ] **Step 3: Implement the exact contract**

```python
class AvailabilityResponse(BaseModel):
    status: Literal["READY", "DEGRADED", "UNAVAILABLE"]
    generatedAt: datetime
    dataFreshness: datetime | None = None
    sources: list[str] = Field(default_factory=list)
    modelVersion: str | None = None
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    reason: str | None = None
    data: dict | None = None
```

- [ ] **Step 4: Make helpers create fresh UTC timestamps and never mutable default lists**

- [ ] **Step 5: Run focused tests and `python -m compileall api`**

- [ ] **Step 6: Update daily report; do not commit**

### Task 3: Expose a truthful advanced-feature status endpoint

**Files:**
- Create: `royal_shield_backend_unified/api/routes/feature_status.py`
- Modify: `royal_shield_backend_unified/api/main.py`
- Create: `royal_shield_backend_unified/tests/routes/test_feature_status.py`

**Interfaces:**
- Produces: `GET /api/v1/features/advanced-status` with keys `vpn`, `forecast`, `threatMap`, `automations`, `reports`, and `policies`, each containing an `AvailabilityResponse`.

- [ ] **Step 1: Write failing endpoint tests**

```python
def test_unverified_features_default_to_unavailable(client, monkeypatch):
    for key in ("VPN_E2E_VERIFIED", "FORECAST_E2E_VERIFIED", "THREAT_MAP_E2E_VERIFIED"):
        monkeypatch.delenv(key, raising=False)
    response = client.get("/api/v1/features/advanced-status")
    assert response.status_code == 200
    body = response.json()
    assert body["vpn"]["status"] == "UNAVAILABLE"
    assert body["forecast"]["status"] == "UNAVAILABLE"
    assert body["threatMap"]["status"] == "UNAVAILABLE"
```

- [ ] **Step 2: Run the focused tests and confirm route failure**

- [ ] **Step 3: Implement fail-closed status derivation**

Configuration presence can prove only `NO_CONFIGURADA` versus `NO_VERIFICADA`; it cannot create `READY`. `READY` requires a non-secret evidence record containing verified timestamp, deployed commit, and module-specific proof. If the repository has no canonical durable evidence store, keep the module `UNAVAILABLE` rather than introducing environment booleans that can bypass evidence.

- [ ] **Step 4: Register the router and run focused/full backend tests**

- [ ] **Step 5: Update daily report; do not deploy or commit**

### Task 4: Remove simulated Forecast and threat intelligence responses

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/predictions.py`
- Modify: `royal_shield_backend_unified/api/routes/threats.py`
- Modify as needed: `royal_shield_backend_unified/api/routes/threat_map.py`
- Create: `royal_shield_backend_unified/tests/routes/test_advanced_unavailable.py`

**Interfaces:**
- Consumes: `AvailabilityResponse`.
- Produces: HTTP 503 `UNAVAILABLE` for routes lacking real model/data evidence.

- [ ] **Step 1: Write tests that reject current simulation**

```python
def test_prediction_without_loaded_verified_artifact_returns_503(client):
    response = client.post("/api/v1/predictions/risk", json=VALID_REQUEST)
    assert response.status_code == 503
    assert response.json()["detail"]["status"] == "UNAVAILABLE"
    assert response.json()["detail"]["confidence"] is None

def test_threat_feed_never_returns_mock_events(client):
    response = client.get("/api/v1/threats")
    assert response.status_code in (200, 503)
    assert "Mock threat" not in response.text
```

- [ ] **Step 2: Run focused tests and capture current failures**

- [ ] **Step 3: Remove fixed `0.75` confidence, mock factors, mock threats, and unimplemented forecast outputs**

When verified data/model is absent, return HTTP 503 with a sanitized reason. Preserve real durable routes only when their source and freshness can be populated.

- [ ] **Step 4: Run focused/full backend tests and static scan**

Run: `rg -n -i "mock|sample|fake|random|confidence=0\.75|For now" royal_shield_backend_unified/api/routes` and review every match.

- [ ] **Step 5: Update daily report; do not deploy or commit**

### Task 5: Remove sample reports, fake file fallbacks, and active sample policies

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/ReportsCenterScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/PolicyManagerScreen.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/screens/ReportsTruthfulnessTest.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/screens/PolicyTruthfulnessTest.kt`

**Interfaces:**
- Produces: empty initial collections; report findings explicitly classified as checked, failed, unavailable, or not verified.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun `fresh report store contains no sample reports`() {
    assertTrue(loadSavedReports(emptyContext).isEmpty())
}

@Test fun `fresh policy store contains no active sample policies`() {
    assertTrue(loadSavedPolicies(emptyContext).isEmpty())
}
```

- [ ] **Step 2: Run focused tests and confirm sample defaults fail them**

- [ ] **Step 3: Replace `loadSampleReports()` fallback with `emptyList()` and remove fictitious fallback file paths**

File checks that cannot open a selected URI must become `NO_VERIFICADA`. Remove delays that only simulate work; retain progress only when it advances from real completed checks.

- [ ] **Step 4: Replace `getSamplePolicies()` fallback with empty state or clearly labeled inactive templates**

Persisting a policy must not claim enforcement. Add a visible `UNAVAILABLE` enforcement state until a corresponding mechanism is verified.

- [ ] **Step 5: Run focused tests, `:app:testDebugUnitTest`, and static scan**

- [ ] **Step 6: Update daily report; do not commit**

### Task 6: Add Android availability models and repository

**Files:**
- Create: `app/src/main/java/com/royalshield/app/data/FeatureAvailability.kt`
- Create: `app/src/main/java/com/royalshield/app/data/FeatureAvailabilityRepository.kt`
- Create: `app/src/main/java/com/royalshield/app/ui/viewmodels/FeatureAvailabilityViewModel.kt`
- Create: `app/src/test/java/com/royalshield/app/data/FeatureAvailabilityRepositoryTest.kt`

**Interfaces:**
- Produces: `AvailabilityState.Ready`, `.Degraded`, `.Unavailable`.
- Produces: `AdvancedFeatureAvailability(vpn, forecast, threatMap, automations, reports, policies)`.

- [ ] **Step 1: Write mapping tests for READY, DEGRADED, UNAVAILABLE, malformed response, timeout, and missing auth**

```kotlin
@Test fun `network failure maps every advanced feature unavailable`() = runTest {
    fakeApi.failure = IOException("offline")
    val result = repository.refresh()
    assertTrue(result.vpn is AvailabilityState.Unavailable)
    assertTrue(result.forecast is AvailabilityState.Unavailable)
}
```

- [ ] **Step 2: Run focused tests and confirm missing types**

- [ ] **Step 3: Implement authenticated backend fetch with fail-closed mapping**

Never cache `READY` beyond its evidence freshness window. A malformed/missing module maps to `Unavailable("STATUS_UNVERIFIED")`.

- [ ] **Step 4: Run focused tests and Android unit suite**

- [ ] **Step 5: Update daily report; do not commit**

### Task 7: Gate VPN, Forecast, Threat Map, automations, reports, and policies in existing UI

**Files:**
- Modify narrowly: `app/src/main/java/com/royalshield/app/ui/screens/DashboardScreen.kt`
- Modify narrowly: `app/src/main/java/com/royalshield/app/ui/screens/CyberHomeScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/VpnScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/AutomationScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/ReportsCenterScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/PolicyManagerScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/navigation/Navigation.kt`
- Create: `app/src/main/java/com/royalshield/app/ui/components/UnavailableFeatureCard.kt`

**Interfaces:**
- Consumes: `AdvancedFeatureAvailability`.

- [ ] **Step 1: Add UI/state tests proving unavailable cards cannot execute protected callbacks**

- [ ] **Step 2: Implement one black-and-gold unavailable card with module name, sanitized reason, freshness, and retry**

- [ ] **Step 3: Apply narrow gates**

VPN uses “VPN no disponible temporalmente”; Forecast uses “Modelo en preparación — predicciones no disponibles”; Threat Map shows an empty state; automation actions without proof show “Próximamente”; reports/policies expose truthful empty/unverified states.

- [ ] **Step 4: Ensure Dashboard and Cyber layouts remain structurally unchanged**

Only card enabled state, supporting copy, and callback behavior may change.

- [ ] **Step 5: Run `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`**

- [ ] **Step 6: Update daily report; do not commit**

### Task 8: Verify release safety and collect device evidence

**Files:**
- Modify: `docs/daily-reports/2026-08-15-royal-shield-daily-report.md`
- Create: `docs/release/advanced-feature-safety-evidence.md`

**Interfaces:**
- Produces: source/build/device evidence for every gated module.

- [ ] **Step 1: Run repository simulation inventory**

Run targeted `rg` searches for mock, sample, fake, random, hardcoded confidence, fallback success, and artificial delay in the affected paths. Classify every remaining match.

- [ ] **Step 2: Run full affected backend and Android validations**

Run backend pytest/compile, Android unit tests, lint, and debug assembly. Record exact commands and exit status.

- [ ] **Step 3: Install the fresh debug APK on the authorized physical device**

Confirm package process and foreground activity, then navigate to each gated module.

- [ ] **Step 4: Verify unavailable behavior**

Confirm VPN cannot claim connected, Forecast cannot show predictions, Threat Map cannot show mock events, automations cannot claim execution, and reports/policies start truthfully.

- [ ] **Step 5: Record redacted screenshots/log evidence and current commit/tree state**

- [ ] **Step 6: Run daily-report validator and `git diff --check`; do not commit, push, or deploy**

## Execution order and stop conditions

- Execute Tasks 1-8 sequentially because they share contracts and UI state.
- Stop a task after two consecutive failures of the same test and investigate root cause before changing more files.
- Do not change a module to `READY` without its module-specific E2E evidence.
- Do not discard or overwrite Antigravity changes; isolate overlapping edits with small patches and inspect the combined diff.
