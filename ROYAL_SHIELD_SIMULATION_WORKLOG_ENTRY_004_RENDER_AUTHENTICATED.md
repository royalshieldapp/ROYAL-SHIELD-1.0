# Royal Shield — Registro de trabajo, entrada 004: auditoría autenticada de Render

**Fecha/hora:** 2026-07-29 22:32 -04:00

## Alcance

Inspección autenticada y read-only del proyecto, servicio web, configuración,
variables, logs y PostgreSQL en Render. No se modificaron servicios, variables,
planes, despliegues, datos ni credenciales.

## Hallazgos confirmados

- El proyecto de Render contiene dos recursos en `Production`:
  - `server-beckend`, Python 3, estado `Deployed`, plan Free, región Virginia.
  - `Royal Shield Postgress server`, PostgreSQL 18, estado `Available`, plan
    Free, región Virginia.
- El servicio web no está conectado al repositorio Android/unificado auditado
  localmente. Su fuente es el repositorio separado
  `royalshieldapp/server-beckend`, rama `main`.
- El despliegue activo usa el commit abreviado `1fe0e2a`, mensaje
  `Add secure WireGuard peer registration`, publicado el 19 de julio de 2026.
- El build usa `pip install -r requirements-render.txt`.
- El start usa `uvicorn api.main:app --host 0.0.0.0 --port $PORT`.
- El health check es `/health`.
- Auto-Deploy está desactivado. Los despliegues visibles fueron activados
  manualmente.
- `DATABASE_URL` y las variables `POSTGRES_*` aparecen definidas. No se leyó,
  copió ni registró ningún valor.
- Los logs de un arranque provocado mediante una consulta pública muestran:
  entorno production, inicialización de base satisfactoria, backend iniciado y
  startup completo.
- La raíz pública continúa identificándose como `Royal Shield Risk Prediction
  API` versión `1.0.0`.
- `/api/v1/system/status` continúa respondiendo HTTP 404; el backend desplegado
  no contiene el endpoint de diagnóstico del Unified API v2 auditado localmente.
- Shell/SSH no está disponible en el plan Free, por lo que no fue posible listar
  tablas o conteos mediante una consulta SQL read-only.
- La base PostgreSQL gratuita muestra fecha de eliminación el
  **11 de agosto de 2026** si no se actualiza a una instancia de pago.
- Existe una variable probablemente mal escrita:
  `TWLIO_PHONE_NUMBER`; el código y convenciones del proyecto esperan
  `TWILIO_PHONE_NUMBER`.

## Conclusión

Render y PostgreSQL están operativos y el backend logra inicializar la conexión.
Sin embargo, producción está desplegando un repositorio backend separado y una
API v1.0.0, no el Unified API v2 del repositorio local. Por ello los cambios
locales de Forecast Engine v2 y el endpoint de estado no llegan actualmente a
producción.

La existencia de una conexión exitosa no demuestra todavía las tablas, filas,
calidad de datos ni artefacto de modelo. La falta de Shell en Free bloquea esa
prueba desde el servicio.

## Riesgos y próximos pasos

1. Prioridad crítica: decidir antes del 11 de agosto si PostgreSQL se actualiza,
   respalda o migra; de lo contrario Render indica que será eliminado.
2. Decidir cuál repositorio será la fuente oficial del backend:
   `royalshieldapp/server-beckend` o el backend unificado del repositorio de la
   app.
3. Reconciliar el código y ejecutar pruebas locales antes de cualquier
   despliegue.
4. Corregir el nombre de la variable Twilio solo después de verificar qué nombre
   consume realmente el backend desplegado.
5. Añadir un diagnóstico seguro de esquema/conteos o habilitar temporalmente un
   método read-only autorizado para verificar tablas sin exponer credenciales.
6. Por precaución, regenerar el deploy hook privado en una operación separada y
   explícitamente autorizada; no se incluye su valor en este registro.
