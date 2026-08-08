# Royal Shield Phase A Public Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a Play-ready baseline where public security flows use Render, VirusTotal and Twilio honestly, and release claims have device evidence.

**Architecture:** Android calls only the authenticated Render FastAPI service at `BuildConfig.API_BASE_URL`. Render owns VirusTotal, Twilio, Firebase Admin and Play service credentials; Android receives typed `READY`, `DEGRADED`, or `UNAVAILABLE` results and never treats unavailable services as success.

**Tech Stack:** Kotlin, Jetpack Compose, Firebase Auth/Firestore/Realtime Database, FastAPI, Pydantic, httpx, Twilio, VirusTotal, Google Play Billing, Render.

## Global Constraints

- Keep secrets exclusively in Render environment variables or ignored local configuration; never in APK, source or logs.
- Preserve the existing black-and-gold visual language and do not restructure Registration, Splash, Dashboard or Cyber screens without an approved narrow change.
- Inputs that cannot be processed return HTTP 422; upstream configuration/outage returns HTTP 503 with a sanitized reason.
- A function is publishable only after source test, release build, physical-device path and provider/production evidence all pass.
- Do not enable restricted Android permissions unless their user-visible use case and Play Console declaration are complete.

## Locked file map

- `royal_shield_backend_unified/api/contracts/status.py`: reusable availability contract and HTTP helpers.
- `royal_shield_backend_unified/api/routes/scan.py`: authenticated, rate-limited VirusTotal URL request/result flow.
- `royal_shield_backend_unified/api/routes/sos.py`: authenticated SOS dispatch that cannot report success when Twilio sends zero messages.
- `app/src/main/java/com/royalshield/app/data/RenderSecurityApi.kt`: Android Retrofit API contracts for scan and SOS.
- `app/src/main/java/com/royalshield/app/data/RenderSecurityRepository.kt`: token-bearing Android client with typed outcome mapping.
- `app/src/main/java/com/royalshield/app/ui/screens/UrlScanScreen.kt`: UI that renders real scan status without client-held VirusTotal keys.
- `app/src/main/java/com/royalshield/app/data/SosRepository.kt`: SOS client/repository; no anonymous user identity or fake confirmation.
- `app/src/main/java/com/royalshield/app/auth/AccountDeletionRepository.kt`: authenticated Firebase account/data deletion coordinator.
- `app/src/main/java/com/royalshield/app/ui/screens/SettingsScreen.kt`: user-confirmed account deletion entry point and permission disclosures.
- `app/src/main/AndroidManifest.xml`: least-privilege release manifest.
- `render.yaml` and `royal_shield_backend_unified/.env.example`: complete secret names, no values.

---

### Task 1: Reconcile the Render deployment before functional changes

**Files:**
- Modify: `render.yaml`
- Modify: `royal_shield_backend_unified/.env.example`
- Create: `docs/release/render-production-evidence.md`

**Produces:** one confirmed service/repository/branch/commit tuple and an inventory of required secret names.

- [ ] **Step 1: Capture the current deployment baseline**

Run `git rev-parse HEAD`, inspect the Render service settings, then record the public URL, deployed commit, health response, configured `API_BASE_URL`, and presence-only status for `FIREBASE_SERVICE_ACCOUNT_JSON`, `VIRUSTOTAL_API_KEY`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER`, and `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.

- [ ] **Step 2: Write the failing reconciliation check**

Create `royal_shield_backend_unified/tests/test_release_config.py`:

```python
from pathlib import Path

def test_render_uses_unified_fastapi_entrypoint():
    text = Path('../render.yaml').read_text(encoding='utf-8')
    assert 'cd royal_shield_backend_unified && uvicorn api.main:app' in text
    assert 'healthCheckPath: /health' in text
```

- [ ] **Step 3: Run the focused check**

Run: `python -m pytest tests/test_release_config.py -q` from `royal_shield_backend_unified`.

- [ ] **Step 4: Make the configuration match the verified service**

Set only non-secret URLs, branch and command in `render.yaml`; list the required variables with empty values in `.env.example`. Do not deploy until the rendered evidence document contains the tuple captured in Step 1.

- [ ] **Step 5: Verify and commit**

Run the focused test and `git diff --check`; commit as `chore: reconcile Render production configuration`.

### Task 2: Add the shared availability contract and test harness

**Files:**
- Create: `royal_shield_backend_unified/api/contracts/status.py`
- Create: `royal_shield_backend_unified/tests/test_status_contract.py`

**Produces:** `AvailabilityResponse` and `unavailable()` used by every provider route.

- [ ] **Step 1: Write failing contract tests**

```python
from api.contracts.status import AvailabilityResponse, unavailable

def test_unavailable_has_no_fabricated_data():
    response = unavailable('VIRUSTOTAL_NOT_CONFIGURED')
    assert response.status == 'UNAVAILABLE'
    assert response.reason == 'VIRUSTOTAL_NOT_CONFIGURED'
    assert response.confidence is None
```

- [ ] **Step 2: Run the test**

Run: `python -m pytest tests/test_status_contract.py -q`; expected failure: module missing.

- [ ] **Step 3: Implement the minimal contract**

```python
class AvailabilityResponse(BaseModel):
    status: Literal['READY', 'DEGRADED', 'UNAVAILABLE']
    generatedAt: datetime
    dataFreshness: datetime | None = None
    sources: list[str] = []
    confidence: float | None = None
    reason: str | None = None

def unavailable(reason: str, source: str | None = None) -> AvailabilityResponse:
    return AvailabilityResponse(status='UNAVAILABLE', generatedAt=datetime.now(timezone.utc), sources=[] if source is None else [source], reason=reason)
```

- [ ] **Step 4: Verify and commit**

Run the focused test and `python -m compileall api`; commit as `feat: add provider availability contract`.

### Task 3: Make VirusTotal scanning server-only and honest

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/scan.py`
- Create: `royal_shield_backend_unified/tests/routes/test_scan.py`
- Create: `app/src/main/java/com/royalshield/app/data/RenderSecurityApi.kt`
- Create: `app/src/main/java/com/royalshield/app/data/RenderSecurityRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/VirusTotalRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/UrlScanScreen.kt`

**Consumes:** `AvailabilityResponse` from Task 2 and Firebase ID token.

**Produces:** `suspend fun checkUrl(url: String): SecurityOutcome` where `SecurityOutcome` is `Ready`, `Pending`, `Unavailable`, or `Invalid`.

- [ ] **Step 1: Write failing backend tests**

```python
def test_url_scan_without_provider_returns_503(client, monkeypatch):
    monkeypatch.delenv('VIRUSTOTAL_API_KEY', raising=False)
    response = client.post('/api/v1/scan/url', json={'url': 'https://example.com'})
    assert response.status_code == 503
    assert response.json()['detail']['status'] == 'UNAVAILABLE'

def test_url_scan_rejects_non_https(client):
    response = client.post('/api/v1/scan/url', json={'url': 'ftp://example.com'})
    assert response.status_code == 422
```

- [ ] **Step 2: Implement provider mapping**

Require Firebase authentication, restrict input to `HttpUrl` with schemes `http` and `https`, map upstream 401/403 to `UNAVAILABLE`, upstream 429 to `DEGRADED`, and preserve provider `queued`, `in-progress`, and `completed` states without inventing a score.

- [ ] **Step 3: Replace the Android direct-provider path**

Delete `PreferencesManager.getVirusTotalApiKey()` use and direct `https://www.virustotal.com/api/v3` requests. Use `Authorization: Bearer <Firebase ID token>` to call Render, poll only while the backend returns `Pending`, and display `Unavailable` as a retryable non-risk result.

- [ ] **Step 4: Verify and commit**

Run focused backend tests, Android unit tests for `RenderSecurityRepository`, `./gradlew :app:testDebugUnitTest`, and `./gradlew :app:assembleDebug`; commit as `feat: route VirusTotal scans through Render`.

### Task 4: Make SOS delivery truthful and consent-based

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/sos.py`
- Create: `royal_shield_backend_unified/tests/routes/test_sos.py`
- Modify: `app/src/main/java/com/royalshield/app/data/SosRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/TrackingShieldScreen.kt`

**Produces:** `SosDispatchOutcome(sentCount: Int, failedCount: Int, status: AvailabilityStatus)`.

- [ ] **Step 1: Write failing server tests**

```python
def test_missing_twilio_never_reports_sos_sent(client, monkeypatch):
    monkeypatch.delenv('TWILIO_ACCOUNT_SID', raising=False)
    response = client.post('/api/v1/sos/alert', json=VALID_ALERT)
    assert response.status_code == 503
    assert response.json()['detail']['reason'] == 'TWILIO_NOT_CONFIGURED'

def test_empty_contacts_is_invalid(client):
    response = client.post('/api/v1/sos/alert', json={**VALID_ALERT, 'contacts': []})
    assert response.status_code == 422
```

- [ ] **Step 2: Implement strict SOS semantics**

Authenticate the caller, validate E.164 recipients, latitude `[-90,90]`, longitude `[-180,180]`, consent timestamp and a maximum of five contacts. Return `READY` only when `smsSent > 0`; return 503 `UNAVAILABLE` when Twilio is absent and `DEGRADED` when only some recipients fail. Log incident ID and counts only.

- [ ] **Step 3: Connect Android confirmation to actual response**

Replace anonymous identity and Firestore-only success with the Render response. Request foreground location at send time, require a visible confirmation of recipients and consent, and show “not sent” for `UNAVAILABLE`.

- [ ] **Step 4: Verify and commit**

Run route tests, Compose/ViewModel test, `./gradlew :app:assembleDebug`, and one authorized Twilio test recipient delivery; commit as `feat: make SOS dispatch verifiable`.

### Task 5: Deliver account deletion and least-privilege release permissions

**Files:**
- Create: `app/src/main/java/com/royalshield/app/auth/AccountDeletionRepository.kt`
- Create: `app/src/test/java/com/royalshield/app/auth/AccountDeletionRepositoryTest.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Produces:** `suspend fun deleteCurrentAccount(): AccountDeletionResult` and a user-confirmed Settings action.

- [ ] **Step 1: Write failing deletion tests**

```kotlin
@Test fun `requires recent login before deleting account`() = runTest {
    assertEquals(AccountDeletionResult.ReauthenticationRequired, repository.deleteCurrentAccount())
}
```

- [ ] **Step 2: Implement deletion order**

Require an authenticated user and recent login, delete owned Realtime Database profile branches and Firestore documents using Firebase-supported calls, then call `FirebaseAuth.currentUser?.delete()`. Return a typed recoverable result; never show deletion success if any mandatory deletion step fails.

- [ ] **Step 3: Reduce manifest permissions**

Remove `READ_SMS`, `RECEIVE_SMS`, `QUERY_ALL_PACKAGES`, `ACCESS_BACKGROUND_LOCATION`, `SYSTEM_ALERT_WINDOW`, camera and microphone declarations unless the associated Phase-A verified feature is enabled with its own approved policy dossier. Keep only network, foreground location while SOS is visible, notifications, billing and VPN declarations needed for the public build.

- [ ] **Step 4: Verify and commit**

Run deletion tests, `./gradlew :app:lintDebug`, inspect merged manifest, and commit as `feat: add account deletion and minimize release permissions`.

### Task 6: Validate Billing without client-only entitlement

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/billing.py`
- Create: `royal_shield_backend_unified/tests/routes/test_billing.py`
- Modify: `app/src/main/java/com/royalshield/app/billing/BillingRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/subscription/EntitlementStore.kt`

**Produces:** entitlement is granted only from a Play purchase verified by Render.

- [ ] **Step 1: Write failing tests**

```python
def test_unconfigured_play_verifier_does_not_grant_access(client):
    response = client.post('/api/billing/verify', json=VALID_PURCHASE)
    assert response.status_code == 503
```

- [ ] **Step 2: Implement verification rule**

Accept Firebase UID, `productId`, `purchaseToken` and `packageName`; verify them using the configured Play service account; persist only the verified tier/expiry/acknowledgement fields; reject a cancelled, expired or mismatched purchase.

- [ ] **Step 3: Make Android await server confirmation**

After `onPurchasesUpdated`, submit the purchase token to Render and acknowledge only a confirmed eligible purchase. `restorePurchases()` must use the same verification path.

- [ ] **Step 4: Verify and commit**

Run backend tests, Billing unit tests, then use an internal Play track and licensed tester to test purchase, cancellation and restore; commit as `feat: verify Play entitlements server side`.

### Task 7: Release evidence and Play submission gate

**Files:**
- Create: `docs/release/phase-a-evidence.md`
- Create: `docs/release/play-console-checklist.md`
- Modify: `README.md`

- [ ] **Step 1: Update claims**

Replace descriptions that call a simulated action functional. Each public feature must point to its `READY`, `DEGRADED` and `UNAVAILABLE` behavior.

- [ ] **Step 2: Build release**

Run `./gradlew :app:clean :app:bundleRelease`, `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`, `./gradlew :app:testDebugUnitTest :app:lintDebug`, and record exact command, commit and outcome.

- [ ] **Step 3: Run physical-device acceptance**

Using a test account and consented test contact, verify install, authentication, URL scan success/unavailable, SOS success/unavailable, account deletion, purchase/restore and post-action logs with sensitive values redacted.

- [ ] **Step 4: Complete Play Console gate**

Upload the AAB to internal testing first, complete Data Safety from observed collection, attach public privacy URL, set content rating, app access/testing instructions, support contact, screenshots and permission declarations. Do not promote beyond internal testing until every evidence row is green.

- [ ] **Step 5: Commit evidence only after redaction**

Run `git diff --check`; confirm documents contain no token, phone number, account ID or provider response body; commit as `docs: record phase A release evidence`.

## Plan self-review

- Spec coverage: Phase A is covered by Tasks 1-7. VPN beta and advanced intelligence are intentionally separate deliverables, not omitted scope.
- Placeholder scan: no deferred implementation markers are used; all externally controlled actions have explicit readiness gates.
- Type consistency: provider-facing routes use `AvailabilityResponse`; Android maps them through `SecurityOutcome`; SOS and billing expose typed results instead of booleans.
