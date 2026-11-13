# Lifetime Plans Reference Design

Date: 2026-08-23

## Decision

Adapt the supplied `CHOOSE YOUR PROTECTION` reference to Royal Shield's existing Jetpack Compose paywall while preserving the real Google Play Billing contract. The reference is visual direction only: Royal Shield continues to sell the existing `lifetime_starter`, `lifetime_gold`, and `lifetime_ultimate` one-time products queried as `INAPP`.

No `/month` label or hardcoded price may appear. Every purchasable plan displays the localized formatted price returned by Google Play and the label `one-time purchase`.

## Scope

In scope:

- Redesign `PremiumEliteScreen` and its plan-card presentation.
- Preserve product selection, purchase, restore, server verification, success navigation, error handling, and limited-feature continuation.
- Reuse the existing Starter, Golden, Ultimate shield assets and gold button assets when they render cleanly.
- Add focused pure layout/presentation models and tests where practical.
- Validate on the authorized Samsung in folded and unfolded layouts.

Out of scope:

- Changing product IDs, product type, prices, entitlements, Play Console, backend verification, or billing credentials.
- Converting lifetime products into subscriptions.
- Modifying Registration, Splash, Dashboard, or Cyber.
- Commit, push, deploy, Play Console changes, or a real purchase charge without separate authorization.

## Visual hierarchy

The screen uses the existing black background with restrained gold circuitry/particle detail. Content is vertically scrollable above a protected bottom action area.

1. Header: `CHOOSE YOUR PROTECTION`.
2. Subtitle: `Select the plan that fits your security needs.`
3. Three stacked plan cards: Starter, Golden, Ultimate.
4. Primary gold action: `CONTINUE`.
5. `Restore purchases` link.
6. Existing `Continue with Limited Features` remains available as a quiet secondary action and must not be obscured by the purchase button.

The design follows the reference's hierarchy and premium metal treatment without embedding the supplied screenshot as a background or copying rasterized text into the app.

## Plan cards

Each card is a wide black rounded rectangle with a metallic border, shield on the left, title/price panel in the center, description below, and selection indicator on the right.

### Starter

- Product: `lifetime_starter`.
- Silver/white metallic treatment.
- Title: `STARTER`.
- Description: `Essential protection`.
- Uses the existing Starter shield asset.

### Golden

- Product: `lifetime_gold`.
- Gold metallic treatment and selected glow.
- Title: `GOLDEN`.
- Description: `Advanced protection`.
- Badge: `MOST POPULAR`.
- Uses the existing Golden shield asset.
- Remains the default selected product only after the complete real catalog is loaded.

### Ultimate

- Product: `lifetime_ultimate`.
- Red core with restrained gold trim.
- Title: `ULTIMATE`.
- Description: `Complete protection`.
- Uses the existing Ultimate shield asset.

Cards use radio semantics: exactly one loaded plan is selected. Selection changes the border/glow and indicator without resizing the card enough to shift surrounding content. Every card retains a touch target of at least 48 dp and exposes its title, real price, one-time nature, and selected state to accessibility services.

## Price integrity

- Display `ProductInfo.price` only when it came from Google Play `ProductDetails`.
- Never substitute `$9.99`, `$19.99`, `$29.99`, `$39.49`, or any other fallback price.
- Display `one-time purchase`; never `/month`, `monthly`, or `subscription` for these products.
- Do not calculate and display a local percentage discount unless Google Play returns an actual purchasable offer whose charged price matches the display.
- If the catalog is incomplete or unavailable, keep purchase actions disabled and show the existing truthful retry state.

This removes the current risk where a locally altered display string could differ from the amount charged by Google Play.

## Interaction states

### Loading

Show the branded background, header, and one centered progress indicator. Do not show fake cards or prices.

### Products ready

Render all three real products. Select Golden by default. `CONTINUE` launches the existing purchase flow for the selected product.

### Purchasing

Keep the selected cards visible when possible, disable plan changes and the primary action, and show clear progress copy such as `Opening Google Play…`. Prevent double submission.

### Error or incomplete catalog

Show a sanitized message and `Retry`. Do not render purchasable cards with fallback values. Keep `Restore purchases` and limited-feature continuation available when safe.

### Purchase success

Continue through the existing verified-success callback only. Source/UI state alone must not grant entitlement.

### Cancellation

Return to the loaded catalog without displaying failure or granting access.

## Bottom action area

The bottom action area uses a subtle black gradient to keep the gold button legible over scroll content.

- Button label: `CONTINUE`.
- Enabled only when a real catalog product is selected and the UI is not purchasing.
- Uses the selected plan to invoke the existing `purchaseSelected` path.
- `Restore purchases` remains directly below or immediately above the limited-feature action.
- Folded layouts must allow access to all three actions without overlap.

## Accessibility and motion

- All affected visible and semantic text is English.
- Text supports font scaling without clipping prices or titles.
- Selection is not communicated by color alone; use radio state and selected semantics.
- Gold/red glows may breathe slowly, but respect system reduced-motion settings and become static when animations are disabled.
- Avoid flashing, aggressive bouncing, and continuous per-card animations that waste battery.

## Expected files

- `app/src/main/java/com/royalshield/app/ui/screens/PremiumEliteScreen.kt`
- `app/src/main/java/com/royalshield/app/ui/viewmodels/PaywallViewModel.kt` only where needed to remove misleading local price mutation or expose stable interaction state
- focused tests under `app/src/test/java/com/royalshield/app/ui/screens/` and/or `ui/viewmodels/`
- optional Compose UI tests under `app/src/androidTest/`

Do not alter `BillingManager` purchase product type, product IDs, verification, acknowledgement, or restore logic for this visual task.

## Verification

1. Test mapping of product IDs to Starter, Golden, and Ultimate presentation.
2. Test that no hardcoded currency value or monthly label is used.
3. Test default Golden selection only after a complete catalog is ready.
4. Test disabled purchase for loading, purchasing, error, and incomplete catalogs.
5. Run existing billing policy and verification tests.
6. Run focused paywall tests and `:app:assembleDebug`.
7. Install on Samsung and inspect folded/unfolded layout, selection, scrolling, Restore, limited continuation, and Google Play launch readiness.

A real paid purchase or restore E2E requires explicit authorization and a licensed Play testing build; it is not implied by successful compilation or local installation.

## Acceptance criteria

- The screen visibly matches the supplied hierarchy: header, three shield cards, Golden emphasis, selection controls, gold Continue action, and Restore link.
- The UI says `one-time purchase`, never `/month`.
- All displayed prices come from Google Play; no fallback or locally discounted display price exists.
- Existing `lifetime_*` IDs and `INAPP` purchase behavior remain unchanged.
- Purchase, cancellation, restore, verified entitlement, errors, and limited-feature continuation remain reachable.
- Loading/error states never imply that unavailable products can be purchased.
- The layout is premium black/silver/gold/red, accessible, and usable folded and unfolded.
- Registration, Splash, Dashboard, and Cyber are unchanged.
- Focused tests and debug build pass.
- Device installation is reported separately from licensed Play Billing E2E evidence.
