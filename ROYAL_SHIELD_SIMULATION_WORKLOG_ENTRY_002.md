# Royal Shield — Registro de trabajo, entrada 002

**Fecha/hora:** 2026-07-29 21:30:55 -04:00  
**Alcance:** revisión read-only de persistencia para problemas e incidencias vinculadas al código. No se creó ninguna tabla, base de datos ni migración.

## Hallazgos

- FastAPI usa SQLAlchemy con `DATABASE_URL`; producción requiere esa variable y desarrollo permite `sqlite:///./dev.db`.
- El arranque ejecuta `Base.metadata.create_all(bind=engine)`, sin un historial de migraciones versionado.
- `render.yaml` declara `DATABASE_URL`, pero el repositorio no demuestra que el esquema o los datos existan actualmente en producción.
- Las tablas existentes cubren Forecast Engine, amenazas, threat map y loyalty; no existe un registro de problemas de ingeniería.
- `threat_map_events` solo cubre parcialmente fecha, severidad y módulo de origen. No incluye archivo, estado, evidencia y resolución.
- Incident Response en Android cubre parcialmente fecha, severidad, estado y enlace de evidencia, pero usa datos simulados, `SharedPreferences` y sincronización Firestore de mejor esfuerzo.
- El modelo IR no tiene módulo/archivo estructurado, fecha de resolución, resolución final ni resultado de validación.
- No hay reglas Firestore versionadas para IR. Realtime Database, Room y el backend Node tampoco contienen un registro equivalente.
- `AuditLogsScreen.kt` usa eventos hardcodeados.
- No se encontraron Prisma, Alembic, migraciones SQL versionadas ni migraciones Firestore.

## Cobertura requerida

| Campo requerido | Cobertura real actual |
| --- | --- |
| Fecha | Parcial |
| Módulo afectado | Parcial |
| Archivo afectado | No |
| Severidad | Parcial, en dominios distintos |
| Estado | Parcial |
| Evidencia | Parcial |
| Resolución | No estructurada |

## Resultado y recomendación

Royal Shield tiene persistencia para eventos de producto, pero no un esquema persistente, versionado y demostrado en producción para registrar problemas de ingeniería vinculados al código.

El próximo paso recomendado es decidir si el registro será interno o visible para usuarios, definir el contrato mínimo y la estrategia de migraciones, y después verificar PostgreSQL de producción de forma read-only. No se deben almacenar secretos, tokens, PII ni logs crudos.

## Cambios y validaciones

- Solo se añadió documentación; no se alteró código, modelos, reglas, datos, endpoints ni infraestructura.
- Se revisaron SQLAlchemy, `DATABASE_URL`, Render, Incident Response, Firebase, Room, Node y Audit Logs.
