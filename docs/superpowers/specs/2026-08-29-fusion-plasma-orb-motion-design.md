# Fusion Plasma Orb Motion Design

## Decision

Make the existing Royal Security Orb feel like living fusion plasma across Registration, security setup, Dashboard, and the voice agent. Use organic fusion motion for normal states and a more intense reactor response only while the agent is listening. Preserve every screen's current structure and black-and-gold identity.

## Current limitation

`RoyalSecurityOrb` renders only `royal_security_orb.png` with a regular sine-wave scale and vertical offset. `RegistrationOrbHero` adds a halo and rotating arcs, but still renders one static orb image. Ten plasma frames already exist as `royal_security_orb_1.png` through `royal_security_orb_10.png`; no production composable currently plays them.

## Scope

- Use one reusable plasma renderer in Registration, `SecuritySetupScreen`, Dashboard, and `DidAgentDialog`.
- Preserve the current callbacks, semantics, navigation, authentication, billing, and voice behavior.
- Do not restructure Registration, Dashboard, Splash, or Cyber.
- Do not replace or regenerate the approved orb assets in this task.
- Do not add backend services, network animation downloads, or runtime-generated claims.

## Motion states

### Locked

- Play the frame sequence slowly with restrained halo intensity.
- Keep the existing reduced opacity and locked messaging.
- Use a small organic pulse and microfloat; tapping still opens information only.

### Setup

- Use the full organic fusion profile.
- Play the ten frames forward in a seamless loop with non-uniform frame timing.
- Combine the frame loop with a gentle `0.97x` to `1.06x` scale envelope, small vertical drift, and two magnetic rings rotating in opposite directions.

### Active

- Use the same fusion profile at compact Dashboard intensity.
- Preserve the existing click target and voice-agent callback.
- Reduce expensive glow layers at small sizes to avoid unnecessary GPU work.

### Listening

- Temporarily switch to the reactor profile.
- Increase frame cadence, halo energy, ring speed, and scale up to a hard maximum of `1.10x`.
- Avoid flashing, abrupt jumps, random jitter, or unbounded scaling.
- Return smoothly to the active fusion profile when listening ends.

## Rendering architecture

Create a reusable frame-sequence model that maps elapsed animation phase and `RoyalSecurityOrbState` to:

- drawable frame index;
- scale;
- vertical offset;
- halo alpha;
- inner and outer ring rotation;
- profile cadence.

The composable owns only rendering and interaction. Pure mapping functions own timing and state transformation so unit tests can verify deterministic behavior without running Compose animations.

The existing `RoyalSecurityOrb` remains the public entry point. `RegistrationOrbHero` reuses the same renderer with a locked profile inside its current panel. This avoids separate implementations drifting across screens.

## Frame behavior

- Use all ten existing frame resources in their numbered order.
- Normal fusion should not advance every frame at an identical interval. Use a deterministic timing curve, not randomness, so motion remains organic but testable.
- Listening uses a faster deterministic cadence.
- Frame changes must not alter layout size.
- Predeclare the resource list; do not resolve resource names dynamically at runtime.

## Accessibility and performance

- Respect the existing reduced-motion decision in Registration and add the same behavior to shared orb rendering.
- Reduced motion uses one static approved frame, a stable scale, and no continuously rotating rings.
- Preserve content descriptions for locked, setup, active, and listening states.
- Do not encode state only through color or animation; existing labels and semantics remain authoritative.
- Avoid bitmap allocation on every animation frame. Reuse Compose painters/resources and keep transient effects in drawing layers.
- Keep the Dashboard variant lightweight because it remains visible for long periods.

## Error handling

- If a frame resource is unavailable at build time, resource compilation must fail; do not fall back to a fabricated remote asset.
- Animation state must never grant authentication, premium access, feature activation, or voice-listening status.
- Listening intensity is driven only by the existing real listening state.

## Expected files

- `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- `app/src/main/java/com/royalshield/app/ui/screens/RegistrationOrbHero.kt`
- focused tests under `app/src/test/java/com/royalshield/app/ui/components/`
- consumer files only if required to pass reduced-motion or state explicitly without changing their layout

## Verification

1. Unit-test ordered use of all ten frames.
2. Unit-test deterministic normal and listening cadence.
3. Unit-test scale ceilings: normal at or below `1.06x`, listening at or below `1.10x`.
4. Unit-test that reduced motion disables frame progression and continuous ring rotation.
5. Run focused orb tests, Android unit tests, and `:app:assembleDebug`.
6. Install the fresh debug APK on the authorized Samsung.
7. Visually verify locked Registration, setup, compact Dashboard, and listening transitions.
8. Record the Android 16 16 KB native-library warning separately; orb motion success does not resolve that release blocker.

## Acceptance criteria

- The orb visibly resembles living fusion plasma instead of a static image that only floats and zooms.
- Organic fusion is the default motion and reactor intensity appears only while listening.
- All four consumers use the same motion engine with state-appropriate intensity.
- No screen structure, authentication path, billing path, or voice callback changes.
- Motion is deterministic, accessible, bounded, and efficient enough for continuous Dashboard display.
- Completion requires fresh tests, build, Samsung installation, and visual evidence; source changes alone are insufficient.
