# Google Home APIs setup for Royal Shield

The Android integration uses Google Home APIs Android SDK 1.9.1 and supports:

- OAuth permission flow for one Google Home structure.
- Discovery of Matter and Works with Google Home lights and plug-in units.
- On/off commands.
- Brightness commands for devices exposing `LevelControl`.
- Local Room persistence of devices selected inside Royal Shield.

## Required Google Cloud configuration

### Current debug configuration (July 14, 2026)

- Google Home Developer Console project `Royalshield` is linked to Google Cloud project `royalshield-fbd06`.
- OAuth publishing status is `Testing` and the development account is registered as a test user.
- Android OAuth client `Royal Shield Android Debug` is registered for package `com.royalshield.app`.
- Debug SHA-1 `5C:A7:E2:AF:63:5E:15:36:8F:B4:6E:A3:2C:D6:49:3E:3B:2F:18:DA` is registered.
- Release OAuth registration remains pending until the production keystore or Google Play App Signing SHA-1 is available.

1. Select the Firebase/Google Cloud project used by `app/google-services.json`.
2. Configure the OAuth consent screen as External or Internal.
3. Add every development Google Account under **Audience > Test users** while the app is unverified.
4. Create an OAuth client with application type **Android**.
5. Use package name `com.royalshield.app`.
6. Add the SHA-1 fingerprint printed by `gradlew :app:signingReport` for the debug certificate.
   - Current local debug SHA-1: `5C:A7:E2:AF:63:5E:15:36:8F:B4:6E:A3:2C:D6:49:3E:3B:2F:18:DA` (registered).
7. Before testing a release build, add the SHA-1 fingerprint of the release signing certificate too.

Do not place OAuth client secrets, service-account JSON files, access tokens or refresh tokens in this repository. An Android OAuth client does not require a client secret inside the APK.

## Runtime requirements

- Android 10 or newer.
- Google Play services 26.24.22 or newer for SDK 1.9.1.
- A Google Account added as an OAuth test user until verification/registration is available.
- A Google Home structure containing compatible devices.
- A supported Google hub for Matter device access and control.

Open **Smart Home**, tap scan, select the Google Account and structure, grant device access, then select a returned device to persist it in Royal Shield.
