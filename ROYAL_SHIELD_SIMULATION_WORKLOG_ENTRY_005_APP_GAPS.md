# Royal Shield — Registro de trabajo, entrada 005: faltantes actuales de la app

**Fecha/hora:** 2026-07-30, America/New_York

## Alcance

Consolidación read-only del estado de Android, backend desplegado, Forecast
Engine, VPN y funciones simuladas. Se ejecutó compilación Kotlin; no se
modificó código fuente, infraestructura ni datos.

## Estado comprobado

- `:app:compileDebugKotlin` finalizó correctamente.
- Render y PostgreSQL están disponibles y el backend abre la conexión.
- Producción despliega `royalshieldapp/server-beckend` rama `main`, API v1.0.0,
  no el Unified API v2 del repositorio local.
- Auto-Deploy está desactivado.
- PostgreSQL Free indica eliminación el 11 de agosto de 2026 si no se actualiza.
- El repositorio contiene `AssistantRepository` y `AssistantViewModel`, pero
  `DashboardScreen` solo referencia `DidAgentDialog`; el chat de texto no está
  conectado de forma visible.

## Simulaciones confirmadas en fuente actual

- `DashboardViewModel`: severidad, puntos y timeline aleatorios.
- `XdrViewModel`: tráfico, investigaciones y alertas aleatorias.
- `IrRepository`: inicializa incidentes, timeline y checklist mock.
- `VirtualAgentDialog`: respuesta hardcodeada de integridad 100%.
- `PolicyManagerScreen`: políticas de ejemplo.
- `risk_maps.py`: eventos vacíos y estructuras mock.
- `hotspots.py`: eventos vacíos y predicción “coming soon”.
- `predictions.py`: factores mock y forecasting “coming soon”.

## Faltantes E2E principales

1. Proteger o migrar PostgreSQL antes de su fecha de eliminación.
2. Definir el repositorio backend oficial y reconciliar v1 desplegada con v2.
3. Cargar datos históricos durables, entrenar Forecast Engine v2 y persistir el
   modelo fuera del filesystem efímero.
4. Sustituir mapas, hotspots y predicciones vacías por consultas/modelo reales.
5. Retirar datos aleatorios y mocks de Dashboard, XDR e Incident Response.
6. Conectar el asistente autenticado al Dashboard y eliminar respuestas falsas.
7. Completar VPN con prueba de consentimiento, interfaz tun, handshake, bytes y
   cambio de ruta/IP.
8. Validar scanners, tracking, business, billing, marketplace, políticas,
   reportes y amenazas contra servicios reales, sin éxito falso.
9. Cerrar seguridad/Play: permisos sensibles, logs con PII, credenciales
   restringidas, reglas de datos y justificaciones de consola.
10. Ejecutar pruebas, lint, assemble, instalación y E2E en dispositivo antes de
    producir un nuevo AAB.

## Validación

Comando:

`.\gradlew.bat :app:compileDebugKotlin --console=plain`

Resultado: exit code 0. Se observó una advertencia de diferencia de versión XML
entre Android Studio y command-line tools; no bloqueó la compilación.
