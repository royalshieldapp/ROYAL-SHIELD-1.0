# Registration Alignment and English UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved Balanced Premium Registration layout with large aligned icons, centered single-line labels, and English-only visible copy.

**Architecture:** Keep presentation constants in `RegistrationLayout.kt` so JVM tests can validate geometry without Compose instrumentation. Apply those constants in the existing `SocialLoginButton` while preserving authentication callbacks and provider availability behavior. Verify source, focused tests, build, and the authorized Samsung device separately.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle Kotlin DSL, Android Debug Bridge.

## Global Constraints

- Change only Registration presentation and focused tests.
- Preserve authentication behavior, navigation, provider availability, black-and-gold styling, and the current screen structure.
- All visible Registration copy must be English.
- Icons must be large, fully inside the button, and aligned in a fixed leading slot.
- Labels must be centered across the full button and remain on one line on Samsung `RFCXC0DGQ4Z`.
- Do not commit, push, deploy, or publish.

---

### Task 1: Lock the Balanced Premium geometry and English copy contracts

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationLayout.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationLayoutTest.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationEnglishCopyTest.kt`

**Interfaces:**
- Produces: `RegistrationSocialButtonLayout(widthFraction, heightDp, iconSizeDp, leadingSlotWidthDp, horizontalPaddingDp)`.
- Produces: source contract proving prohibited Spanish Registration phrases are absent and approved English phrases are present.

- [ ] **Step 1: Extend failing geometry assertions**

Assert the approved values: width `0.92f`, height `80f`, icon `52f`, leading slot `64f`, and horizontal padding `16f`.

- [ ] **Step 2: Add the failing English-copy source test**

Read `RegistrationScreen.kt`; assert it contains `Your security agent is waiting.`, every `Coming Soon` provider label, `Security Agent Locked`, and `GOT IT`; assert it does not contain `próximamente`, `Regístrate`, `Teléfono`, `Agente bloqueado`, or `ENTENDIDO`.

- [ ] **Step 3: Run RED tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.RegistrationLayoutTest" --tests "com.royalshield.app.ui.screens.RegistrationEnglishCopyTest" --console=plain`

Expected: FAIL because the new geometry fields/copy do not exist yet.

- [ ] **Step 4: Implement the layout constants**

Add `leadingSlotWidthDp` and `horizontalPaddingDp` to `RegistrationSocialButtonLayout`; return the approved Balanced Premium values.

- [ ] **Step 5: Run the focused geometry test**

Expected: `RegistrationLayoutTest` PASS while English copy remains RED until Task 2.

### Task 2: Apply aligned buttons and English-only Registration copy

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`

**Interfaces:**
- Consumes: `registrationSocialButtonLayout()` constants from Task 1.
- Preserves: existing `onClick`, `enabled`, Google launcher, Phone navigation, and unavailable-provider behavior.

- [ ] **Step 1: Replace visible Spanish copy**

Use `Your security agent is waiting.`, `Apple — Coming Soon`, `GitHub — Coming Soon`, `Facebook — Coming Soon`, `Email — Coming Soon`, `Security Agent Locked`, the approved English dialog explanation, and `GOT IT`.

- [ ] **Step 2: Implement a fixed leading icon slot**

Use a full-size `Box`; place the icon in a start-aligned container of `leadingSlotWidthDp`, entirely inside `horizontalPaddingDp`; place the label in a separate centered layer using `maxLines = 1` and `softWrap = false`. Apply `iconSizeDp` consistently to raster and vector icons.

- [ ] **Step 3: Run focused Registration tests**

Run the Task 1 command plus `RegistrationIconAssetsTest`; expected: PASS.

- [ ] **Step 4: Scan visible Registration strings**

Run: `rg -n -i "próximamente|Regístrate|Teléfono|Agente bloqueado|ENTENDIDO|Tu agente" app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`

Expected: no matches.

### Task 3: Build and verify on the Samsung device

**Files:**
- No source changes unless verification exposes a reproducible defect.

**Interfaces:**
- Consumes: debug APK from the modified source.
- Produces: build, installation, foreground, screenshot, and login-log evidence.

- [ ] **Step 1: Build the debug APK**

Run: `./gradlew.bat :app:assembleDebug --console=plain`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install and open on Samsung**

Run `adb -s RFCXC0DGQ4Z install -r app/build/outputs/apk/debug/app-debug.apk`, then launch `com.royalshield.app/.MainActivity`.

- [ ] **Step 3: Capture visual evidence**

Capture a screenshot and confirm icons are fully inside, share one aligned column, labels are centered/single-line, and no Spanish text is visible.

- [ ] **Step 4: Capture one failing authentication flow**

Clear logcat immediately before one user-triggered login attempt; collect sanitized Firebase/Auth/App Check/Google status codes without credentials or tokens.

- [ ] **Step 5: Report truthfully**

Separate UI completion from authentication status. Do not call login fixed unless a real provider flow succeeds on-device.
