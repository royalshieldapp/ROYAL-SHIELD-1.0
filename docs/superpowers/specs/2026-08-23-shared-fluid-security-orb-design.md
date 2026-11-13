# Shared Fluid Security Orb Design

Date: 2026-08-23

## Decision

Replace the current nearly static shared Royal Shield orb presentation with a visibly alive, fluid black-and-gold animation implemented with Jetpack Compose Canvas. The visual language is inspired by responsive voice assistants but remains an original Royal Shield design rather than a copy of ChatGPT branding.

The shared behavior applies everywhere `RoyalSecurityOrb` appears: Registration, Dashboard, Security Setup, and the voice-agent dialog. Each surface retains its current layout and interaction contract.

## Scope and constraints

In scope:

- Shared orb rendering and motion model.
- State-specific animation for locked, setup, active, listening, and processing states.
- Registration wrapper integration without moving its surrounding controls.
- Reduced-motion behavior.
- Focused motion, semantics, and rendering tests.
- Performance validation on the authorized Samsung device.

Out of scope:

- Redesigning Splash, Dashboard, Cyber, Security Setup, or voice-dialog layouts.
- Changing authentication, navigation, microphone, AI, or assistant behavior.
- Copying ChatGPT artwork, exact animation, colors, sounds, or brand identity.
- Adding third-party animation libraries or downloading runtime animation assets.

## Visual composition

`RoyalSecurityOrb` remains one bounded circular composable. It renders these layers from back to front:

1. a soft radial gold halo;
2. two or three low-opacity organic energy lobes drawn with Canvas;
3. a slow asymmetric sweep ring;
4. the existing transparent Royal Shield orb texture as the recognizable core;
5. a small moving highlight and state pulse.

The Canvas layers use deterministic phase functions based on sine/cosine interpolation. They must not use random values per frame, allocate bitmaps continuously, or create unbounded particle objects. Motion changes shape, alpha, scale, and phase subtly enough to remain premium rather than playful.

The implementation targets the project's `minSdk 28`; it does not require Android 13 `RuntimeShader`. A later shader enhancement may be considered separately after the compatible Canvas version is verified.

## State behavior

### LOCKED

- Slow breathing and a contained halo.
- No energetic success or unlocked appearance.
- Tapping retains the caller-provided informational behavior only.

### SETUP

- Gentle outward waves communicate configuration in progress.
- Motion must not imply completion until the existing setup state changes.

### ACTIVE

- Continuous visible fluid movement with moderate halo and highlight travel.
- Dashboard's 44 dp orb remains readable and does not clip its parent circle.

### LISTENING

- Faster breathing, brighter responsive lobes, and a stronger pulse.
- The animation reflects the existing listening state only; it does not infer microphone activity independently.

### PROCESSING

- Add a shared `PROCESSING` visual state for callers that already know the assistant is processing.
- Use inward/outward energy circulation distinct from LISTENING.
- Do not change voice-state business logic merely to force this appearance. Callers adopt it only where a real processing state already exists.

## Motion profiles

Motion values live in pure data models/functions so they can be unit tested. Each profile defines bounded ranges for:

- core breathing scale;
- vertical drift;
- halo alpha;
- lobe deformation amplitude;
- sweep speed;
- state pulse strength.

The shared component owns the continuous transition. Callers provide only the semantic state, size modifier, click callback, and optional reduced-motion override when needed for deterministic previews/tests.

State transitions interpolate rather than jump. The orb texture itself does not spin continuously; the surrounding energy layers carry the rotational movement.

## Reduced motion

Royal Shield respects the system animation-duration scale and any existing app-level reduced-motion signal.

When reduced motion is active:

- continuous drift, breathing, lobe deformation, sweep rotation, and repeated pulses stop;
- the orb renders as a stable composition with a soft static halo;
- state changes may use a short crossfade with no spatial movement;
- tap feedback uses a small opacity change rather than scale compression;
- state, accessibility semantics, authentication, and assistant behavior remain unchanged.

Reduced motion is an accessibility behavior, not a feature-disabled or error state.

## Accessibility and semantics

- All shared content descriptions are English.
- Descriptions reflect the real state: locked, setting up, active, listening, or processing.
- Decorative Canvas layers are merged into the orb's single semantic node.
- Existing click targets remain at least 48 dp where the caller provides an interactive control. The 44 dp Dashboard artwork remains inside its existing larger clickable parent if that parent supplies the touch target.
- Motion must not flash rapidly or use abrupt high-contrast changes.

## Performance and lifecycle

- Use one remembered infinite transition or frame clock per visible orb, not one per visual layer.
- Reuse brushes, colors, paths, and profile objects with `remember` where appropriate.
- Stop composition-driven animation automatically when the composable leaves the tree.
- Avoid bitmap creation, logging, network access, and repository work during frames.
- Keep Canvas drawing bounded to the composable and clip only where required by the caller.

Physical validation must check Dashboard's small orb and the 190-260 dp setup/voice variants for smoothness, clipping, temperature, and obvious battery-heavy behavior. Formal power profiling is desirable but not required for initial source acceptance.

## Architecture

Expected shared units:

- `RoyalSecurityOrbState`: semantic visual states, adding `PROCESSING`.
- `RoyalSecurityOrbMotionProfile`: immutable state-specific ranges and durations.
- pure phase-resolution functions for deterministic unit tests.
- `RoyalSecurityOrb`: public shared composable and semantic boundary.
- private Canvas layer composables/helpers for halo, lobes, ring, and highlight.

`RegistrationOrbHero` should delegate core orb animation to `RoyalSecurityOrb` and retain only Registration-specific container, copy, locked dialog, and any outer hero decoration. It must not run a second competing core breathing/float animation.

## Files expected to change

- `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- `app/src/main/java/com/royalshield/app/ui/screens/RegistrationOrbHero.kt`
- `app/src/main/java/com/royalshield/app/ui/components/DidAgentDialog.kt` only to map an existing real processing state when available
- focused tests under `app/src/test/java/com/royalshield/app/ui/components/`
- Compose UI tests under `app/src/androidTest/` if the current runner supports them

Dashboard and Security Setup call sites should require no layout redesign. Any necessary call-site edit must be limited to state mapping and must preserve the existing structure.

## Verification

1. Unit-test motion bounds, deterministic phase resolution, state profiles, and reduced-motion resolution.
2. Test English state descriptions and single semantic-node behavior.
3. Run focused orb and Registration tests.
4. Run `:app:assembleDebug` and relevant lint checks.
5. Install and inspect on Samsung in these surfaces:
   - Registration locked orb;
   - Dashboard active 44 dp orb;
   - Security Setup 190 dp orb;
   - voice dialog active, listening, and real processing states.
6. Repeat with Android animator duration scale at `0` or the app's reduced-motion signal enabled, then restore the user's original setting.

## Acceptance criteria

- The orb has clearly visible fluid life on all four surfaces without moving surrounding controls.
- Each semantic state has a distinct but consistent Royal Shield motion profile.
- No state simulates authentication, setup completion, microphone activity, or AI processing.
- Reduced motion stops continuous spatial animation and retains a stable premium appearance.
- The implementation works from Android 9 onward without a shader-only dependency.
- The shared texture does not spin, clip, reveal a rectangular background, or produce flashing artifacts.
- All affected accessibility descriptions are English.
- Focused tests and `assembleDebug` pass.
- Physical-device visual and performance evidence is reported separately from source/build evidence.
- Splash, Dashboard, Cyber, Security Setup, and voice-dialog layouts are not redesigned.
