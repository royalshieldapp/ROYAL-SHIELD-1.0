# Royal Shield - Simulated Functions Checklist

Estado:
- `[ ]` pendiente
- `[!]` requiere credencial, infraestructura o decision de producto antes de cerrar
- `->` hecho o descartado de la lista activa

## Pendientes activos

- [!] VPN ya no usa config mock en Android, pero requiere infraestructura WireGuard real.
  - Archivos: `app/src/main/java/com/royalshield/app/vpn/VpnProfileRepository.kt`, `backend/routes/vpn.js`, `app/src/main/java/com/royalshield/app/ui/screens/VpnScreen.kt`.
  - Cambio parcial: Android consulta `/api/vpn/status`, `/api/vpn/servers` y `/api/vpn/config`; el servicio usa `GoBackend` de WireGuard; backend responde `not_configured` o `peer_registration_not_configured` en vez de simular.
  - Pendiente: configurar servidor WireGuard real y flujo de registro de peers antes de marcarlo hecho.

- [!] Phone number checker cae a resultados mock cuando no hay `NUMVERIFY_API_KEY`.
  - Archivos: `backend/routes/phone.js`, `app/src/main/java/com/royalshield/app/managers/ReputationManager.kt`.
  - Meta: configurar proveedor real o devolver `not_configured` sin inventar carrier/location.

- [ ] Loyalty usa variables en memoria como "mock database".
  - Archivos: `backend/routes/loyalty.js`, `app/src/main/java/com/royalshield/app/data/LoyaltyRepository.kt`.
  - Meta: persistir por usuario en base de datos o storage durable.

- [ ] Threat map/risk endpoints devuelven datos dummy o `mock-h3`.
  - Archivos: `backend/routes/threats.js`, `backend/routes/risk.js`, `royal_shield_backend_unified/api/routes/risk_maps.py`, `royal_shield_backend_unified/api/routes/hotspots.py`.
  - Meta: conectar a fuentes reales o a eventos reales almacenados.

- [ ] Business lead solo responde al cliente y tiene TODO de email/CRM.
  - Archivos: `backend/routes/business.js`, `royal_shield_backend_unified/api/routes/business.py`, `app/src/main/java/com/royalshield/app/data/BusinessRepository.kt`.
  - Meta: guardar lead y notificar por proveedor configurado.

- [ ] Billing/entitlements tiene TODO de consulta real.
  - Archivos: `backend/routes/billing.js`, `royal_shield_backend_unified/api/routes/billing.py`, `app/src/main/java/com/royalshield/app/billing/BillingRepository.kt`.
  - Meta: validar compras contra Google Play/backend y cachear entitlement seguro.

- [ ] Policy Manager inicia con `getSamplePolicies()`.
  - Archivo: `app/src/main/java/com/royalshield/app/ui/screens/PolicyManagerScreen.kt`.
  - Meta: persistir politicas creadas y aplicar reglas reales.

- [ ] Reports Center inicia con `loadSampleReports()`.
  - Archivo: `app/src/main/java/com/royalshield/app/ui/screens/ReportsCenterScreen.kt`.
  - Meta: generar reportes desde scans/eventos reales.

- [ ] Speed test muestra valores random.
  - Archivo: `app/src/main/java/com/royalshield/app/ui/components/SpeedTestDialog.kt`.
  - Meta: medir latencia/descarga/subida reales o renombrarlo como diagnostico estimado.

- [ ] Voice scam screen incrementa amenazas con random.
  - Archivo: `app/src/main/java/com/royalshield/app/ui/screens/VoiceScamScreen.kt`.
  - Meta: alimentar desde `VoiceScamDetectionService` o logs reales.

- [!] Seguridad alta: token Mapbox hardcodeado en `settings.gradle.kts`.
  - Meta: mover a `local.properties`/variable de entorno y rotar el token expuesto.

- [!] Config/API faltante al migrar desde otros Royal Shield.
  - Hallazgo: este checkout solo tenia `sdk.dir` en `local.properties`; faltaban nombres de claves Android usadas por Gradle.
  - Hallazgo: faltaban variables OpenClaw email/Whisper en `backend/.env.example`.
  - Hallazgo audit: AdMob seguia con ID de prueba hardcodeado en manifest.
  - Cambio parcial: agregado `local.properties.example`, documentadas variables backend faltantes sin secretos reales y `ADMOB_APP_ID` ahora entra por placeholder.

## Hechas / descartadas

-> GPS tracking Android enviaba a `https://your-api.com/location`.
  - Cambio: conectado a `BuildConfig.API_BASE_URL + /api/location/track` y agregado endpoint backend con validacion.
  - Validado: POST `/api/location/track`, bloqueo de `/api/location/recent` sin secreto y lectura con `X-Internal-Secret`.
