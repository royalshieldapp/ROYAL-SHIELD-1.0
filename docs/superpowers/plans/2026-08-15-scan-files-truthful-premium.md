# Truthful Premium File Scanning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver one Starter-or-higher file scanner that reads Android URIs correctly, queries VirusTotal only through authenticated Render, and never represents an error or unknown hash as clean.

**Architecture:** `FileScanScreen` delegates URI streaming and hashing to `FileScannerRepository`, then sends the SHA-256 hash through an authenticated `RenderSecurityApi`. FastAPI maps VirusTotal results to a shared availability contract; Android renders typed results and uses the server-verified `EntitlementStore` tier.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines, ContentResolver, Retrofit/OkHttp, Firebase Auth, FastAPI, Pydantic, httpx, pytest.

## Global Constraints

- Preserve the black-and-gold design and do not restructure Dashboard or Cyber screens.
- Keep `VIRUSTOTAL_API_KEY` exclusively in backend environment variables.
- `FREE` is blocked; only server-verified `STARTER`, `GOLD`, or `ULTIMATE` is allowed.
- HTTP 422 means invalid input; HTTP 503 means unavailable provider or service.
- Unknown hashes, exceptions, timeouts, and 404 responses must never become a clean result.
- Do not commit, push, deploy, or expose secrets without explicit authorization.

---

### Task 1: Shared backend availability contract

**Files:**
- Create: `royal_shield_backend_unified/api/contracts/__init__.py`
- Create: `royal_shield_backend_unified/api/contracts/status.py`
- Create: `royal_shield_backend_unified/tests/test_status_contract.py`

**Interfaces:**
- Produces: `AvailabilityStatus`, `AvailabilityResponse`, and `unavailable(reason, source=None)`.

- [ ] **Step 1: Write the failing contract test**

```python
from api.contracts.status import AvailabilityResponse, unavailable

def test_unavailable_contains_no_fabricated_result():
    result = unavailable("VIRUSTOTAL_NOT_CONFIGURED")
    assert isinstance(result, AvailabilityResponse)
    assert result.status == "UNAVAILABLE"
    assert result.data is None
    assert result.confidence is None
```

- [ ] **Step 2: Run the focused test and confirm `ModuleNotFoundError`**

Run from `royal_shield_backend_unified`: `python -m pytest tests/test_status_contract.py -q`.

- [ ] **Step 3: Implement the typed contract**

```python
class AvailabilityResponse(BaseModel):
    status: Literal["READY", "DEGRADED", "UNAVAILABLE"]
    generatedAt: datetime
    dataFreshness: datetime | None = None
    sources: list[str] = Field(default_factory=list)
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    reason: str | None = None
    data: dict | None = None
```

- [ ] **Step 4: Run the focused test and `python -m compileall api`**

- [ ] **Step 5: Record results in today's daily report; do not commit**

### Task 2: Honest authenticated file-hash route

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/scan.py`
- Create: `royal_shield_backend_unified/tests/routes/test_file_scan.py`
- Reuse: the existing Firebase authentication dependency after locating its canonical module with `rg -n "verify.*firebase|Firebase.*token|Depends" royal_shield_backend_unified/api`.

**Interfaces:**
- Consumes: `AvailabilityResponse`.
- Produces: `POST /api/v1/scan/file-hash` accepting `FileHashRequest(hash: SHA-256)` and returning the availability contract.

- [ ] **Step 1: Add failing route tests**

```python
def test_missing_provider_returns_503(client, monkeypatch):
    monkeypatch.delenv("VIRUSTOTAL_API_KEY", raising=False)
    response = client.post("/api/v1/scan/file-hash", json={"hash": "a" * 64}, headers=AUTH)
    assert response.status_code == 503
    assert response.json()["detail"]["status"] == "UNAVAILABLE"

def test_unknown_hash_is_not_clean(client, fake_vt_404):
    response = client.post("/api/v1/scan/file-hash", json={"hash": "b" * 64}, headers=AUTH)
    assert response.status_code == 200
    assert response.json()["status"] == "UNAVAILABLE"
    assert response.json()["reason"] == "HASH_NOT_ANALYZED"

def test_invalid_hash_returns_422(client):
    assert client.post("/api/v1/scan/file-hash", json={"hash": "bad"}, headers=AUTH).status_code == 422
```

- [ ] **Step 2: Run the route tests and confirm the current schema/status assertions fail**

- [ ] **Step 3: Require Firebase authentication and map upstream statuses**

Map 401/403 to HTTP 503 `VIRUSTOTAL_AUTH_FAILED`, 404 to HTTP 200 `UNAVAILABLE/HASH_NOT_ANALYZED`, 429 to `DEGRADED/RATE_LIMITED`, and other upstream/network failures to HTTP 503. Include only engine counts, freshness, meaningful name, and provider permalink in `data`; never return the provider body.

- [ ] **Step 4: Run route tests and the complete backend test suite**

Run: `python -m pytest tests/routes/test_file_scan.py -q` then `python -m pytest -q`.

- [ ] **Step 5: Record evidence; do not deploy or commit**

### Task 3: Android streaming, limits, and typed outcomes

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/data/FileScannerRepository.kt`
- Create: `app/src/main/java/com/royalshield/app/data/FileScanOutcome.kt`
- Create: `app/src/test/java/com/royalshield/app/data/FileScannerRepositoryTest.kt`

**Interfaces:**
- Produces: `suspend fun calculateSha256(input: InputStream, declaredSize: Long?): LocalFileHashResult`.
- Produces: `FileScanOutcome.Ready`, `.Degraded`, `.Unavailable`, `.Invalid`.

- [ ] **Step 1: Write failing hash and size-limit tests**

```kotlin
@Test fun `hashes stream without filesystem path`() = runTest {
    val result = repository.calculateSha256("abc".byteInputStream(), 3)
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result.sha256)
}

@Test fun `rejects empty stream`() = runTest {
    assertTrue(repository.calculateSha256(byteArrayOf().inputStream(), 0) is LocalFileHashResult.Invalid)
}
```

- [ ] **Step 2: Run `:app:testDebugUnitTest` and confirm the new types are missing**

- [ ] **Step 3: Implement streaming with `use`, a 32 MiB limit, and no file path conversion**

Stop reading once the limit is exceeded and return `Invalid(FILE_TOO_LARGE)`; return `Invalid(EMPTY_FILE)` when zero bytes were read.

- [ ] **Step 4: Run focused tests and `:app:compileDebugKotlin`**

- [ ] **Step 5: Record results; do not commit**

### Task 4: Authenticated Render client and ViewModel

**Files:**
- Create: `app/src/main/java/com/royalshield/app/data/RenderSecurityApi.kt`
- Modify: `app/src/main/java/com/royalshield/app/data/FileScannerRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/viewmodels/FileScanViewModel.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/viewmodels/FileScanViewModelTest.kt`

**Interfaces:**
- Produces: `suspend fun scanFile(context: Context, uri: Uri): FileScanOutcome`.

- [ ] **Step 1: Write tests proving missing token, 404, 503, and exception never create a clean result**

```kotlin
@Test fun `unknown hash is unavailable not clean`() = runTest {
    fakeApi.response = BackendFileHashResponse(status = "UNAVAILABLE", reason = "HASH_NOT_ANALYZED")
    assertTrue(repository.scanHash("b".repeat(64)) is FileScanOutcome.Unavailable)
}
```

- [ ] **Step 2: Run focused tests and confirm failure against the direct VirusTotal repository**

- [ ] **Step 3: Implement Firebase ID-token retrieval and `Authorization: Bearer` request to `BuildConfig.API_BASE_URL`**

Remove artificial delay and mock-threat fallback. Map the backend status exactly to `FileScanOutcome`.

- [ ] **Step 4: Run focused tests and `:app:testDebugUnitTest`**

- [ ] **Step 5: Record evidence; do not commit**

### Task 5: One UI, one entitlement gate, one navigation path

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/FileScanScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/navigation/Navigation.kt`
- Modify: `app/src/main/java/com/royalshield/app/SecurityActivity.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/CyberHomeScreen.kt`
- Remove only after reference scan: `app/src/main/java/com/royalshield/app/FileScanActivity.kt`
- Modify only if the activity is removed: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `EntitlementStore.tier`, `Capabilities.canUseFileScanner(tier)`, and `FileScanOutcome`.

- [ ] **Step 1: Add tests that FREE is blocked and STARTER+ is allowed only from verified entitlement state**

- [ ] **Step 2: Replace `BillingManager.hasPremiumAccess` and `PreferencesManager.isPremiumUser()` checks with the canonical tier capability**

- [ ] **Step 3: Route Dashboard, Cyber, Security, and Solution Engine to `file_scan`; remove the direct legacy activity path**

- [ ] **Step 4: Render honest copy**

Use “Known threats detected”, “No known detections in the available report”, “Hash has not been analyzed”, and “Scanner unavailable”. Do not use “Safe” or “No threat detected” for unknown/unavailable results.

- [ ] **Step 5: Run `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`**

- [ ] **Step 6: Validate on a physical device with one non-sensitive known hash and one unknown hash; record redacted evidence**
