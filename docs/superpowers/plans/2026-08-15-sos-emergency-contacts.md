# SOS Emergency Contacts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide one visible, encrypted emergency-contact list used by every SOS entry point and report delivery success only after backend confirmation.

**Architecture:** A focused contacts repository wraps encrypted `PreferencesManager` storage and one-time migration. Tracking/SOS and Settings navigate to the same Compose screen; `SosRepository` sends confirmed recipients to authenticated Render and maps delivery counts to typed outcomes.

**Tech Stack:** Kotlin, Jetpack Compose, EncryptedSharedPreferences, Navigation Compose, Firebase Auth, FastAPI, Pydantic, Twilio, pytest.

## Global Constraints

- Do not request `READ_CONTACTS`.
- Validate E.164 numbers and limit the list to five recipients.
- Never log phone numbers, precise locations, message bodies, tokens, or provider errors.
- Zero confirmed deliveries is `UNAVAILABLE`, never success.
- Do not commit, push, deploy, or send a real SMS without explicit authorization.

---

### Task 1: Canonical encrypted contact repository and migration

**Files:**
- Create: `app/src/main/java/com/royalshield/app/data/EmergencyContactsRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/managers/PreferencesManager.kt`
- Modify: `app/src/main/java/com/royalshield/app/LocationActivity.kt`
- Create: `app/src/test/java/com/royalshield/app/data/EmergencyContactsRepositoryTest.kt`

**Interfaces:**
- Produces: `list()`, `add(name, phone)`, `delete(id)`, and `migrateLegacyContactsOnce()`.

- [ ] **Step 1: Write tests for E.164, duplicates, maximum five, deletion, and idempotent migration**

```kotlin
@Test fun `rejects non E164 phone`() {
    assertEquals(AddContactResult.InvalidPhone, repository.add("Home", "5551234"))
}

@Test fun `migration does not duplicate contacts`() {
    repository.migrateLegacyContactsOnce()
    repository.migrateLegacyContactsOnce()
    assertEquals(1, repository.list().size)
}
```

- [ ] **Step 2: Run focused tests and confirm failure**

- [ ] **Step 3: Implement validation `^\\+[1-9]\\d{7,14}$`, normalization, encrypted persistence, and a migration-complete flag**

- [ ] **Step 4: Make `LocationActivity` consume the repository instead of `live_location_prefs/contacts_list`**

- [ ] **Step 5: Run focused tests and Android unit suite; record results**

### Task 2: Shared emergency-contact screen and navigation

**Files:**
- Create: `app/src/main/java/com/royalshield/app/ui/screens/EmergencyContactsScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/navigation/Navigation.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/SettingsScreen.kt`
- Modify: the actual Tracking/SOS entry screen identified with `rg -n "triggerSOS|onNavigateToSOS|SosManager" app/src/main/java`.

**Interfaces:**
- Produces route: `emergency_contacts`.
- Consumes: `EmergencyContactsRepository`.

- [ ] **Step 1: Add Compose/repository tests for empty, invalid, full, add, and delete states**

- [ ] **Step 2: Implement the black-and-gold screen with name, E.164 phone, recipient count, add, and delete**

- [ ] **Step 3: Add a permanent “Emergency contacts” row in Settings and Tracking/SOS**

- [ ] **Step 4: Route every access to `emergency_contacts`; remove misleading instructions about a nonexistent person icon**

- [ ] **Step 5: Run unit tests, lint, and `assembleDebug`; record evidence**

### Task 3: Truthful authenticated SOS backend

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/sos.py`
- Create: `royal_shield_backend_unified/tests/routes/test_sos.py`
- Reuse: shared availability contract and canonical Firebase auth dependency.

**Interfaces:**
- Produces response with `status`, `incidentId`, `sentCount`, `failedCount`, and sanitized `reason`.

- [ ] **Step 1: Write failing tests**

```python
def test_missing_twilio_returns_503(client, monkeypatch):
    monkeypatch.delenv("TWILIO_ACCOUNT_SID", raising=False)
    response = client.post("/api/v1/sos/alert", json=VALID_ALERT, headers=AUTH)
    assert response.status_code == 503
    assert response.json()["detail"]["status"] == "UNAVAILABLE"

def test_empty_contacts_returns_422(client):
    response = client.post("/api/v1/sos/alert", json={**VALID_ALERT, "contacts": []}, headers=AUTH)
    assert response.status_code == 422
```

- [ ] **Step 2: Add auth, E.164, coordinate bounds, consent timestamp, and maximum-five validation**

- [ ] **Step 3: Return READY only when all recipients succeed, DEGRADED for partial delivery, and HTTP 503 UNAVAILABLE for zero delivery**

- [ ] **Step 4: Log incident ID and counts only; never include contact or raw exception**

- [ ] **Step 5: Run focused and full backend suites; do not deploy**

### Task 4: Android SOS confirmation and typed outcome

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/data/SosRepository.kt`
- Modify: `app/src/main/java/com/royalshield/app/SosManager.kt`
- Create: `app/src/main/java/com/royalshield/app/data/SosDispatchOutcome.kt`
- Create: `app/src/test/java/com/royalshield/app/data/SosRepositoryTest.kt`

**Interfaces:**
- Produces: `suspend fun dispatch(recipients, location, consentAt): SosDispatchOutcome`.

- [ ] **Step 1: Test missing auth, zero recipients, READY, DEGRADED, UNAVAILABLE, and network failure mappings**

- [ ] **Step 2: Replace anonymous Firestore success and per-recipient `TwilioManager` calls with one authenticated Render request**

- [ ] **Step 3: When contacts are empty, navigate directly to `emergency_contacts` without claiming configuration exists elsewhere**

- [ ] **Step 4: Add visible recipient confirmation and consent before manual SOS; automated SOS must use the previously consented configuration and truthful notification state**

- [ ] **Step 5: Run unit tests, lint, and `assembleDebug`**

- [ ] **Step 6: With explicit authorization, test one consented recipient on a physical device and record redacted delivery evidence**
