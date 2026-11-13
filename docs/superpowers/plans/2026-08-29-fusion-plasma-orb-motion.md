# Fusion Plasma Orb Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Animate the existing ten Royal Security Orb frames as bounded organic fusion plasma, with a stronger reactor profile only while the real voice agent is listening.

**Architecture:** Keep `RoyalSecurityOrb` as the shared public composable. Add pure deterministic frame/motion mapping in the same focused component file, then let Registration reuse that renderer inside its existing halo panel. State controls cadence and intensity; reduced motion freezes progression.

**Tech Stack:** Kotlin, Jetpack Compose animation/drawing APIs, Android drawable resources, JUnit 4.

## Global Constraints

- Preserve Registration, Dashboard, Splash, and Cyber screen structure.
- Use `royal_security_orb_1.png` through `royal_security_orb_10.png` in numbered order.
- Normal scale must not exceed `1.06x`; listening scale must not exceed `1.10x`.
- Reduced motion disables frame progression and continuous ring rotation.
- Do not modify authentication, billing, navigation, entitlement, or voice callbacks.
- Do not add network-loaded assets or randomness.

---

### Task 1: Deterministic plasma motion model

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- Test: `app/src/test/java/com/royalshield/app/ui/components/RoyalSecurityOrbMotionTest.kt`

**Interfaces:**
- Consumes: `RoyalSecurityOrbState`, normalized animation phase in `0f..1f`, and `reducedMotion: Boolean`.
- Produces: `ROYAL_SECURITY_ORB_FRAMES`, `OrbPlasmaMotion`, and `resolveOrbPlasmaMotion(state, phase, reducedMotion)`.

- [ ] **Step 1: Extend the failing tests**

Assert that the resource list contains the ten numbered drawables in order; reduced motion always returns frame zero and zero ring rotation; every state stays within its scale ceiling; listening advances faster than active for the same elapsed phase.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.components.RoyalSecurityOrbMotionTest"`

Expected: compilation failure because the new motion APIs do not exist.

- [ ] **Step 3: Implement the pure model**

Add a fixed `intArrayOf(R.drawable.royal_security_orb_1, ..., R.drawable.royal_security_orb_10)`. Map phase through a deterministic eased cadence, calculate bounded scale/offset/halo values, and return zero continuous motion for reduced motion.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same focused Gradle test and require zero failures.

### Task 2: Shared plasma renderer and state profiles

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationOrbHero.kt`
- Test: `app/src/test/java/com/royalshield/app/ui/components/RoyalSecurityOrbMotionTest.kt`

**Interfaces:**
- Consumes: `resolveOrbPlasmaMotion` from Task 1.
- Produces: shared frame playback, opposing magnetic rings, organic halo, and state-specific visual intensity through `RoyalSecurityOrb`.

- [ ] **Step 1: Add source-contract assertions**

Require `RoyalSecurityOrb` to select its painter from `ROYAL_SECURITY_ORB_FRAMES`, require two opposing ring rotations, and require Registration to call the shared renderer instead of painting `R.drawable.royal_security_orb` directly.

- [ ] **Step 2: Run focused tests and verify RED**

Expected: the shared-renderer assertions fail against the current static image implementation.

- [ ] **Step 3: Implement the shared renderer**

Drive a normalized infinite phase with Compose, resolve the current frame and motion profile, render the frame at a stable layout size, draw two restrained magnetic arcs, and retain the existing click semantics and state descriptions.

- [ ] **Step 4: Reuse it in Registration**

Replace only the static orb `Image` inside `RegistrationOrbHeroContent` with `RoyalSecurityOrb(state = LOCKED, ...)`, pass reduced-motion behavior without changing the surrounding panel, copy, callback, or layout.

- [ ] **Step 5: Run focused tests and verify GREEN**

Require the motion and source-contract tests to pass.

### Task 3: Verification, Samsung evidence, and authorized commit

**Files:**
- Verify all modified and untracked files already present in the repository.
- Update no external configuration, Firebase setting, deployment, or secret.

**Interfaces:**
- Consumes: completed shared renderer.
- Produces: test/build/device evidence and one local commit containing the authorized working tree.

- [ ] **Step 1: Run Android verification**

Run: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`

If Java loopback blocks Gradle before compilation, report that environment failure and use the latest already-built APK only for explicitly labeled non-fresh visual evidence.

- [ ] **Step 2: Run repository checks**

Run: `git diff --check`, `git status --short --branch`, and inspect the staged diff summary. Do not discard existing work.

- [ ] **Step 3: Install and inspect when a fresh APK exists**

Run: `adb -s RFCXC0DGQ4Z install -r app/build/outputs/apk/debug/app-debug.apk`, confirm `com.royalshield.app/.MainActivity`, and visually inspect locked, setup, active, and listening states without claiming unobserved screens.

- [ ] **Step 4: Create the authorized local commit**

Stage the complete working tree with `git add -A` only after reviewing its summary. Commit locally with a message describing the consolidated Royal Shield Android, security, and plasma-orb work. Do not push or deploy.
