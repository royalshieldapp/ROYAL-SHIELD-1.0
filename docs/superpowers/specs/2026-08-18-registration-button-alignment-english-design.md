# Registration Button Alignment and English UI Design

## Goal

Make the Registration authentication controls visually consistent, easier to read, and fully English while preserving Royal Shield's premium black-and-gold identity.

## Scope

- Change only the Registration screen presentation and its focused layout tests.
- Preserve the existing authentication behavior and navigation.
- Do not redesign Splash, Dashboard, Cyber, or any unrelated screen.

## Button Layout

- Every authentication button uses the same width, height, corner shape, and internal spacing.
- Each raster icon is large, fully contained inside the button, and rendered at a consistent size.
- Icons occupy a fixed-width leading slot so all icons align vertically.
- The label is visually centered across the full button, independent of the icon width.
- Labels remain on one line on the tested Samsung viewport.
- Disabled providers retain the same geometry with reduced opacity and no misleading active state.

## English Copy

All visible Registration copy must be English, including headings, supporting text, provider labels, disabled-state labels, dialogs, and action labels.

Provider labels:

- `Continue with Phone`
- `Continue with Google`
- `Apple — Coming Soon`
- `GitHub — Coming Soon`
- `Facebook — Coming Soon`
- `Email — Coming Soon`

Primary heading:

- `Your security agent is waiting.`

No Spanish text may remain visible on this screen.

## Visual System

- Preserve the current black, gold, dark gray, and soft-white palette.
- Preserve the current gold button background asset and overall screen structure.
- Do not add third-party branding beyond the provider icons already used by the screen.

## Accessibility and Edge Cases

- Keep each enabled control at least 48 dp high.
- Preserve meaningful icon content descriptions.
- Prevent label wrapping or clipping on the authorized Samsung device.
- Keep disabled providers visibly disabled and non-interactive.
- Avoid overlap between icons, text, button edges, and neighboring controls.

## Verification

- Add or update focused layout tests for button height, icon size, alignment-slot behavior, and English labels.
- Run the focused Registration tests.
- Run `:app:assembleDebug`.
- Install and open the debug APK on Samsung `RFCXC0DGQ4Z`.
- Capture a device screenshot and visually confirm alignment, icon scale, one-line labels, and English-only copy.

## Out of Scope

- Fixing Google, Phone, or Email authentication failures.
- Changing Firebase, App Check, OAuth, backend, or provider configuration.
- Commit, push, deploy, or publication actions.
