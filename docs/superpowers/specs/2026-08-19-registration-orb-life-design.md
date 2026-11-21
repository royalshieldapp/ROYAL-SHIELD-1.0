# Registration Orb Life Design

Date: 2026-08-19

## Objective

Give the Royal Shield security orb a premium, living presence and simplify the Registration Screen without changing its existing background composition. All visible and accessibility copy introduced or modified by this work must be English.

## Scope

This change affects only the Registration presentation and a Registration-specific wrapper around the shared orb.

In scope:

- Registration foreground hierarchy and copy.
- Registration-specific orb presentation and motion.
- Authentication button alignment and enabled states.
- Focused layout, motion, asset, semantics, and English-copy tests.
- Physical Samsung visual verification in folded and unfolded layouts when available.

Out of scope:

- Billing, Play Console, DNS, Firebase provider configuration, Dashboard, Security Setup, voice dialogs, and backend behavior.
- Changes to authentication logic or provider credentials.
- Using the supplied 4x4 reference image directly as an application sprite atlas.

## Preserved Background Contract

The Registration background must remain visually and structurally unchanged:

- Keep `bg_registration_gold_ai.png`.
- Keep `ContentScale.Crop`.
- Keep scale `1.12`.
- Keep the existing parallax range of plus or minus 8 dp.
- Keep the cyan and magenta particles.
- Keep the current black overlay gradient with alpha stops `0.32`, `0.48`, and `0.68`.
- Keep foreground controls stationary while only the background parallax responds to motion.

No background asset replacement, recoloring, crop change, particle redesign, or overlay adjustment is authorized.

## Reference Image Decision

The supplied 1254x1254 image contains 16 opaque orb renderings and a checkerboard baked into the RGB pixels. It is not a production-ready transparent sprite atlas: its cells are not perfectly regular and the orb centers and diameters vary.

The implementation will therefore use the existing transparent `royal_security_orb.png` as the base texture. The reference image defines the intended warm golden energy and changing highlights, not the runtime frame source.

## Registration Architecture

### RegistrationScreen

`RegistrationScreen.kt` retains authentication and sensor orchestration but delegates the hero presentation to a new Registration-specific component. Authentication behavior must remain unchanged.

### RegistrationOrbHero

Create `RegistrationOrbHero.kt` to own:

- the Registration-only halo;
- the outer energy ring;
- the light sweep;
- the headline, supporting copy, and locked status chip;
- tap feedback and the locked-information callback.

This wrapper prevents Registration motion changes from altering Dashboard, Security Setup, or voice-dialog uses of `RoyalSecurityOrb`.

### RoyalSecurityOrb

Keep the shared component compatible with existing callers. Add only the smallest motion-profile or presentation inputs required by the Registration wrapper. Convert any modified accessibility descriptions to English.

### RegistrationLayout

Keep numeric geometry and alignment rules in `RegistrationLayout.kt`. Introduce constants for the hero, halo, button height, icon slot, and symmetrical side slots so labels remain optically centered.

## Visual Design

- Horizontal screen padding: 20 dp.
- Hero container: one premium composition with a 24 dp corner radius.
- Halo diameter: 220 dp.
- Visible locked orb diameter: approximately 196 dp.
- Locked orb alpha: 0.88.
- Headline: `Your security agent is waiting.`
- Supporting copy: `Sign up to configure available security, privacy, and family protection features.`
- Status chip: `SECURITY AGENT • LOCKED`
- Authentication section heading: `Choose how to continue`
- Phone and Google remain the only enabled authentication providers.
- Apple, GitHub, Facebook, and Email appear in a compact `COMING SOON` section and remain clearly disabled.
- Primary authentication buttons use a height between 72 and 76 dp.
- Provider icons remain 52 dp and entirely inside the button bounds.
- Left and right icon slots use equal width so the label is geometrically centered.
- Continue using `bg_button_gold.png`, compensating for its transparent canvas padding during rendering or through a non-destructive cropped sibling asset.

## Motion Design

The base orb texture does not rotate.

- Vertical float: -3 dp to +3 dp, reversible over 3.2 seconds.
- Breathing scale: 0.985 to 1.015, reversible over 3.2 seconds.
- Halo alpha: 0.14 to 0.24, reversible over 2.8 seconds.
- Light sweep rotation: one revolution every 12 seconds.
- Outer ring rotation: one revolution every 18 seconds.
- Tap feedback: compress to 0.97 for 90 ms, return over 220 ms, and emit one halo pulse.

When the orb is locked, tapping it only opens the informational dialog. It must never authenticate, unlock a feature, navigate forward, or report simulated success.

## Reduced Motion

When Android reduced-motion or animation-scale settings indicate that continuous motion should be minimized:

- render the orb, halo, and ring statically;
- disable float, breathing, sweep, continuous rotation, and repeated pulses;
- retain the same size, copy, enabled states, and tap behavior.

## Locked Dialog Copy

- Title: `Security Agent Locked`
- Body: `Continue with Phone or Google to begin setup. Feature availability depends on your plan, device permissions, and configured services.`
- Button: `GOT IT`

## State and Interaction Flow

1. Registration owns the current authentication-provider state.
2. `RegistrationOrbHero` receives only presentation state, reduced-motion state, and an `onLockedInfo` callback.
3. Tapping the locked orb triggers `onLockedInfo` and opens the informational dialog.
4. Phone and Google callbacks continue through the existing authentication code.
5. Disabled providers expose disabled semantics and cannot invoke callbacks.
6. Motion state remains local to the hero and does not enter authentication state or repositories.

## Error and Safety Behavior

- Missing orb or button assets must fail visibly during tests/build rather than fall back to an unrelated icon.
- Disabled providers must not display success, launch external authentication, or mutate registration state.
- The hero must avoid logging user information or authentication results.
- No new permission, service, dependency, backend endpoint, credential, or secret is required.
- Preserve touch targets of at least 48 dp.

## Files Expected to Change

- `app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`
- `app/src/main/java/com/royalshield/app/ui/screens/RegistrationLayout.kt`
- `app/src/main/java/com/royalshield/app/ui/components/RoyalSecurityOrb.kt`
- New: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationOrbHero.kt`
- Focused tests under `app/src/test/java/com/royalshield/app/ui/`
- Optional non-destructive cropped sibling of `bg_button_gold.png` only if rendering compensation cannot produce correct bounds.

The implementation must not modify Billing files, `DashboardScreen.kt`, `SecuritySetupScreen.kt`, or `DidAgentDialog.kt`.

## Test Strategy

Follow test-driven development:

1. Extend layout tests for 196 dp orb, 220 dp halo, button height, 52 dp icons, and symmetrical slots.
2. Extend motion tests for the specified ranges, durations, locked tap contract, and reduced-motion static state.
3. Extend English-copy coverage to include the new hero and modified shared-orb accessibility text.
4. Extend asset tests to reject a baked checkerboard or opaque rectangular background around the runtime orb.
5. Add Compose semantics coverage for enabled Phone/Google, disabled coming-soon providers, centered labels, and locked-orb behavior where the existing test stack supports it.
6. Run focused unit tests and `:app:assembleDebug`.
7. Install the debug APK on Samsung `RFCXC0DGQ4Z` and visually inspect alignment, clipping, motion, locked tap behavior, and both folded and unfolded layouts when available.

## Acceptance Criteria

- The preserved background contract is unchanged.
- No checkerboard, gray rectangle, or dirty edge appears around the orb.
- The locked orb is approximately 196 dp and feels alive without distracting from authentication.
- Buttons and foreground controls never move with the orb or background parallax.
- Phone and Google are the only enabled providers.
- Locked-orb tap opens only the English informational dialog.
- All visible and accessibility copy affected by this work is English.
- Provider labels are centered and icons remain fully inside button bounds.
- Touch targets are at least 48 dp.
- Reduced motion renders a stable composition.
- Focused tests and `assembleDebug` pass.
- Physical Samsung verification is reported separately from source/build evidence.
- No Billing or out-of-scope file is changed.
