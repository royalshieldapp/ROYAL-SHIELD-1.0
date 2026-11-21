# Security Orb Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the supplied golden orb as a locked registration agent, a post-authentication three-step security setup guide, and the Dashboard voice-navigation control while keeping SMS and VirusTotal handling truthful and privacy-safe.

**Architecture:** A small onboarding domain model and encrypted per-UID store drive a dedicated Compose setup screen inserted into `AppMainFlow` after authentication. Existing VPN, QR, chatbot, and Dashboard navigation remain the execution surfaces. SMS URLs are analyzed locally first and optionally submitted to the authenticated backend VirusTotal route; Android never owns the provider key.

**Tech Stack:** Kotlin, Jetpack Compose, Firebase Auth, EncryptedSharedPreferences, Android runtime permissions, SpeechRecognizer, OkHttp, FastAPI, Pydantic, pytest, JUnit.

## Global Constraints

- Do not restructure Registration, Splash, Dashboard, or Cyber Screen.
- Use the supplied transparent golden orb and preserve the black-and-gold identity.
- The three setup choices are VPN, automatic SMS phishing protection, and secure QR scanning.
- Declining a feature never blocks access to the rest of the app.
- Never log SMS bodies, senders, extracted URLs, voice transcripts, provider responses, tokens, or secrets.
- Only an extracted URL may be submitted for advanced VirusTotal analysis; never submit the complete SMS.
- A preference is not proof of activation. Unconfigured or unverified services remain disabled or `UNAVAILABLE`.
- Do not commit, push, deploy, or modify production secrets.

---

### Task 1: Secure the existing backend URL scanner

**Files:**
- Modify: `royal_shield_backend_unified/api/routes/scan.py`
- Create: `royal_shield_backend_unified/tests/routes/test_scan_security.py`

**Interfaces:**
- Consumes: `require_firebase_uid` from `api.security.firebase_auth`.
- Produces: authenticated `POST /api/v1/scan/url` and `GET /api/v1/scan/result/{analysis_id}`; missing provider configuration returns HTTP 503 without secrets.

- [ ] Write focused tests that override `require_firebase_uid`, verify unauthenticated rejection, verify URL validation, and verify missing `VIRUSTOTAL_API_KEY` returns HTTP 503.
- [ ] Run `python -m pytest tests/routes/test_scan_security.py -q` from `royal_shield_backend_unified` and confirm the authentication test fails first.
- [ ] Add `firebase_uid: str = Depends(require_firebase_uid)` to URL submission and result retrieval. Do not log the URL or provider response body.
- [ ] Replace generic provider failures with sanitized 502/503 responses and retain Pydantic `HttpUrl` validation for HTTP 422.
- [ ] Run the focused test, `python -m pytest tests/routes/test_scan_security.py tests/test_status_contract.py -q`, and `python -m compileall api`.

### Task 2: Add the per-user onboarding model and encrypted store

**Files:**
- Create: `app/src/main/java/com/royalshield/app/securitysetup/SecuritySetup.kt`
- Create: `app/src/main/java/com/royalshield/app/securitysetup/SecuritySetupStore.kt`
- Create: `app/src/test/java/com/royalshield/app/securitysetup/SecuritySetupStoreTest.kt`

**Interfaces:**
- Produces: `enum class SecurityFeature { VPN, SMS_PHISHING, QR_SCANNER }`.
- Produces: `data class SecuritySetupPreferences(val uid: String, val completed: Boolean, val vpnEnabled: Boolean, val smsPhishingEnabled: Boolean, val qrScannerEnabled: Boolean)`.
- Produces: `SecuritySetupStore.load(uid)`, `saveChoice(uid, feature, enabled)`, `markCompleted(uid)`, and `needsSetup(uid)`.

- [ ] Write tests proving the exact three-feature order, UID isolation, incomplete-by-default behavior, persisted decline behavior, and completion only after all three choices exist.
- [ ] Run `gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.securitysetup.*"` and confirm missing types fail first.
- [ ] Implement a storage interface with a map-backed test implementation and a production EncryptedSharedPreferences implementation. Prefix every key with a stable SHA-256 digest of the Firebase UID; never persist the raw UID as a preference key.
- [ ] Make `needsSetup(uid)` return true when any choice is absent even if an old completion flag exists.
- [ ] Run the focused tests and `git diff --check`.

### Task 3: Add the orb component and post-authentication setup screen

**Files:**
- Add: `app/src/main/res/drawable-nodpi/royal_security_orb.png`
- Create: `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- Create: `app/src/main/java/com/royalshield/app/ui/screens/SecuritySetupScreen.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/screens/SecuritySetupFlowTest.kt`
- Modify: `app/src/main/java/com/royalshield/app/MainActivity.kt`
- Modify narrowly: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`

**Interfaces:**
- Produces: `RoyalSecurityOrb(state: RoyalSecurityOrbState, modifier: Modifier, onClick: () -> Unit)`.
- Produces: `enum class RoyalSecurityOrbState { LOCKED, SETUP, ACTIVE, LISTENING }`.
- Produces: `SecuritySetupScreen(uid, store, onOpenVpn, onRequestSms, onOpenQr, onComplete)`.
- Adds `AppState.SECURITY_SETUP` between successful authentication/premium continuation and `MAIN` when `needsSetup(uid)` is true.

- [ ] Write state tests proving exactly three questions appear in order, `Ahora no` advances, and completion cannot occur before the third answer.
- [ ] Run focused tests and confirm they fail before implementation.
- [ ] Copy the generated transparent PNG to `drawable-nodpi/royal_security_orb.png`; verify it has an alpha channel and no magenta-dominant background pixels.
- [ ] Replace only Registration's current 260 dp logo content with a compact locked orb and supporting locked message. A tap shows the registration-required message and invokes no protected callback.
- [ ] Add the setup screen with concise benefit/limitation text and explicit `Activar` / `Ahora no` actions. Animate only the orb size when entering setup.
- [ ] Route every successful authentication method through a single post-auth destination decision. Do not treat the existing testing skip or placeholder social buttons as authenticated.
- [ ] Run focused tests and `:app:testDebugUnitTest`.

### Task 4: Implement truthful SMS consent and advanced URL analysis

**Files:**
- Create: `app/src/main/java/com/royalshield/app/data/AdvancedUrlScanRepository.kt`
- Create: `app/src/test/java/com/royalshield/app/data/AdvancedUrlScanRepositoryTest.kt`
- Modify: `app/src/main/java/com/royalshield/app/receivers/SmsReceiver.kt`
- Modify: `app/src/main/java/com/royalshield/app/services/PhishingDetectionService.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/SecuritySetupScreen.kt`
- Create: `app/src/test/java/com/royalshield/app/receivers/SmsPrivacyTest.kt`

**Interfaces:**
- Produces: `AdvancedUrlScanRepository.submit(url): AdvancedScanResult` using a Firebase ID token and `${BuildConfig.API_BASE_URL}/api/v1/scan/url`.
- Produces: `sealed interface AdvancedScanResult { Submitted, Unavailable, Rejected }` with sanitized reasons.
- SMS consent immediately precedes the Android `RECEIVE_SMS` / `READ_SMS` permission request.

- [ ] Write tests proving the backend URL route receives only `{ "url": ... }`, uses `Authorization: Bearer`, maps 401/503/network failure without fake safety, and never includes message or sender fields.
- [ ] Write a static privacy test that rejects `$body`, `$sender`, and extracted URL interpolation in `SmsReceiver` logging calls.
- [ ] Run focused tests and confirm current logging/direct VirusTotal behavior fails.
- [ ] Make `SmsReceiver` run local heuristics first, submit only extracted URLs through `AdvancedUrlScanRepository` when opt-in is stored, and show alerts only from actual local/provider findings.
- [ ] Remove direct Android VirusTotal-key dependency from the SMS path; keep unrelated legacy consumers unchanged in this task.
- [ ] Implement the prominent disclosure with `Aceptar y continuar` and `Ahora no`, then request Android permissions only after affirmative consent.
- [ ] Run focused tests, Android unit tests, and `rg -n 'Received SMS from|Found .* URLs|Scan result for' app/src/main/java/com/royalshield/app/receivers/SmsReceiver.kt` expecting no matches.

### Task 5: Use the orb for real Dashboard voice navigation

**Files:**
- Create: `app/src/main/java/com/royalshield/app/ui/voice/VoiceCommandRouter.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/voice/VoiceCommandRouterTest.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/components/DidAgentDialog.kt`
- Modify narrowly: `app/src/main/java/com/royalshield/app/ui/screens/DashboardScreen.kt`

**Interfaces:**
- Produces: `enum class VoiceDestination { FILE_SCAN, PHISHING, QR_SCANNER, VPN, SOS, MAP, SETTINGS, AUTOMATION, AI_CHAT, UNKNOWN }`.
- Produces: `VoiceCommandRouter.resolve(command: String): VoiceDestination` with accent-insensitive Spanish/English matching.
- Dashboard receives or launches the existing QR scanner destination without changing its overall layout.

- [ ] Write routing tests for Spanish and English commands covering VPN, phishing, QR, SOS, chatbot, unknown input, and terms with accents.
- [ ] Run focused tests and confirm missing router failure.
- [ ] Extract the inline keyword mapping from Dashboard to the pure router and connect every destination to an existing callback or explicit activity launch.
- [ ] Replace only the 44 dp microphone artwork with `RoyalSecurityOrb(ACTIVE)`; retain its click target and the adjacent text-chat button.
- [ ] Keep real `SpeechRecognizer` behavior in `DidAgentDialog`, display the orb in active/listening states, remove any sample-command fallback, and never log or persist transcripts.
- [ ] Run focused tests and `:app:testDebugUnitTest`.

### Task 6: Full verification and physical-device evidence

**Files:**
- Update: `docs/daily-reports/2026-08-15-royal-shield-daily-report.md`

**Interfaces:**
- Produces: build and device evidence without secrets or personal data.

- [ ] Run backend focused/full affected tests and `python -m compileall api`.
- [ ] Run `gradlew.bat :app:testDebugUnitTest`, `gradlew.bat :app:lintDebug`, and `gradlew.bat :app:assembleDebug`.
- [ ] Verify `git diff --check` and inspect `git status --short` without discarding unrelated changes.
- [ ] Install the fresh APK with `adb -s RFCXC0DGQ4Z install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Confirm `versionName=1.3`, package process, and foreground `com.royalshield.app/.MainActivity`.
- [ ] Manually verify locked registration orb, post-auth three-step flow, consent decline behavior, and Dashboard voice navigation. Do not claim unobserved flows as verified.
- [ ] Record results in the daily report; do not commit, push, or deploy.
