# Royal Shield — Registro de trabajo 009

Fecha: 2026-07-31 (America/New_York)

## Alcance

Conectar el chatbot de texto del Dashboard al flujo autenticado existente sin modificar el asistente de voz ni rediseñar la pantalla.

## Evidencia inicial

| Función | Android | Backend | Fuente/credencial | Almacenamiento | Prueba API | Prueba dispositivo | Estado inicial |
|---|---|---|---|---|---|---|---|
| Chatbot de texto | `AssistantRepository` y `AssistantViewModel` existen; `VirtualAgentDialog` está desconectado | `POST /api/assistant/chat` identificado en fuente previa | Firebase ID token | Historial en memoria, acotado | No verificada hoy | No verificada hoy | SIMULADA en UI / NO VERIFICADA E2E |

## Simulación confirmada

`VirtualAgentDialog.kt` genera respuestas locales con `delay(1000)` y afirma `System integrity 100%`. Ese comportamiento debe eliminarse, no exponerse como capacidad real.

## Límites

- Sin commit, push o deploy.
- Preservar cambios locales previos de Forecast y Solution Engine.
- Compilación o `/health` no se considerarán evidencia E2E.

## Evidencia de implementación y validación

### Implementación

- `VirtualAgentDialog` consume `AssistantViewModel.uiState` y delega los envíos a `AssistantViewModel.sendMessage`.
- Se eliminaron `AgentMessage`, `delay(1000)`, `onSendMessage`, `getAgentResponse`, el saludo "AI Online" y la afirmación falsa "System integrity 100%".
- Los errores reales se representan como mensajes `ERROR`; no se sustituyen por respuestas locales.
- El Dashboard incluye un control de chat de texto junto al micrófono sin modificar `DidAgentDialog`.
- En producción, `AssistantViewModel` conserva `AssistantRepository.sendMessage` como emisor predeterminado.

### Pruebas y comandos

| Evidencia | Resultado |
|---|---|
| Prueba RED enfocada | Falló porque `AssistantViewModel` no admitía `sender`; confirmó el límite de comprobabilidad. |
| Prueba enfocada del ViewModel | `BUILD SUCCESSFUL`; 2 pruebas de resultado, error, normalización y bloqueo de duplicados. |
| `gradlew.bat :app:compileDebugKotlin` | `BUILD SUCCESSFUL`. |
| `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` | `BUILD SUCCESSFUL`; APK debug generado. |
| `gradlew.bat :app:lintDebug` | Falló por deuda global previa: 18 errores y 865 warnings. Ningún error corresponde a los archivos del chatbot. |
| `git diff --check` | Sin errores de whitespace; solo avisos LF/CRLF. |
| Búsqueda de simulaciones retiradas | Sin coincidencias en diálogo, Dashboard o ViewModel. |

### Estado E2E

- Fuente, pruebas y APK: verificados.
- Instalación en Samsung `SM-F956U1`: verificada mediante `adb install -r` con resultado `Success`; proceso `com.royalshield.app` y `MainActivity` activos.
- Consulta autenticada en dispositivo: `NO VERIFICADA`; el teléfono quedó bloqueado con `NotificationShade`/Always On sobre la aplicación.
- Backend/Gemini en producción: `NO VERIFICADO`. Si devuelve `AI_NOT_CONFIGURED`, se debe configurar `GEMINI_API_KEY` en el servicio Render correcto sin exponerla.

## Continuación de verificación — 2026-08-01

| Evidencia | Resultado |
|---|---|
| `gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.viewmodels.AssistantViewModelTest" --tests "com.royalshield.app.ui.screens.SettingsLegalAssetsTest" :app:assembleDebug` | `BUILD SUCCESSFUL` en 11 s. |
| `git diff --check` | Exit code 0; sin errores de whitespace, solo avisos LF/CRLF. |
| `adb -s RFCXC0DGQ4Z install -r app\\build\\outputs\\apk\\debug\\app-debug.apk` | `Success`; APK actual reinstalado en Samsung `SM-F956U1`. |
| Estado visible tras abrir la app | La app mostró Registration sin sesión autenticada; posteriormente el usuario mantuvo Galería en primer plano. |
| Chat autenticado y visor legal | `NO VERIFICADOS E2E`: requieren inicio de sesión manual y navegación del usuario; no se solicitaron ni manipularon credenciales. |
| Compatibilidad Android 16 KB | `BLOQUEADOR DE PUBLICACIÓN`: el sistema reportó librerías nativas no alineadas, incluidas `libwg-go.so`, `libwg.so`, `libwg-quick.so`, `libdatastore_shared_counter.so`, `libimage_processing_util_jni.so`, `libbarhopper_v3.so` y `libandroidx.graphics.path.so`. |

### Checklist pendiente para cierre E2E

- [ ] El usuario inicia sesión manualmente en el Samsung y deja Royal Shield en el Dashboard.
- [ ] Enviar un mensaje desde el botón de chat de texto y observar respuesta real o error honesto del backend.
- [ ] Abrir Settings → Privacy Policy y Settings → Terms & Conditions y comprobar contenido, scroll, cierre y enlaces.
- [ ] Capturar evidencia no sensible de logcat/red para la solicitud autenticada y registrar el estado real de Gemini/Render.
- [ ] Corregir y volver a verificar compatibilidad de páginas de memoria de 16 KB antes de publicar.
