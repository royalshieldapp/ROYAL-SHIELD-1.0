# Royal Shield - Registro de cambios locales

Fecha de auditoria: 2026-07-22  
Alcance: estado del directorio de trabajo comparado con `HEAD`. Este documento
describe cambios detectados por Git; no atribuye autoria ni confirma que hayan
sido publicados en Render, Firebase o Google Play.

## Resumen

- 24 archivos modificados rastreados por Git.
- 7 rutas nuevas sin rastrear.
- Cambio neto rastreado: 3,291 inserciones y 440 eliminaciones.
- El APK debug no compila actualmente: `VoiceScamDetectionService.kt` usa
  `PreferencesManager` sin el import correspondiente.
- Render despliega `royal_shield_backend_unified` (FastAPI), no `backend/`
  (Node.js). Los cambios de Node requieren un despliegue independiente para
  afectar a usuarios.

## Android

| Archivo | Cambio local detectado | Estado / observacion |
| --- | --- | --- |
| `app/src/main/AndroidManifest.xml` | Servicio VPN declarado como foreground service `specialUse`. | Requiere justificacion Play Console para el tipo de servicio. |
| `app/src/main/java/com/royalshield/app/vpn/RoyalVpnService.kt` | Ajustes de integracion WireGuard. | Pendiente prueba de tunel real. |
| `app/src/main/java/com/royalshield/app/vpn/VpnProfileRepository.kt` | Configuracion VPN obtenida desde backend y uso de identidad/clave local. | Pendiente prueba Android a VPN cloud. |
| `app/src/main/java/com/royalshield/app/vpn/VpnKeyStore.kt` | Archivo nuevo para claves WireGuard y client ID local. | Debe validarse dentro de un APK instalado. |
| `app/src/main/java/com/royalshield/app/ui/screens/VpnScreen.kt` | Adaptacion de UI al estado de VPN configurada/no configurada. | Depende de control plane y peer real. |
| `app/src/main/java/com/royalshield/app/data/UserProfileRepository.kt` | Ajustes para Profile/Firebase. | Requiere prueba E2E con APK fresco. |
| `app/src/main/java/com/royalshield/app/features/trackingshield/data/FamilyRepository.kt` | Cambios de Family Tracking, pairing y datos asociados. | Requiere prueba E2E con dos cuentas/dispositivos. |
| `app/src/main/java/com/royalshield/app/data/LoyaltyRepository.kt` | Cliente de Loyalty contra endpoint backend. | No debe confiar en UID ni puntos entregados por cliente. |
| `app/src/main/java/com/royalshield/app/managers/PreferencesManager.kt` | Nuevas preferencias locales, incluidas metricas de Voice Scam. | No es fuente de verdad para datos economicos o puntos. |
| `app/src/main/java/com/royalshield/app/services/VoiceScamDetectionService.kt` | Registro de llamadas/amenazas. | Bloquea build por import faltante de `PreferencesManager`. |
| `app/src/main/java/com/royalshield/app/FileScanActivity.kt` | Cambios del flujo de escaneo. | Validar con proveedor real y sin resultados mock. |
| `app/src/main/java/com/royalshield/app/ui/screens/FileScanScreen.kt` | Cambios de UI de File Scan. | Validar resultado y estados de error. |
| `app/src/main/java/com/royalshield/app/ui/screens/ReferralScreen.kt` | Cambios de referral. | Pendiente backend/dynamic links reales. |
| `app/src/main/java/com/royalshield/app/ui/screens/RoyalMapScreen.kt` | Ajustes del mapa. | Validar datos reales y permisos. |
| `app/src/main/java/com/royalshield/app/ui/screens/VoiceScamScreen.kt` | Ajustes de interfaz Voice Scam. | La deteccion aun necesita evidencia real. |
| `app/src/main/java/com/royalshield/app/ui/components/BottomBar.kt` | Ajustes visuales/navegacion inferior. | Sin validacion visual reciente. |
| `app/src/main/res/drawable-nodpi/` | Recursos nuevos sin rastrear. | Revisar licencias, tamanos y uso. |
| `app/src/main/res/drawable/bg_threat_detected.png` | Recurso visual nuevo sin rastrear. | Revisar antes de empaquetar. |

## Backend Node.js (`backend/`)

| Archivo | Cambio local detectado | Estado / observacion |
| --- | --- | --- |
| `backend/routes/loyalty.js` | Loyalty migrado de memoria a Firestore. | Riesgo: acepta `userId` y puntos enviados por cliente; requiere Firebase ID token, reglas de acciones y ledger servidor. |
| `backend/config/` | Configuracion Firebase nueva sin rastrear. | Confirmar que no contenga secretos y que este cubierto por `.gitignore` si aplica. |
| `backend/package.json` y `backend/package-lock.json` | Dependencia `firebase-admin`. | Sintaxis Node validada; falta prueba con credenciales reales. |
| `backend/.env.example` | Variables adicionales de VPN/OpenClaw. | Mantener valores de ejemplo, nunca secretos reales. |

## Backend FastAPI y despliegue Render

| Archivo | Cambio local detectado | Estado / observacion |
| --- | --- | --- |
| `royal_shield_backend_unified/api/routes/vpn.py` | Registro de peers por control plane con firma HMAC y validacion de clave publica. | El backend publico reporto VPN disponible; falta prueba completa de peer/tunel. |
| `royal_shield_backend_unified/config/settings.py` | Variables de control plane VPN. | Secretos deben configurarse en Render, no en Git. |
| `render.yaml` | Variables `VPN_CONTROL_URL` y `VPN_CONTROL_SECRET`. | Requieren valores secretos en Render. |
| `infra/` | Infraestructura nueva sin rastrear para agente WireGuard. | Revisar y versionar antes de despliegue. |

## Firebase y configuracion

| Archivo | Cambio local detectado | Estado / observacion |
| --- | --- | --- |
| `database.rules.json` | Reglas de Profile, pairing codes, children, safe zones y risk zones. | Las reglas se publicaron previamente, pero Profile/Family debe probarse con APK actual. |
| `SIMULATED_FUNCTIONS_CHECKLIST.md` | Loyalty y Voice Scam marcados como trabajo realizado. | Mantenerlo alineado con pruebas reales, no solo con codigo. |

## Archivos y carpetas locales adicionales

- `.idea/`: configuracion de Android Studio; normalmente no se versiona completa.
- `com/`: ruta nueva sin rastrear; inspeccionar contenido antes de agregarla.
- `infra/`: scripts/configuracion de infraestructura WireGuard; revisar secretos y documentacion.

## Validaciones ejecutadas durante la auditoria

- `node --check` en 18 archivos del backend Node: correcto.
- `python -m compileall -q api config services scripts`: correcto.
- `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest`: fallo por referencia no resuelta a `PreferencesManager` en `VoiceScamDetectionService.kt`.
- `git diff --check`: detecta espacios finales en `backend/routes/loyalty.js`.
- Backend Render: `/health` respondio `200 healthy` en la auditoria; verificar de nuevo antes de una publicacion.

## Bloqueadores prioritarios

1. Corregir el build Android.
2. Asegurar Loyalty: autenticacion Firebase, ledger persistente y acciones otorgadas solo por servidor.
3. Probar VPN de extremo a extremo desde Android.
4. Reemplazar datos mock de riesgo, hotspots y predicciones por datos persistidos/modelos reales.
5. Mover credenciales sensibles fuera del APK y completar Billing server-side.

## Cambios solicitados pero no aplicados en esta sesion

La integracion del banner AdMob (barra inferior y Cyber) fue autorizada, pero
no se aplico porque el mecanismo de edicion del workspace fallo antes de
escribir archivos. Los IDs recibidos no se registran en este documento para
evitar duplicar configuracion de entorno.
