# Registration Six-Provider Authentication Design

Date: 2026-08-23

## Decision

Redesign Registration as a compact, premium authentication screen with six real Firebase Authentication methods. Phone and Google remain functional. Email becomes the third primary action. Facebook, GitHub, and Apple appear under an expandable `More sign-in options` section.

No provider may navigate forward, create a local success state, or appear available unless its real authentication path is configured. Missing external configuration must fail closed as `Unavailable`.

## Scope

In scope:

- Registration presentation and provider selection.
- Real Firebase Auth flows for Email, Facebook, GitHub, and Apple.
- Existing real Phone and Google flows.
- One shared post-authentication path that persists the Firebase profile before navigation.
- Provider-specific loading, cancellation, error, unavailable, and account-collision states.
- Focused unit, Compose UI, build, and physical-device verification.

Out of scope:

- Splash, Dashboard, Cyber, Billing, VPN, plans, or unrelated onboarding screens.
- Commit, push, deploy, provider-console changes, or handling real secrets without separate authorization.
- Fake provider responses, local-only authentication, hardcoded credentials, or simulated success.

## Visual Layout

Preserve the existing Registration background, stationary foreground contract, black-and-gold palette, and Registration-only orb presentation.

The vertical hierarchy is:

1. Compact security-orb hero.
2. Heading: `Choose how to continue`.
3. Primary actions: `Continue with Phone`, `Continue with Google`, and `Continue with Email`.
4. Expandable action: `More sign-in options`.
5. Expanded providers: `Continue with Facebook`, `Continue with GitHub`, and `Continue with Apple`.
6. Terms and Privacy links or clearly non-interactive copy until real destinations are confirmed.

All affected visible and accessibility text is English. Controls remain usable on the Samsung in folded and unfolded layouts, with at least 48 dp touch targets. The orb and background may animate, but buttons and text remain stationary. Provider labels are geometrically centered and icons are fully contained.

The implementation must fix the invalid or mislabeled font asset before claiming the requested typography is active. It must not silently substitute `Oxnitro`; a verified font file and license are required. Until supplied, use the existing safe theme fallback and report the exact typography as pending.

## Authentication Architecture

### Provider policy

`RegistrationAccessPolicy` becomes configuration-aware rather than a hardcoded visual promise:

- Phone and Google retain their current real flows.
- Email is available only when Firebase Email/Password is enabled and the local flow is present.
- Facebook is available only when required non-secret app identifiers are configured and Firebase/Meta provider setup is complete.
- GitHub is available only when the Firebase provider and GitHub OAuth application are configured.
- Apple is available only when Apple Developer and Firebase provider configuration are complete.

The app must distinguish `Available`, `Loading`, and `Unavailable(reason)` without exposing secrets or internal provider responses.

### Shared completion path

Every successful provider returns a real `FirebaseUser` to one coordinator. The coordinator must:

1. call `AuthProfileRepository.ensureProfile`;
2. preserve existing non-empty profile fields;
3. surface a safe retryable error if profile persistence fails;
4. invoke Registration success navigation only after authentication and required profile persistence succeed.

This closes the current inconsistency where Google authentication does not guarantee the same profile persistence as Phone.

### Email

Email uses Firebase Email/Password authentication through a Compose flow integrated with the current navigation, not the legacy activity.

It supports:

- create account;
- sign in to an existing account;
- email normalization and existing strong password validation;
- confirmation-password matching during account creation;
- Firebase email verification after account creation;
- resend verification with throttling;
- password reset;
- safe errors for invalid credentials, disabled user, collision, network failure, and rate limiting.

Unverified-email policy: authentication may succeed in Firebase, but protected onboarding navigation remains blocked until `isEmailVerified` is true. The UI explains how to verify and refresh status without claiming an email was delivered if Firebase reports failure.

### GitHub and Apple

GitHub and Apple use Firebase Android `OAuthProvider` flows with their official provider IDs: `github.com` and `apple.com`. The coordinator checks `pendingAuthResult` before starting a new browser flow so Activity recreation does not force duplicate authentication.

Only the minimum identity scopes needed for account creation are requested. Access tokens are never logged or persisted by Royal Shield unless a future, separately approved feature requires provider API access.

### Facebook

Facebook obtains a real Facebook Login access token through the supported Android provider flow and exchanges it for a Firebase credential. App IDs may be resources or build configuration; the Meta App Secret must never be packaged in Android.

Only basic identity permissions are requested. Royal Shield does not request Page, advertising, friends, or publishing permissions for authentication.

### Account collisions and linking

When Firebase reports that an email already exists with another provider, Registration must not silently create a duplicate account or automatically link identities. It shows a safe message identifying the required existing sign-in method when Firebase permits that lookup. Provider linking is a separate authenticated-account operation and is not part of this Registration scope.

## State and Error Model

Authentication state is owned by a ViewModel/coordinator, not by composable business logic. Each attempt tracks one provider, prevents double submission, and can return:

- `Idle`;
- `Launching(provider)`;
- `AwaitingEmailVerification`;
- `PersistingProfile`;
- `Success`;
- `Cancelled`;
- `Unavailable(provider, safeReason)`;
- `Error(provider, safeMessage, retryable)`.

Cancellation is not reported as failure. Raw tokens, passwords, private keys, full phone numbers, provider payloads, and sensitive Firebase exceptions are never logged. UI messages remain useful but sanitized.

## External Configuration Gates

Implementation can prepare the source safely, but real completion requires user-controlled configuration:

- Firebase Console: enable Email/Password, Facebook, GitHub, and Apple providers.
- Meta Developer: Android app association, package/signature settings, App ID, App Secret configured only in the provider console/Firebase where required, and valid OAuth redirect URI.
- GitHub: OAuth App Client ID/Secret and Firebase callback URL.
- Apple Developer: active program membership, Service ID, Team ID, Key ID, private key, return URL, and private-email relay configuration when applicable.

Secrets must not be added to source control, `strings.xml`, `BuildConfig`, APK resources, logs, screenshots, or documentation. Missing values are documented with explicit placeholders in `.env.example` or provider setup documentation only when that location is appropriate; Android must not receive server-side secrets.

## Files Expected to Change

Likely production files:

- `RegistrationScreen.kt`
- `RegistrationAccessPolicy.kt`
- `RegistrationLayout.kt`
- `RegistrationOrbHero.kt`
- a new Registration auth ViewModel/coordinator and provider service classes under existing auth/UI conventions
- `AuthProfileRepository.kt`
- `MainActivity.kt` only for required navigation wiring
- `app/build.gradle.kts` and Android resources only for verified provider dependencies and non-secret identifiers

Legacy `RegisterActivity.kt` may be used as behavioral reference but must not be reactivated or duplicated into the Compose flow.

Expected tests:

- provider availability and fail-closed configuration;
- Email validation, create/sign-in/verify/reset states;
- OAuth pending-result, cancellation, collision, and safe-error mapping;
- shared profile persistence after every provider;
- Compose semantics for primary and expandable providers;
- no callback from unavailable controls;
- English copy, accessibility labels, layout, and reduced motion.

## Verification

Verification proceeds in layers:

1. Focused JVM tests for validation, state transitions, error mapping, policy, and profile coordination.
2. Compose UI tests for button behavior, expanded options, loading/disabled semantics, and Email forms.
3. `:app:assembleDebug` and relevant lint checks.
4. Physical Samsung inspection folded and unfolded.
5. One clean real-device authentication attempt per provider, with safe evidence from Firebase Auth users and expected navigation/profile creation.

Source/build success is not provider E2E proof. A provider remains `NO VERIFICADA` until its console configuration and physical flow succeed.

## Acceptance Criteria

- Phone, Google, Email, Facebook, GitHub, and Apple never simulate success.
- Phone and Google continue working without regression.
- Email registration, verification, login, and password reset work with Firebase.
- Facebook, GitHub, and Apple authenticate through real configured providers.
- Every provider uses the same profile-persistence and navigation contract.
- Unavailable providers are truthful, non-interactive, and explain the safe next step.
- The screen remains compact and usable folded and unfolded.
- All affected copy and accessibility descriptions are English.
- No secrets or sensitive authentication material enter source, logs, screenshots, or APK resources.
- Focused tests and the debug build pass.
- Physical provider results are reported independently as `READY`, `UNAVAILABLE`, or `NO VERIFICADA`.
- Splash, Dashboard, Cyber, Billing, and unrelated features remain unchanged.
