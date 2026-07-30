# Royal Shield — Registro de eliminación de funciones simuladas

Este documento es el registro continuo de la auditoría y conversión de funciones
simuladas de Royal Shield. Debe actualizarse conforme avance el trabajo.

Reglas del registro:

- Registrar fecha y hora con zona horaria.
- Distinguir código presente, validación local y prueba real de extremo a extremo.
- No considerar una función terminada por compilar o devolver HTTP 200.
- No registrar claves, tokens, credenciales, PII ni otros datos sensibles.
- No marcar como real una ruta con datos aleatorios, de muestra, hardcodeados,
  persistencia solo local, éxito falso o dependencias externas sin verificar.

## Estado general

- Auditoría activa.
- Checkout: `ROYAL_SHIELD_1.0.2_UNIFIED_PUBLISH`.
- Rama observada: `royalshield-v1.0.2-unified`.
- El árbol ya contenía cambios locales ajenos a esta auditoría; deben preservarse.
- No se ha autorizado commit, push ni deploy.

---

## Entrada 001 — Inventario inicial y Forecast Engine v2

**Fecha/hora:** 2026-07-29 21:20:04 -04:00  
**Tipo:** auditoría de solo lectura y creación del registro  
**Resultado:** parcial; se confirmó infraestructura de entrenamiento, pero no un
modelo real entrenado y desplegado.

### Hallazgos

1. El repositorio contiene tres superficies que deben mantenerse claramente
   separadas durante la implementación:
   - Android/Kotlin/Jetpack Compose en `app/`.
   - Backend Node/Express en `backend/`.
   - Backend FastAPI desplegable en `royal_shield_backend_unified/`.
2. `render.yaml` apunta al backend FastAPI, no al backend Node.
3. Forecast Engine v2 dispone de componentes reales para:
   - cargar eventos y POI persistidos;
   - crear muestras temporales evitando data leakage;
   - separar entrenamiento y validación cronológicamente;
   - entrenar XGBoost;
   - calcular R², MAE y RMSE;
   - guardar modelos y métricas.
4. No se encontró en el checkout ningún artefacto local de modelo o métricas:
   - `models/risk_predictor_latest.pkl`;
   - `models/cyber_forecast_latest.pkl`;
   - archivo `*.metrics.json`.
5. Tampoco se encontró una base de datos local con datos durables para repetir el
   entrenamiento desde este checkout.
6. Los endpoints de predicción aún no consumen el pipeline real de extremo a
   extremo: varias rutas suministran listas vacías al modelo, valores fijos o
   respuestas explícitamente mock.
7. La comprobación pública no permitió confirmar un modelo desplegado:
   - `/health` agotó el tiempo de lectura;
   - `/api/v1/system/status` respondió HTTP 404.

### Funciones detectadas como simuladas o incompletas

| Prioridad | Función | Evidencia actual | Estado |
| --- | --- | --- | --- |
| Alta | Forecast/risk prediction | `predictions.py` usa `current_events = []`, `current_pois = []`, confianza fija y factores mock | Pendiente |
| Alta | Risk maps | `risk_maps.py` contiene consultas de base de datos pendientes y estructuras mock | Pendiente |
| Alta | Hotspots | `hotspots.py` usa eventos vacíos y predicción pendiente | Pendiente |
| Alta | Threat intelligence | `threats.py` devuelve datos mock en lugar de proveedor/base interna | Pendiente |
| Alta | Breach monitor | `BreachCheckManager.kt` usa retrasos, coincidencias de texto y aleatoriedad | Pendiente |
| Alta | XDR telemetry | `XdrViewModel.kt` añade tráfico y contadores aleatorios | Pendiente |
| Alta | Incident Response | `IrRepository.kt` inicializa `mock_incident_1` y `mock_incident_2` | Pendiente |
| Media | Global incidents | `GlobalIncidentRepository.kt` devuelve incidentes y recordatorios fijos | Pendiente |
| Alta | Business lead | `BusinessRepository.kt` devuelve éxito al fallar la red; backend no persiste/notifica el lead | Pendiente |
| Alta | Billing entitlement | `billing.py` devuelve tier gratuito por defecto sin consulta durable | Pendiente |
| Media | Policy Manager | `PolicyManagerScreen.kt` inicia con `getSamplePolicies()` | Pendiente |
| Media | Reports Center | `ReportsCenterScreen.kt` inicia con `loadSampleReports()` y agrega archivos fallback | Pendiente |
| Alta | Marketplace | `MarketplaceScreen.kt` usa catálogo `mockItems` y estado cliente | Pendiente |

### Cambios realizados

- Creado `ROYAL_SHIELD_SIMULATION_WORKLOG.md`.
- No se modificó código Android ni backend en esta entrada.
- No se tocaron los cambios locales existentes en autenticación o Assistant.

### Validaciones ejecutadas

| Validación | Resultado real |
| --- | --- |
| `git status --short --branch` | Rama un commit por delante de `origin`; cuatro rutas locales preexistentes detectadas |
| Búsqueda de marcadores `mock`, `simulated`, `sample`, `Random`, `TODO` | Confirmó simulaciones en Android, Node y FastAPI |
| Revisión de `train_forecast_model.py`, `training_data.py`, `feature_engineering.py` y `risk_predictor.py` | Pipeline de entrenamiento presente |
| Búsqueda de modelos, métricas y bases locales | No se encontraron artefactos entrenados ni base durable local |
| Comprobación HTTPS del backend público | `/health`: timeout; `/api/v1/system/status`: HTTP 404 |

### Bloqueos y riesgos

- Falta confirmar la base PostgreSQL real y contar eventos/POI por fuente y rango
  temporal.
- Un entrenamiento sintético anterior no demuestra calidad ni disponibilidad
  productiva.
- El filesystem normal de Render es efímero; los modelos necesitan disco
  persistente u object storage.
- No se debe activar inferencia hasta validar volumen, cobertura temporal,
  duplicados, coordenadas, métricas y carga del modelo al iniciar FastAPI.
- Cualquier proveedor externo o cambio de infraestructura requerirá configuración
  segura y autorización específica antes de deploy.

### Próximo trabajo

1. Obtener evidencia de la base PostgreSQL configurada sin exponer su URL.
2. Añadir una auditoría segura de conteos y cobertura de `forecast_events`,
   `forecast_pois`, `threat_map_events` y observaciones cyber.
3. Ejecutar entrenamiento real solo cuando se alcance el mínimo de datos.
4. Persistir el artefacto y sus métricas fuera del filesystem efímero.
5. Sustituir los datos vacíos/mock de las rutas por loaders durables y respuestas
   de error explícitas cuando el modelo no esté disponible.
