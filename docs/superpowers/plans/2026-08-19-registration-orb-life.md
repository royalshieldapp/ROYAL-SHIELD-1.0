# Registration Orb Life Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan with test-first checkpoints and an independent review.

**Goal:** Give the Registration security orb a premium living presence, improve the Registration hierarchy and provider-button alignment, and keep the existing animated background contract unchanged.

**Architecture:** Keep authentication and sensor orchestration in `RegistrationScreen.kt`. Add a Registration-only `RegistrationOrbHero` so stronger animation does not alter Dashboard, Security Setup, or voice-dialog callers of the shared `RoyalSecurityOrb`. Keep testable geometry and motion values in pure Kotlin models/functions, with Compose consuming those values.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle Kotlin DSL, Android Debug Bridge.

## Global Constraints

- Preserve `bg_registration_gold_ai.png`, `ContentScale.Crop`, scale `1.12`, parallax plus or minus `8 dp`, cyan/magenta particles, and overlay alpha stops `0.32`, `0.48`, `0.68` exactly.
- Keep Phone and Google as the only enabled providers. Do not change authentication callbacks, credentials, Firebase configuration, navigation destinations, or provider logic.
- All Registration copy and modified accessibility text must be English.
- Do not modify Billing files, `DashboardScreen.kt`, `SecuritySetupScreen.kt`, `DidAgentDialog.kt`, backend files, DNS, or Play Console configuration.
- Do not use the supplied opaque 4x4 reference image at runtime. Continue with the transparent `royal_security_orb.png` asset.
- Preserve the existing dirty worktree. Do not commit, push, deploy, publish, delete, or rewrite unrelated changes.
- A locked-orb tap may only open the informational dialog; it must never authenticate, unlock, navigate, or report success.

---

### Task 1: Define testable Registration hero, geometry, and motion contracts

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationLayout.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationLayoutTest.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/components/RoyalSecurityOrbMotionTest.kt`

**Interfaces:**
- `registrationSocialButtonLayout()` returns `heightDp` in `72f..76f`, `iconSizeDp = 52f`, and equal leading/trailing slot widths.
- `registrationOrbHeroLayout()` returns `orbSizeDp = 196f`, `haloSizeDp = 220f`, `cornerRadiusDp = 24f`, and `horizontalPaddingDp = 20f`.
- A pure Registration motion profile exposes float `-3f..3f`, breath `0.985f..1.015f`, halo alpha `0.14f..0.24f`, float/breath duration `3200L`, halo duration `2800L`, sweep duration `12000L`, ring duration `18000L`, press scale `0.97f`, press duration `90L`, and release duration `220L`.
- Reduced-motion resolution returns static neutral values and disables continuous animation.

- [ ] **Step 1: Write failing geometry tests**

  Extend `RegistrationLayoutTest` to assert the hero dimensions, button height range, 52 dp icon, and equal leading/trailing slots. Retain assertions for the preserved background parallax.

- [ ] **Step 2: Write failing motion tests**

  Extend `RoyalSecurityOrbMotionTest` with exact Registration motion bounds/durations and a reduced-motion case that resolves to zero offset, scale `1f`, static halo, and no continuous motion.

- [ ] **Step 3: Run the RED tests**

  Run:

  `./gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.RegistrationLayoutTest" --tests "com.royalshield.app.ui.components.RoyalSecurityOrbMotionTest" --console=plain`

  Expected: FAIL because the hero layout and Registration motion contracts do not yet exist.

- [ ] **Step 4: Implement the smallest pure-Kotlin contracts**

  Add the layout and motion data needed by the tests. Preserve all existing shared-orb defaults so other callers render as before.

- [ ] **Step 5: Run the focused tests GREEN**

  Re-run the Step 3 command. Expected: PASS.

### Task 2: Build the Registration-only living orb hero and redesign the foreground

**Files:**
- Create: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationOrbHero.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationEnglishCopyTest.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationAccessPolicyTest.kt`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationIconAssetsTest.kt`
- Optional only if required after visual proof: create a non-destructive cropped sibling of `app/src/main/res/drawable-nodpi/bg_button_gold.png`

**Interfaces and copy:**
- `RegistrationOrbHero(reducedMotion: Boolean, onLockedInfo: () -> Unit, modifier: Modifier = Modifier)` owns halo, ring, light sweep, press feedback, hero copy, and locked chip.
- Hero headline: `Your security agent is waiting.`
- Supporting copy: `Sign up to configure available security, privacy, and family protection features.`
- Status chip: `SECURITY AGENT • LOCKED`
- Provider heading: `Choose how to continue`
- Dialog title: `Security Agent Locked`
- Dialog body: `Continue with Phone or Google to begin setup. Feature availability depends on your plan, device permissions, and configured services.`
- Dialog action: `GOT IT`

- [ ] **Step 1: Write failing source-contract tests**

  Extend English-copy and access-policy tests to require the approved copy, locked-only callback behavior, Phone/Google enabled state, and Apple/GitHub/Facebook/Email disabled `COMING SOON` presentation. Extend the asset test to require a transparent runtime orb and reject the supplied opaque checkerboard image as a runtime asset.

- [ ] **Step 2: Run the RED Registration suite**

  Run:

  `./gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.Registration*Test" --tests "com.royalshield.app.ui.components.RoyalSecurityOrbMotionTest" --console=plain`

  Expected: FAIL on the new hero/copy/behavior contracts.

- [ ] **Step 3: Implement `RegistrationOrbHero`**

  Render the transparent orb at 196 dp and alpha `0.88`, inside a 220 dp halo. Animate vertical float, breath, halo alpha, 12-second light sweep, and 18-second outer ring without rotating the base texture. Implement press compression for 90 ms, 220 ms release, and one halo pulse. When reduced motion is active, render the same composition statically.

- [ ] **Step 4: Integrate the approved Registration hierarchy**

  Replace only the Registration foreground hero with `RegistrationOrbHero`. Keep the current background layer untouched. Add the approved heading/copy/chip, then Phone and Google primary actions, then a compact disabled `COMING SOON` section for Apple, GitHub, Facebook, and Email.

- [ ] **Step 5: Correct provider-button alignment**

  Keep `bg_button_gold.png`. Render each enabled button at 72-76 dp with a 52 dp icon in a fixed left slot and an equal empty right slot so the label is centered across the entire button. Keep all content inside the button bounds and all touch targets at least 48 dp.

- [ ] **Step 6: Implement locked dialog and English semantics**

  Wire the orb tap only to the approved English informational dialog. Use English content descriptions for the orb, providers, status, and disabled state. Do not introduce fake-success or navigation paths.

- [ ] **Step 7: Run the focused suite GREEN**

  Re-run the Step 2 command. Expected: PASS.

- [ ] **Step 8: Verify scope and background preservation**

  Run `git diff -- app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt` and confirm the background asset, crop, scale, parallax, particles, and overlay alpha stops are unchanged. Run `git status --short` and confirm no protected file was modified by this task.

### Task 3: Build and physically verify without overstating results

**Files:**
- No source changes unless a visual defect is reproduced and fixed through Tasks 1-2 tests.

- [ ] **Step 1: Run focused tests and compile**

  Run:

  `./gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.screens.Registration*Test" --tests "com.royalshield.app.ui.components.RoyalSecurityOrbMotionTest" :app:assembleDebug --console=plain`

  Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install on the authorized Samsung when available**

  Verify `adb devices`, then install with `adb -s RFCXC0DGQ4Z install -r app/build/outputs/apk/debug/app-debug.apk` and launch `com.royalshield.app/.MainActivity`.

- [ ] **Step 3: Capture visual evidence**

  Inspect folded and unfolded layouts when device state permits. Confirm: unchanged background, no checkerboard/rectangle around the orb, 196 dp visual prominence, unclipped 52 dp icons, centered labels, stationary controls, restrained motion, English copy, and locked tap opening only the informational dialog.

- [ ] **Step 4: Check reduced motion if controllable**

  With animation scale disabled or reduced, verify the composition remains visible and stable without continuous motion. Restore the user's device setting afterward.

- [ ] **Step 5: Report evidence by layer**

  Separate source/test/build evidence from device visual evidence. If the device cannot be inspected, report visual behavior as `NO VERIFICADA`. Do not claim Firebase login, Billing, DNS, or publication was validated by this task.
