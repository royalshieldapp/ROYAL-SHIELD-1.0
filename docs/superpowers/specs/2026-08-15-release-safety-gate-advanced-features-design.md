# Royal Shield Release Safety Gate para funciones avanzadas

Fecha: 2026-08-15
Zona horaria: America/New_York

## Objetivo

Impedir que la versión de entrega presente como operativas funciones avanzadas simuladas, no configuradas o sin evidencia end-to-end. Mantener visible únicamente información real y verificable, usando estados honestos cuando una capacidad todavía no esté disponible.

## Estado común

Todo módulo cubierto por este gate utiliza uno de estos estados:

- `READY`: fuente real, configuración, operación y evidencia end-to-end suficientes para la acción mostrada.
- `DEGRADED`: datos o acción reales, pero con una limitación concreta y visible.
- `UNAVAILABLE`: proveedor ausente, configuración incompleta, simulación detectada, falta de evidencia o fallo que impide sostener el resultado.

Cada respuesta incluye `generatedAt`, `dataFreshness`, `sources`, `modelVersion`, `confidence` y `reason` cuando sean aplicables. Los campos que no puedan sostenerse con evidencia permanecen nulos; nunca se rellenan con constantes, muestras o aleatorios.

## Política de presentación

- Una función `READY` conserva su acción normal.
- Una función `DEGRADED` muestra la limitación antes de permitir una acción segura.
- Una función `UNAVAILABLE` muestra una tarjeta visible y deshabilitada cuando el usuario necesita entender que la capacidad existe pero no está disponible.
- Se ocultan acciones peligrosas, engañosas o sin comportamiento útil cuando mostrarlas pueda inducir al usuario a creer que está protegido.
- No se rediseñan Dashboard ni Cyber; solamente se cambia disponibilidad, copy y navegación de las tarjetas afectadas.
- Compilación, `/health` y existencia de código no son evidencia end-to-end.

## 1. VPN

### `READY`

Requiere perfil WireGuard real emitido por el servicio autorizado, consentimiento Android, interfaz `VpnService` activa, handshake reciente, bytes transmitidos y recibidos, y prueba de ruta o IP pública a través del túnel.

### Estado previo a la evidencia

- Mostrar `UNAVAILABLE` con “VPN no disponible temporalmente”.
- No permitir que un estado local o la creación de `VpnService` muestre “Connected”.
- No crear configuración fallback, peer local o éxito simulado.
- Conservar la pantalla y controles para pruebas internas únicamente cuando estén claramente marcados como no conectados.

## 2. Forecast Engine v2

### `READY`

Requiere fuentes reales identificadas, filas durables válidas, rechazo documentado de filas inválidas, split cronológico sin leakage, entrenamiento reproducible, métricas de validación, artefacto persistente con versión y SHA-256, carga verificada en el backend desplegado, respuesta API y resultado Android.

### Estado previo a la evidencia

- Mostrar `UNAVAILABLE` con “Modelo en preparación — predicciones no disponibles”.
- Eliminar confianza fija, factores mock, resultados de muestra y forecast sin modelo.
- Un entrenamiento con datos sintéticos o un script que solamente ejecuta no habilita `READY`.
- El informe de investigación de fuentes se incorpora como evidencia de selección, no como evidencia de entrenamiento.

## 3. Threat Map e inteligencia avanzada

### `READY`

Cada evento requiere fuente, identificador externo o procedencia verificable, `dataFreshness`, coordenadas válidas cuando sean necesarias y respuesta real del backend.

### Estado previo a la evidencia

- Eliminar amenazas mock y conteos derivados de muestras.
- Mostrar mapa vacío con `UNAVAILABLE`, razón sanitizada y posibilidad de reintentar.
- No mostrar objetivo principal, alertas, severidad o confianza inventados.
- Diferenciar explícitamente inteligencia cyber de eventos físicos o geoespaciales.

## 4. Automatizaciones

### `READY`

Una automatización requiere regla persistida, disparador observable, acción implementada, resultado verificable y registro no sensible de ejecución.

### Estado previo a la evidencia

- Las reglas persistidas pueden mostrarse, pero cada acción no implementada queda deshabilitada como “Próximamente”.
- Una barra de progreso, delay o cambio visual no equivale a ejecución.
- Nunca mostrar finalización exitosa si la acción real no ocurrió.
- Acciones de seguridad o SOS requieren sus propios contratos y consentimiento.

## 5. Reportes y políticas

### Reportes

- El estado inicial es una lista vacía, no reportes de muestra.
- Un reporte identifica verificaciones ejecutadas, omitidas, fallidas y no disponibles.
- Se eliminan rutas de archivos fallback ficticios.
- Un elemento no leído o no accesible se marca `NO_VERIFICADA`, nunca limpio.

### Políticas

- El estado inicial es una lista vacía o una lista de plantillas claramente etiquetadas como plantillas no activas.
- Guardar una política no implica que esté aplicada al sistema.
- Solo se muestra `READY` cuando el mecanismo de enforcement correspondiente fue verificado.
- Las políticas sin enforcement quedan como configuración propuesta, no protección activa.

## Arquitectura

```text
Backend real/proveedor
        ↓
AvailabilityResponse
        ↓
Repository/ViewModel
        ↓
FeatureAvailabilityRegistry
        ↓
Tarjeta READY | DEGRADED | UNAVAILABLE
```

`FeatureAvailabilityRegistry` centraliza únicamente el estado recibido y las reglas de presentación. No inventa disponibilidad local ni sustituye al backend.

## Inventario inicial confirmado

| Función | Evidencia actual | Clasificación inicial |
|---|---|---|
| VPN | Código de túnel sin prueba actual completa de handshake, tráfico y ruta | `NO_VERIFICADA` |
| Forecast Engine | Confianza fija, factores mock y forecast pendiente en rutas actuales | `SIMULADA` |
| Threat Map/inteligencia | Ruta backend con amenazas mock y UI con métricas visibles | `SIMULADA` |
| Automatizaciones | Persistencia parcial; delays/progreso no demuestran acciones | `NO_VERIFICADA` |
| Reports Center | Reportes iniciales y archivos fallback de muestra | `SIMULADA` |
| Policy Manager | Políticas de muestra persistidas sin enforcement demostrado | `SIMULADA` |

## Pruebas

- Pruebas unitarias del mapeo `READY`, `DEGRADED` y `UNAVAILABLE`.
- Pruebas que rechazan confianza fija, muestras, aleatorios y éxito sin acción.
- Pruebas de UI para tarjetas deshabilitadas y copy honesto.
- Pruebas de backend que devuelven HTTP 422 para entrada inválida y HTTP 503 para proveedor/acción no disponible.
- Búsqueda estática enfocada de `mock`, `sample`, `fake`, `random`, `fallback`, confianza fija y delays que representen trabajo.
- Compilación, lint y pruebas de los módulos afectados.
- Navegación en dispositivo físico comprobando que ninguna acción no disponible afirme éxito.
- Evidencia E2E específica antes de cambiar cualquier módulo a `READY`.

## Reporte diario

Cada cambio del gate se registra con hora, archivo, comando, resultado y clasificación. El reporte no contiene secretos, tokens, teléfonos, ubicaciones precisas, identificadores de cuenta ni cuerpos de proveedor.

## Fuera de alcance inmediato

- Completar todas las integraciones externas antes del miércoles.
- Publicar, desplegar, comprar servicios o cambiar secretos sin autorización explícita.
- Declarar una función `READY` basándose solamente en código local, build o health check.
- Reestructurar Registration, Splash, Dashboard o Cyber.

## Criterios de aceptación

- Ninguna función avanzada simulada o no verificada se presenta como operativa.
- No quedan amenazas, reportes, archivos, políticas, predicciones, confianza ni éxitos de muestra visibles como datos reales.
- VPN no muestra conexión sin evidencia de túnel y tráfico.
- Forecast no muestra predicción sin artefacto entrenado, cargado y verificado.
- Automatizaciones no muestran éxito sin acción real.
- Los estados no disponibles son claros, seguros y consistentes.
- La app compila, las pruebas pasan y la navegación se valida en dispositivo antes de la entrega.
