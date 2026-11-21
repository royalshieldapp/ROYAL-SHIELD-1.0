# Royal Shield Camera Gateway - Production Design

Date: 2026-08-10  
Status: Approved design, pending written-spec review  
Deployment target: Android plus the FastAPI/SQLAlchemy/PostgreSQL backend deployed by Render

## 1. Objective

Deliver a real local-camera integration without a physical hub. Royal Shield Android discovers and configures ONVIF cameras on the active Wi-Fi LAN, accepts manually configured RTSP cameras, and renders compatible live streams. The Render backend stores user-owned camera metadata and normalized events, but never attempts to reach private LAN addresses or arbitrary camera URLs.

The module must never invent cameras, connectivity, capabilities, events, remote access, recording, or detection results. Unsupported or unverified capabilities are shown as unavailable.

## 2. Verified Existing Context

- Android uses Kotlin, Compose, ViewModels, repositories, Room, OkHttp/Retrofit, Firebase Authentication, and encrypted preferences.
- Navigation already exposes the `security_camera` route.
- The existing `SecurityCameraScreen` and `SecurityCameraViewModel` mix the phone CameraX preview with simulated remote-camera presentation, including a hard-coded stream URL and success-like logs. They are replaced within the existing route.
- Render deploys `royal_shield_backend_unified` with FastAPI, SQLAlchemy, and PostgreSQL. The older Node backend is not the Camera Gateway target.
- Firebase ID tokens are already verified server-side through `require_firebase_uid`.
- Media3 RTSP is not currently included.
- Room is version 3 and currently uses a destructive fallback. Camera persistence requires an explicit non-destructive migration.

## 3. Scope

### Included

- ONVIF WS-Discovery on the active Wi-Fi LAN.
- ONVIF device information and capability queries when supported.
- Manual RTSP camera configuration and real connection testing.
- Media3-based RTSP playback behind a replaceable stream-provider interface.
- Encrypted local credential storage.
- Room storage for local camera metadata without secrets.
- Camera dashboard, discovery, add, details, and live-view screens.
- Authenticated Render API for camera metadata and normalized events.
- Ownership enforcement, input validation, log sanitization, and SSRF-safe backend behavior.
- Unit, backend, build, and available physical-device verification.

### Excluded

- Ring, Nest, Reolink Cloud, or other unverified cloud adapters.
- A physical hub, permanent phone gateway, NVR, tunnel, or remote LAN access.
- Cloud video storage or continuous recording.
- Royal Shield person detection unless a real camera event or separately verified vision model provides it.
- Claims of 24/7 monitoring when the phone is outside the camera LAN.

## 4. Android Architecture

New code lives under `com.royalshield.app.features.cameragateway`:

```text
cameragateway/
  data/local/
  data/remote/
  domain/model/
  domain/repository/
  network/
  onvif/
  rtsp/
  repository/
  ui/
  viewmodel/
```

The dependency direction is:

```text
Compose UI -> ViewModel -> CameraRepository -> local/ONVIF/RTSP/backend data sources
```

Composables contain rendering and user events only. ONVIF, RTSP, persistence, authentication, and networking remain outside the UI layer.

### Domain contracts

`SecurityCamera` contains a stable local ID, display name, optional manufacturer/model, LAN endpoint, connection type, current verified status, capabilities, local-only flag, and last verification time.

Connection types are `ONVIF`, `RTSP`, and reserved `CLOUD_API`. Camera statuses are `ONLINE`, `OFFLINE`, `AUTH_REQUIRED`, and `ERROR`. Capabilities are `LIVE_STREAM`, `MOTION_EVENTS`, `AUDIO`, `TWO_WAY_AUDIO`, `PTZ`, `RECORDING`, and `SNAPSHOT`.

`CameraAdapter` abstracts connect, capability query, stream URI retrieval, and status verification. The initial implementations are `OnvifCameraAdapter` and `RtspCameraAdapter`. Cloud adapter names remain extension points only and have no fake implementations.

`CameraStreamProvider` owns playback compatibility so a future engine can replace Media3 without changing UI or ViewModels.

## 5. ONVIF Discovery

Discovery runs only when Android reports an active Wi-Fi network. It acquires a Wi-Fi `MulticastLock`, sends a bounded WS-Discovery probe, collects deduplicated responses during a fixed deadline, and releases the lock in `finally` for success, timeout, cancellation, or failure.

XML parsing disables DTDs and external entities. Discovery results are normalized and validated before use. For every real response, the repository may query device information and capabilities. Authentication failures produce `AUTH_REQUIRED`; timeouts and protocol failures produce explicit errors. Zero responses produce an empty result, never sample cameras.

Only `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, and `CHANGE_WIFI_MULTICAST_STATE` are considered for this module. No location, SMS, overlay, or unrelated permission is added.

## 6. RTSP and Live View

Manual RTSP input is split into name, host/IP, port, username, password, and path. The application validates scheme, endpoint, port range, and control characters. It constructs the credential-bearing URI only in memory at the connection boundary.

Connection testing must reach the endpoint and receive a protocol/stream response. A successful TCP socket alone is not presented as playable video. Media3 RTSP is the initial playback engine. Codec, authentication, transport, and timeout failures map to honest UI states. Full credential-bearing URIs are never written to logs, Room, analytics, exceptions, or backend payloads.

## 7. Local Persistence and Credentials

Room stores local camera metadata, LAN endpoint fields, type, capabilities, last verified status, and timestamps. It does not store usernames, passwords, or credential-bearing RTSP URLs.

Credentials are stored in a dedicated encrypted keystore/preferences component keyed by local camera ID. Failure to initialize encrypted storage fails closed; the module must not fall back to plaintext preferences.

The Room schema gains a camera entity and DAO through an explicit migration from version 3. Existing automation rules and smart-device records must survive. The new path must not rely on destructive migration.

## 8. UI and Navigation

The existing `security_camera` destination becomes the camera dashboard. Nested destinations provide discovery, manual add, camera details, and live view. No protected Registration, Splash, Dashboard, or Cyber screen is structurally changed.

All screens preserve the premium black-and-gold visual language. UI status derives from repository state. A LAN-only camera visibly states that remote access is unavailable when the phone is not on the same network. Empty, loading, authentication-required, offline, incompatible-stream, and error states are first-class states.

## 9. Backend Architecture

The production implementation uses FastAPI, SQLAlchemy, PostgreSQL, and Render.

`CameraRecord` stores server ID, Firebase UID owner, client-local identifier, name, manufacturer, model, connection type, reported status, reported capabilities, timestamps, and last report time. It does not store LAN host, private IP, RTSP path, username, password, or stream URL.

`CameraEventRecord` stores server ID, camera ID, normalized type, event timestamp, optional confidence, bounded metadata, and creation time.

`CameraGatewayService` performs registration, ownership checks, metadata updates, event normalization, and deletions. Every user route depends on `require_firebase_uid`; client-provided owner IDs are rejected or ignored.

Routes are:

```text
POST   /api/cameras
GET    /api/cameras
GET    /api/cameras/{camera_id}
PATCH  /api/cameras/{camera_id}
DELETE /api/cameras/{camera_id}
POST   /api/cameras/{camera_id}/events
GET    /api/cameras/{camera_id}/events
```

The backend never resolves or requests a client camera endpoint. Models do not accept arbitrary URLs. Invalid input returns HTTP 422, unauthenticated access returns 401, cross-user access returns 404 to avoid ownership disclosure, and unavailable infrastructure returns an honest 503 response.

## 10. Events

Supported normalized event types are `MOTION_DETECTED`, `PERSON_DETECTED`, `CAMERA_ONLINE`, `CAMERA_OFFLINE`, `RECORDING_STARTED`, `RECORDING_STOPPED`, `TAMPER_DETECTED`, and `UNKNOWN`.

`PERSON_DETECTED` is accepted only when the source camera or another verified producer explicitly reports it. Confidence is optional, bounded from 0 through 1, and never generated as a fallback. Metadata has strict size, depth, and allowed-value limits.

## 11. Security Controls

- No camera secrets in backend, Room, logs, analytics, or crash messages.
- Log sanitization removes URI user info and sensitive query/path content.
- Backend request models exclude network targets, preventing server-side camera SSRF by construction.
- Firebase-derived ownership protects every record and event.
- Payload lengths, ports, enums, timestamps, confidence, and metadata are validated.
- Discovery and connection work is bounded by timeouts and cancellation.
- Rate limiting is applied through an existing supported middleware or a narrowly added backend mechanism; no in-memory production claim is made if multi-instance enforcement is unavailable.
- The backend uses HTTPS in production through Render.

## 12. Error and Availability Contract

Android distinguishes empty discovery from failure. Saved cameras retain their last verified status with a visible verification timestamp; stale status is not presented as current. Backend synchronization failure does not fabricate cloud registration and does not expose credentials.

Capabilities and flows are classified as `REAL`, `NO VERIFICADA`, `NO CONFIGURADA`, or `SIMULADA`. Any existing simulated camera path is removed or replaced with an explicit unavailable state.

## 13. Testing Strategy

### Android unit tests

- WS-Discovery and ONVIF XML parsing, including malformed XML and XXE rejection.
- Deduplication, capability mapping, authentication-required mapping, and empty discovery.
- RTSP input validation and URI/log redaction.
- Repository state, encrypted-secret boundary, ViewModel transitions, and local-only messaging.
- Room migration preserving existing records.

### Backend tests

- Camera CRUD with verified Firebase UID injection.
- Cross-user read, update, delete, and event rejection.
- Payload validation, event normalization, confidence bounds, metadata limits, and URL-field rejection.
- Database cascade behavior and response redaction.

### Build and integration evidence

- Focused tests first, followed by the Android unit suite and `assembleDebug`.
- Backend pytest/compile checks for the actual FastAPI deployment surface.
- Physical Samsung install and navigation through the existing route by Wireless debugging.
- Real ONVIF discovery only when authorized compatible hardware is present.
- Real RTSP connection/live view only when an authorized stream is available.
- Missing hardware remains `NO VERIFICADA E2E`; it is never replaced with a mock.

## 14. Delivery Order

1. Truthful domain contracts and failing tests.
2. Backend models, ownership service, routes, and tests.
3. Room entity/DAO/migration and encrypted credential store.
4. ONVIF discovery and device/capability client.
5. RTSP validation, adapter, Media3 provider, and tests.
6. Repository and authenticated backend synchronization.
7. Compose screens and navigation within the camera route.
8. Full module validation, build, physical-device evidence, and simulation audit.

No commit, push, deploy, purchase, or publication occurs without explicit authorization.
