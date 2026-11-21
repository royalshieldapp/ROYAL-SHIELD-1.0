# Forecast Engine v2 — Diseño de producción

## Propósito y límite

Esta especificación define el primer proyecto de la eliminación total de funciones simuladas de Royal Shield: Forecast Engine v2. Su objetivo es entregar predicciones espaciales y de ciberseguridad sustentadas en datos reales, persistidos, trazables y verificables de extremo a extremo.

No cubre la corrección de todas las demás simulaciones de Royal Shield. Esas se abordarán como proyectos posteriores con su propio inventario, diseño y plan, para no mezclar cambios de alto riesgo en una sola entrega.

## Estado comprobado al inicio

- El trabajo de Forecast Engine v2 existe en GitHub en la rama `royalshield-v1.0.2-unified`, incluido el commit `3d5ea407`.
- Esa rama no está fusionada con `master`.
- Render despliega el repositorio separado `royalshieldapp/server-beckend`, commit `1fe0e2a`, y no hay evidencia de que tenga los endpoints de Forecast Engine v2.
- La app Android compila, pero compilar no demuestra una predicción real.
- Los modelos locales no son aptos para producción si sus artefactos quedan en el filesystem efímero de Render.

## Arquitectura objetivo

`royalshieldapp/server-beckend` será el backend de producción. Se incorporarán allí, de forma selectiva y revisable, las piezas reutilizables de `royal_shield_backend_unified`.

```text
Fuentes oficiales -> colectores aislados -> validación/normalización
  -> PostgreSQL versionado -> features temporales sin leakage
  -> entrenamiento y artefacto persistente -> API autenticada
  -> Android (estado y procedencia visibles)
```

Cada colector, repositorio, servicio de modelo y ruta tendrá una sola responsabilidad. Android seguirá siendo consumidor de API y no ejecutará entrenamiento ni guardará secretos de proveedores.

## Datos y procedencia

| Dominio | Fuentes aceptadas | Uso |
|---|---|---|
| Espacial/físico | Miami-Dade, NASA FIRMS, NWS/NOAA, USGS, OpenStreetMap | Eventos y contexto geolocalizado para riesgo espacial. |
| Histórico regional | FBI Crime Data Explorer | Priors e históricos agregados; nunca coordenadas puntuales inventadas. |
| Cyber | CISA KEV, NVD, OTX, AbuseIPDB, VirusTotal | Riesgo digital separado del modelo espacial. |
| Noticias/OSINT | Proveedores autorizados posteriormente | Solo tras geocodificación real, auditable y con confianza suficiente. |

Los datos se guardarán en PostgreSQL mediante migraciones versionadas. Cada registro incluirá fuente, identificador externo, fecha de origen, fecha de ingesta, coordenadas validadas cuando correspondan, nivel de precisión y metadatos mínimos de procedencia. Las claves, tokens y cargas sensibles no se almacenarán en logs ni en la base de incidencias.

## Entrenamiento y artefactos

- Las features solo podrán usar eventos anteriores al corte temporal; la ventana futura será exclusivamente el objetivo.
- La separación entrenamiento/validación será cronológica.
- Un entrenamiento solo se ejecutará con volumen mínimo configurable de datos durables y calidad validada. Si no se alcanza, terminará sin modelo nuevo y con un estado explícito.
- Métricas mínimas: tamaño de muestra, periodo, MAE, RMSE y R² cuando sea matemáticamente aplicable.
- Todo modelo aprobado llevará versión, fecha, conjunto de fuentes, métricas y checksum de artefacto.
- Para el primer modelo, los artefactos XGBoost JSON de hasta 10 MiB se guardarán en PostgreSQL junto con versión, métricas, periodo, estado activo y SHA-256. Este límite protege la base de datos de blobs grandes.
- Si un artefacto supera 10 MiB, el entrenamiento no lo promoverá: se migrará a almacenamiento persistente externo y PostgreSQL conservará únicamente URI, versión, métricas y checksum.
- Un artefacto local temporal nunca puede declararse desplegado.

## Contrato API y estados

Las respuestas útiles incluirán `status`, `generatedAt`, `dataFreshness`, `sources`, `modelVersion` cuando haya modelo, y `confidence` cuando sea calculable.

| Estado | Semántica | Respuesta |
|---|---|---|
| `READY` | Resultado sustentado por datos y modelo o regla real vigente. | HTTP 200 con procedencia y vigencia. |
| `DEGRADED` | Resultado real pero con fuentes parciales o retrasadas dentro de un límite permitido. | HTTP 200 con causas y fuentes ausentes. |
| `UNAVAILABLE` | No hay datos suficientes, modelo válido o proveedor crítico. | HTTP 503 sin valor de riesgo inventado. |
| Entrada inválida | Parámetros fuera de contrato. | HTTP 422, mensaje seguro y accionable. |

Los endpoints que hoy devuelvan datos de muestra, aleatorios o éxito ficticio se sustituirán por una respuesta `UNAVAILABLE` hasta que su cadena real esté operativa.

## Android

La interfaz mostrará el estado de disponibilidad, hora de actualización y procedencia resumida. No mostrará un puntaje, hotspot o predicción cuando el backend responda `UNAVAILABLE`. En `DEGRADED`, mostrará el resultado real acompañado de una advertencia visible de cobertura parcial.

Los ViewModels traducirán el contrato a estados de UI. Repositories manejarán autenticación, errores de red y contratos tipados. No se añadirán tokens de proveedores al APK.

## Incidencias y trazabilidad

Se verificará la capacidad persistente existente antes de crear una migración. Si falta, la propuesta posterior deberá registrar al menos: identificador, fecha, módulo, archivo o componente afectado, severidad, estado, evidencia no sensible, versión/despliegue y resolución.

La bitácora de trabajo del repositorio conserva hallazgos y validaciones. La tabla de incidencias solo se implementará tras confirmar el esquema real del backend de Render para evitar crear modelos paralelos incompatibles.

## Seguridad y operación

- Validar parámetros geográficos, rangos temporales y paginación.
- Aplicar límites de tasa a rutas costosas o que consuman APIs de terceros.
- Mantener secretos exclusivamente en variables de entorno configuradas en Render.
- Sanitizar errores y logs.
- Restringir la conectividad de PostgreSQL a los orígenes necesarios; la regla abierta a todo Internet detectada en Render requiere corrección separada y confirmación antes de cambiarla.
- Usar tareas programadas con registro de éxito/fallo, conteos reales de inserción/actualización/rechazo y alertas ante datos obsoletos.

## Estrategia de integración y despliegue

1. Obtener una copia verificable del repositorio de Render y crear una matriz archivo por archivo contra Forecast Engine v2.
2. Portar los componentes necesarios en cambios pequeños y cubiertos por pruebas.
3. Añadir migraciones revisables y validar una base de datos no productiva.
4. Configurar variables no secretas en `.env.example` y secretos solo en Render.
5. Ejecutar colectores con fuentes reales, verificar filas persistidas y entrenar solo si se alcanza el mínimo.
6. Publicar el artefacto persistente, probar el endpoint y comprobar que Android refleja los tres estados.
7. Desplegar manualmente en Render y repetir la verificación desde la URL pública.

## Decisión de finalización 2026-08-25

- El alcance aprobado es producción E2E, no solo validación local.
- La implementación continuará en `C:\tmp\royal-shield-server-backend-forecast`, basada en `royalshieldapp/server-beckend`, para aislarla del árbol Android con cambios locales.
- Render debe desplegar `royalshieldapp/server-beckend`, rama `main`. El servicio público comprobado todavía expone la API legacy 1.0.0: no tiene `/api/v1/system/status` ni la ruta Forecast v2 `/api/v1/predict/risk`, y `/api/v1/hotspots` devuelve una colección vacía sin contrato de disponibilidad.
- La primera cadena verificable será Miami-Dade durable -> PostgreSQL -> entrenamiento cronológico -> artefacto PostgreSQL -> API -> Android/Samsung.
- Commit, push, migración de producción y deploy requieren autorización explícita justo antes de ejecutarse.

No se hará merge, push ni despliegue de un cambio concreto sin verificación proporcional y sin conservar los cambios locales existentes del usuario.

## Criterios de aceptación

Una capacidad de Forecast se considerará terminada únicamente si cumple todos los puntos:

1. La fuente real responde o su fallo está demostrado y manejado.
2. Los registros reales validados existen en PostgreSQL y pueden contarse sin depender de contadores simulados.
3. El modelo, si aplica, fue entrenado con datos durables y tiene métricas/versionado almacenados.
4. El endpoint de producción entrega `READY`, `DEGRADED` o `UNAVAILABLE` coherente, sin datos inventados.
5. Android presenta el mismo estado, procedencia y fecha de actualización.
6. Pruebas automatizadas y una prueba manual E2E documentan el resultado.
7. La bitácora de trabajo incluye comando, fecha, evidencia y cualquier bloqueo restante.

## Fuera de alcance inmediato

- Resolver simultáneamente módulos no relacionados (VPN, pagos, chatbot, perfiles, automatizaciones sociales, etc.).
- Inventar fuentes, coordenadas, credenciales, endpoints o esquemas de base de datos.
- Declarar producción por una compilación local o un `/health` exitoso.
