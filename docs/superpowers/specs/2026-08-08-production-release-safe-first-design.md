# Royal Shield — Diseño de lanzamiento seguro y evolución a producción

## Objetivo

Publicar Royal Shield sin presentar datos ficticios, estados aleatorios o integraciones no verificadas como protección real; convertir los módulos avanzados en flujos verificables por fases.

## Decisión de producto

La primera versión pública contendrá únicamente funciones que superen prueba de extremo a extremo. Las funciones restantes no aparecerán como activas: se ocultarán o mostrarán un estado explícito `No disponible` con su motivo y sin puntajes ni resultados inventados. Los módulos avanzados se liberarán primero a pruebas cerradas y pasarán a producción solo con evidencia registrada.

## Arquitectura objetivo

```text
Android (Kotlin/Compose)
  -> Render: API autenticada, validación, rate limit y estado de disponibilidad
      -> VirusTotal: análisis de URL/archivo, únicamente desde servidor
      -> Twilio: SOS por SMS, únicamente después de consentimiento y validación
      -> PostgreSQL/Firebase: datos del usuario, auditoría mínima y preferencias
  -> Google Cloud: servidor WireGuard administrado para el túnel VPN
```

- Android no incluirá secretos de VirusTotal, Twilio ni credenciales de infraestructura.
- Render será el único backend público configurado por `API_BASE_URL`; se reconciliará el servicio desplegado con el repositorio y commit fuente antes de habilitar módulos.
- Google Cloud alojará WireGuard. Android solo consume configuración emitida para el usuario autenticado y nunca registra claves privadas.
- VirusTotal se invocará solo desde Render con límites por usuario/IP, normalización de entrada y respuestas sanitizadas.
- Twilio se invocará solo desde Render. Cada SOS almacenará consentimiento, hora, destinatarios configurados y resultado sin exponer teléfono completo en logs.

## Contrato de verdad para módulos remotos

Todo endpoint o pantalla con proveedor externo devuelve uno de estos estados:

```json
{
  "status": "READY | DEGRADED | UNAVAILABLE",
  "generatedAt": "ISO-8601",
  "dataFreshness": "ISO-8601 | null",
  "sources": ["provider-name"],
  "confidence": 0.0,
  "reason": "sanitized optional explanation"
}
```

- `READY`: respuesta actual obtenida y validada.
- `DEGRADED`: respuesta real con limitación visible.
- `UNAVAILABLE`: no se muestran métricas ni resultados ficticios; la UI explica cómo reintentar.
- Entradas inválidas: HTTP 422. Proveedor no disponible: HTTP 503 cuando la acción no puede completarse.

## Alcance por fase

### Fase A — Base publicable

1. Compilación reproducible de AAB firmado, pruebas unitarias/lint y APK de release en dispositivo físico.
2. Autenticación, perfil, ajustes, términos, privacidad y eliminación de cuenta/datos verificables.
3. SOS con ubicación y Twilio, con consentimiento, permisos mínimos y prueba de entrega controlada.
4. Análisis de URL mediante Render/VirusTotal con límites, resultado real y manejo de cuota agotada.
5. Google Play Billing con productos reales, tester licenciado, compra, restauración y entitlement verificable.
6. Data Safety, política de privacidad pública, disclosures dentro de la app, soporte, ficha de Play y assets de lanzamiento.

### Fase B — VPN beta cerrada

1. Reconciliar Google Cloud, instalación/configuración WireGuard, firewall y rotación de pares.
2. Emitir configuración por backend autenticado y revocable.
3. Probar túnel, DNS, fuga de IP, reconexión, revocación y consumo de batería en teléfono físico.
4. Solo después, habilitar VPN para un porcentaje de testers y registrar fallos sin datos sensibles.

### Fase C — Inteligencia y automatizaciones avanzadas

1. Reemplazar Dashboard/XDR/Threat Radar, incidentes, reportes, marketplace y políticas con repositorios durables o retirarlos del flujo público.
2. Completar Forecast con datos históricos reales, modelo versionado, métricas y contrato de disponibilidad.
3. Revisar individualmente permisos SMS, listado de aplicaciones, ubicación en segundo plano, overlay, cámara y micrófono; eliminar los que no sean imprescindibles y habilitar los restantes solo con caso de uso, consentimiento y declaración aprobable en Play.

## Casos límite obligatorios

- Sin red, cuota de proveedor agotada, token vencido o backend no desplegado: `UNAVAILABLE`, sin fallback simulado.
- SOS sin permiso de ubicación, sin contacto o sin consentimiento: bloquear envío y guiar al usuario; nunca declarar alerta enviada.
- VPN sin servidor sano o par revocado: no conectar y conservar tráfico fuera del túnel salvo que el usuario active explícitamente una política kill switch compatible.
- Compra cancelada, pendiente o no reconocida: no desbloquear Elite; restaurar solo compras confirmadas.
- Datos de un menor o familiar: no compartir ubicación ni contacto sin rol, autorización y reglas Firebase verificadas.

## Métricas de salida

- 100% de pantallas públicas clasificadas `REAL`, `DEGRADADA` u `OCULTA`; cero resultados mock en flujos activos.
- AAB verificado, instalado y recorrido en un teléfono físico con cuenta de prueba.
- SOS controlado entregado, análisis de URL real y compra/restauración de prueba exitosos.
- VPN con prueba documentada de túnel antes de cualquier activación pública.
- Play Console sin declaraciones pendientes para permisos/datos usados.

## Fuera de alcance inicial

No se habilitan envíos SMS, acceso a SMS, escaneo global de apps, audio continuo, overlay o ubicación en segundo plano solo porque el manifiesto los declare. Cada uno requiere una ficha independiente, evidencia técnica y aceptación de Play antes de entrar a producción.
