# Dashboard Agent Orb Controls Design

## Decision

Make the existing animated Royal Security Orb the Dashboard's primary security-agent control. Place two horizontal actions immediately below it: `SYSTEM ARMED` / `SYSTEM DISARMED` and `CHAT`.

## Scope

- Preserve the current plasma/particle orb assets and animation.
- Replace the Dashboard's central lock presentation with the shared orb.
- Place the armed-state and chat buttons in one row directly below the orb.
- Tapping the orb opens the existing voice agent.
- Tapping Chat opens the existing authenticated `VirtualAgentDialog`.
- Tapping the armed-state button arms immediately when disarmed and asks for confirmation before disarming.
- Reuse `PreferencesManager.setSystemArmed(...)`; do not create a second armed state.
- Remove the redundant floating chat and orb controls from the emergency-action row.
- Preserve all Dashboard navigation and unrelated cards.
- Preserve Splash, Registration, Plans, Security Setup, and Dashboard screens. The requested startup flow displays them in that order on each app launch; this behavior is tracked separately from the Dashboard component change.

## Truthfulness and safety

The armed control represents only behavior actually connected through `setSystemArmed`, currently Safe Path monitoring. It must not claim that VPN, malware scanning, privacy monitoring, or other independent modules were activated by this control.

Disarming requires an explicit confirmation dialog. Arming does not request unrelated permissions or silently enable independent modules.

## Interaction states

- Armed: green indicator and `SYSTEM ARMED`.
- Disarmed: red indicator and `SYSTEM DISARMED`.
- Chat: gold outlined action labelled `CHAT`.
- Orb: `ACTIVE` normally and the existing voice-agent interaction on tap.

## Verification

- Contract test verifies the Dashboard uses the shared orb in the security-agent section.
- Contract test verifies both labels and existing chat/voice callbacks remain wired.
- Contract test verifies `PreferencesManager.setSystemArmed` remains the single toggle path.
- Build focused Android tests and `assembleDebug` when Gradle loopback is available.
- Install the fresh APK and visually verify the centered orb, buttons below, arm/disarm confirmation, chat opening, and voice opening.
