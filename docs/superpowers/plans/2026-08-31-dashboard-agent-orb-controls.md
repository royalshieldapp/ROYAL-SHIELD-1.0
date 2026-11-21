# Dashboard Agent Orb Controls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Dashboard lock centerpiece and floating agent controls with the shared animated orb and two functional buttons directly below it.

**Architecture:** Add a small pure presentation model for armed-state labels/colors/confirmation behavior, then render the existing `RoyalSecurityOrb` and existing callbacks inside the current Dashboard security card. Reuse `PreferencesManager.setSystemArmed` and `VirtualAgentDialog`; do not duplicate state or services.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, SharedPreferences, JUnit 4.

## Global Constraints

- Do not remove or restructure Splash, Registration, Plans, Security Setup, Dashboard, or Cyber screens.
- Keep the current plasma orb assets and animation.
- Buttons are horizontal and immediately below the orb.
- Disarming requires confirmation; arming is direct.
- Do not claim independent protection modules are controlled by System Armed.
- Preserve all existing navigation callbacks.
- Do not push or deploy.

---

### Task 1: Agent control presentation contract

**Files:**
- Create: `app/src/main/java/com/royalshield/app/ui/dashboard/DashboardAgentControls.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/dashboard/DashboardAgentControlsTest.kt`

**Interfaces:**
- Consumes: `armed: Boolean`.
- Produces: `DashboardAgentControlPresentation(label: String, requiresConfirmation: Boolean)` through `dashboardAgentControlPresentation(armed)`.

- [ ] **Step 1: Write failing tests**

Test that armed state returns `SYSTEM ARMED` and requires confirmation before the next toggle; disarmed returns `SYSTEM DISARMED` and arms directly.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.dashboard.DashboardAgentControlsTest"`

Expected: compilation failure because the presentation API does not exist.

- [ ] **Step 3: Implement the pure presentation model**

Create the immutable data class and pure mapping function without Android dependencies.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same focused test and require zero failures.

### Task 2: Dashboard agent center integration

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/DashboardScreen.kt`
- Test: `app/src/test/java/com/royalshield/app/ui/dashboard/DashboardAgentControlsTest.kt`

**Interfaces:**
- Consumes: `RoyalSecurityOrb`, `PreferencesManager.setSystemArmed`, `showVoiceAssistant`, and `showTextAssistant`.
- Produces: centered agent orb with an armed-state button and Chat button beneath it.

- [ ] **Step 1: Extend the source-contract test and verify RED**

Require a `DashboardAgentCenter` composable call, `SYSTEM ARMED`, `SYSTEM DISARMED`, `CHAT`, voice callback, chat callback, and no legacy floating-control comments.

- [ ] **Step 2: Implement `DashboardAgentCenter`**

Render the shared orb at a stable size, then a horizontal row containing the state button and Chat button. Use confirmation only for disarming and call `PreferencesManager.setSystemArmed` only after confirmation.

- [ ] **Step 3: Replace the lock centerpiece and remove duplicate floating controls**

Keep the containing Dashboard card and emergency action unchanged. Replace only its internal lock visuals and remove the floating text-chat/orb boxes.

- [ ] **Step 4: Run focused and relevant unit tests**

Run the focused test plus `RoyalSecurityOrbMotionTest` and `VoiceCommandRouterTest`.

### Task 3: Startup contract, build, device proof, and commit

**Files:**
- Modify only if required: `app/src/main/java/com/royalshield/app/MainActivity.kt`
- Test only if required: existing onboarding tests.

**Interfaces:**
- Consumes: existing `AppState` flow.
- Produces: preserved startup-screen sequence without deleting any screen.

- [ ] **Step 1: Verify the startup states remain present**

Confirm `SPLASH`, `REGISTRATION`, `PREMIUM_PROMO`, `SECURITY_SETUP`, and `MAIN` remain wired. Do not implement a navigation change unless a failing contract proves the requested each-launch behavior is absent.

- [ ] **Step 2: Run Android verification**

Run focused tests, `:app:testDebugUnitTest`, and `:app:assembleDebug`. If Gradle fails before compilation because Windows loopback is unavailable, report it as an environment blocker rather than a code failure.

- [ ] **Step 3: Install and visually verify a fresh APK when available**

Confirm the orb, buttons below, state transition, disarm confirmation, Chat dialog, and voice dialog on the Samsung without purchasing or enabling unrelated permissions.

- [ ] **Step 4: Commit the isolated feature files**

Stage only the spec, plan, new presentation/test files, and relevant hunks from `DashboardScreen.kt`. Commit locally with `feat: center dashboard controls on security agent orb`. Do not push.
