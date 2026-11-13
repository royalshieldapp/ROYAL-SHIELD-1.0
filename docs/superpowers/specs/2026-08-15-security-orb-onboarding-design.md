# Security Orb Onboarding and Voice Navigation Design

## Objective

Replace the oversized registration emblem with the supplied golden orb and make it a truthful security agent across registration and Dashboard. The orb guides a newly authenticated user through three optional security setup decisions and reuses the existing Dashboard voice assistant for navigation.

## Scope

- Registration Screen only: replace the existing large logo area with the orb without restructuring the rest of the screen.
- After successful registration or sign-in: show a three-step security setup flow.
- Dashboard Screen only: replace the current microphone button artwork with the orb while preserving its existing voice command routing.
- Integrate the existing authenticated text chatbot entry point with the same agent identity.
- Persist setup completion and individual choices per authenticated user on the device.

## Orb states

### Locked

- Visible in Registration Screen at a compact size.
- Does not request permissions or run protected actions.
- Tap response: "Regístrate o inicia sesión para activar tu agente de seguridad."

### Activated

- Begins only after Firebase reports an authenticated user.
- Animates from compact to expanded size.
- Starts the security setup flow if that user has not completed it.

### Dashboard

- Replaces only the existing 44 dp microphone button visual.
- Opens the existing voice assistant and uses the existing command-to-navigation mapping.
- The adjacent text chatbot remains available and shares the same agent identity.

## Security setup flow

The flow contains exactly three decisions. Each step presents a benefit, a limitation, and two explicit choices. Declining one feature never blocks access to the rest of the app.

### VPN

- Question: "¿Quieres configurar la VPN?"
- Benefit: improves privacy for compatible traffic, especially on public Wi-Fi.
- Limitation: it does not provide total anonymity or eliminate every risk.
- Accept: open the existing VPN configuration/consent flow.
- If the VPN is unavailable or lacks verified configuration, store the preference as pending and show `UNAVAILABLE`; never claim a connection.

### Automatic SMS phishing protection

- Question: "¿Quieres activar la protección automática contra phishing por SMS?"
- Disclosure: Royal Shield accesses received SMS to identify suspicious links and phishing patterns. Message content is not used for advertising or sold.
- Accept: show the in-app disclosure immediately before Android requests SMS permission.
- Decline: keep SMS protection disabled and allow later activation.
- Local analysis runs first.
- Advanced VirusTotal analysis is opt-in and transmits only an extracted URL, never the complete SMS or sender.
- Remove message bodies, senders, URLs, and provider response details from logs.
- A missing permission, provider configuration, or verified backend path must produce a disabled or `UNAVAILABLE` state.

### Secure QR scanner

- Question: "¿Quieres activar el escáner seguro de códigos QR?"
- Benefit: previews and evaluates a QR destination before the user opens it.
- Limitation: no scanner can guarantee that every destination is safe.
- Accept: open the existing QR scanner setup or permission flow.
- Decline: leave QR scanning disabled and allow later activation.

## Data and persistence

- Key setup state by authenticated Firebase UID; do not use one global completion flag for every account.
- Store only completion state and the three feature choices in encrypted preferences.
- Do not store SMS content, sender addresses, scanned QR payload history, or voice transcripts as part of onboarding.
- Do not send SMS content to the Royal Shield backend.
- VirusTotal credentials must not be introduced into UI code or committed files.

## Error handling and truthful states

- Authentication lost: return the orb to locked state and do not expose protected actions.
- Permission denied: mark only that feature disabled and continue.
- Permission permanently denied: explain how to enable it in Android Settings.
- Provider or backend unavailable: show a sanitized `UNAVAILABLE` reason and keep retry/configure-later available.
- Voice command not recognized: keep the user on Dashboard and ask them to try a supported destination.
- Never convert a preference to an activation claim without service-level evidence.

## Accessibility and visual behavior

- Preserve the black-and-gold visual identity.
- Use the transparent golden orb asset supplied by the user.
- Provide content descriptions for locked, setup, and voice-assistant states.
- Do not rely only on scale or color to communicate locked/active state; include supporting text and enabled state.
- Keep animations short and respect reduced-motion behavior where available.

## Validation

- Unit tests for per-user setup state, exact three-step order, decline behavior, and no fabricated activation.
- UI/state tests proving the locked orb cannot invoke protected actions.
- Tests proving SMS logs contain no body, sender, or URL.
- Existing voice routing tests or focused mapping tests for VPN, QR, phishing, SOS, and chatbot destinations.
- Android unit tests, lint, and debug assembly.
- Install the fresh APK on the authorized physical Samsung device and confirm process, foreground activity, registration orb, post-auth setup, and Dashboard voice navigation.

## Out of scope

- Redesigning Registration, Splash, Dashboard, or Cyber Screen structure.
- Deploying a new backend or changing production secrets.
- Claiming Google Play approval for SMS permissions.
- Claiming VPN, VirusTotal, QR, or SMS protection is active without end-to-end evidence.
