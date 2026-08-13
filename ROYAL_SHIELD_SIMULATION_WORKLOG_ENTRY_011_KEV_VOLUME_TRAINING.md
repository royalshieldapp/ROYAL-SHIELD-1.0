# Royal Shield — Registro de trabajo 011

Fecha: 2026-08-09 (America/New_York)

## Alcance

Convertir la evaluación local del CSV CISA KEV en un pipeline reproducible de
entrenamiento para un objetivo limitado y verificable. No se modificaron
pantallas Android, rutas API, despliegues, credenciales ni la Fase A.

## Objetivo y límites de verdad

| Elemento | Decisión | Estado |
|---|---|---|
| Objetivo | Pronosticar el volumen de altas de catálogo CISA KEV durante los próximos 7 días | IMPLEMENTADO EN FUENTE |
| Probabilidad de explotación por CVE | Fuera de alcance: las 25 filas disponibles tienen `label=exploited` | UNAVAILABLE |
| Atributos del modelo | Sólo conteos y calendario derivados de `event_time`; no usa `label`, `cisa_due_date` ni otros campos posteriores | VERIFICADO EN PRUEBA |
| Validación | Holdout temporal final de 14 días; fechas de entrenamiento estrictamente anteriores | VERIFICADO EN PRUEBA |
| Exposición API/UI | No existe ruta ni pantalla nueva; por tanto no se publica ningún puntaje | NO CONFIGURADA |

`event_time` representa la fecha de adición al catálogo, no la primera
observación de explotación. El pipeline no afirma prevenir, detectar o
predecir la explotación de una vulnerabilidad concreta.

## Evidencia de ejecución local

- Entrada: `D:\Downloads\cyber_training_cisa_kev_2026_07.csv`.
- Filas: 25; periodo de `event_time`: 2026-07-01 a 2026-07-27.
- SHA-256 de entrada: `c295dce0b732ba287d7e0683b9d5bcc6b0a10a03421f08ddcb41f5f5f216f03b`.
- Versión propuesta de artefacto: `kev-volume-forecast-2026.08.09.1`.
- Resultado del comando real: `UNAVAILABLE`, motivo `INSUFFICIENT_TEMPORAL_HISTORY`.
- Historial observado: 27 días; mínimo configurado: 60 días; no se escribió
  un archivo de modelo.
- Reporte reproducible local: `royal_shield_backend_unified/artifacts/kev_volume_cisa_2026_07/kev_volume_training_report.json`.

## Validaciones

- RED: la suite falló primero al importar el módulo inexistente y después al
  exigir el CLI/reporte inexistentes.
- GREEN: `python -m pytest tests/test_kev_volume_training.py -q` → 5 passed.
- `python -m compileall services/ml/kev_volume_training.py scripts/train_kev_volume_model.py` → exit 0.
- `git diff --check` → exit 0; sólo avisos CRLF sobre cambios ajenos existentes.

## Bloqueos y siguiente ruta segura

1. Conservar al menos 60 días de adiciones diarias de CISA KEV con el mismo
   esquema y ejecutar el CLI de nuevo.
2. Revisar `validation_mae` frente a `naive_validation_mae`: sólo `READY` si
   el modelo no es peor que el baseline; si no, quedará `DEGRADED`.
3. Antes de exponer resultados, definir un contrato backend con
   `READY | DEGRADED | UNAVAILABLE`, fuente, frescura, versión y métricas; una
   ausencia de artefacto debe responder `UNAVAILABLE`, nunca un score fallback.
4. La Fase A sigue bloqueada independientemente por el baseline Android Royal
   Elite y la evidencia de publicación pendiente; este trabajo no altera ese
   estado.
