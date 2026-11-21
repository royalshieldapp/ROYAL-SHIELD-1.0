# Royal Shield: Scan Files, contactos SOS y reportes diarios

Fecha: 2026-08-15
Zona horaria operativa: America/New_York

## Objetivo

Corregir antes de la entrega los flujos de escaneo de archivos y contactos SOS, eliminando resultados engañosos, rutas duplicadas y almacenamiento incompatible. Registrar cada jornada de trabajo en Markdown con evidencia reproducible y sin datos sensibles.

## Alcance

### 1. Scan Files

- Mantener una sola pantalla de escaneo y una sola ruta de navegación.
- Restringir el escaneo a un entitlement de servidor `STARTER`, `GOLD` o `ULTIMATE`.
- Leer el archivo seleccionado mediante `ContentResolver`; no convertir un URI `content://` en `File(uri.path)`.
- Calcular SHA-256 localmente mediante streaming, con límites explícitos de tamaño y tiempo.
- Consultar el hash únicamente a través del backend Render autenticado.
- Mantener `VIRUSTOTAL_API_KEY` exclusivamente en el entorno del backend.
- Mostrar estados diferenciados:
  - `READY`: el proveedor devolvió un análisis real y vigente.
  - `DEGRADED`: existe una respuesta real parcial o limitada.
  - `UNAVAILABLE`: proveedor, configuración, red o backend no disponible; nunca equivale a limpio.
  - `INVALID`: archivo vacío, ilegible, demasiado grande o entrada no aceptada.
- Un hash desconocido o una respuesta `404` se mostrará como no analizado, no como seguro.
- El cliente no afirmará que un archivo es completamente seguro. Comunicará si se detectaron amenazas conocidas o si no fue posible verificarlo.

### 2. Contactos de emergencia SOS

- Crear una única fuente de verdad en `PreferencesManager` usando almacenamiento cifrado.
- Sustituir el almacenamiento separado `live_location_prefs/contacts_list` o migrarlo de forma segura una sola vez.
- Ofrecer la misma sección de administración desde Tracking/SOS y Settings.
- Cuando no existan contactos, el intento de SOS abrirá directamente esa sección.
- Permitir agregar y eliminar contactos; no solicitar acceso a la libreta del teléfono.
- Validar nombre no vacío, teléfono con formato internacional E.164 y un máximo de cinco destinatarios.
- Mostrar y confirmar los destinatarios antes de un envío manual.
- No registrar teléfonos, ubicación precisa ni contenido del mensaje en logs.
- Mostrar `enviado` solamente si el backend confirma al menos una entrega. Fallos parciales deben aparecer como `DEGRADED`; cero entregas como `UNAVAILABLE`.

### 3. Reportes diarios

- Almacenar los reportes en `docs/daily-reports/`.
- Usar el nombre `YYYY-MM-DD-royal-shield-daily-report.md`.
- Registrar cada evento con hora local `HH:mm:ss z` de America/New_York.
- Incluir:
  - objetivo del día;
  - estado inicial de Git;
  - decisiones aprobadas;
  - cambios y archivos afectados;
  - comandos ejecutados y resultados;
  - clasificación `REAL`, `SIMULADA`, `NO_CONFIGURADA` o `NO_VERIFICADA`;
  - bloqueadores y riesgos;
  - siguiente paso recomendado.
- Redactar secretos, tokens, teléfonos, correos privados, identificadores de cuenta, ubicaciones precisas y cuerpos sensibles de proveedores.
- El reporte registra evidencia, pero no sustituye pruebas de dispositivo, proveedor o despliegue.

## Arquitectura

### Scan Files

```text
FileScanScreen
  -> FileScanViewModel
  -> FileScannerRepository
  -> ContentResolver + SHA-256
  -> RenderSecurityApi con Firebase ID token
  -> Render FastAPI
  -> VirusTotal
```

La respuesta del backend utiliza el contrato de disponibilidad compartido. Android transforma la respuesta en estados tipados de UI y no utiliza booleanos ambiguos.

### SOS

```text
Tracking/SOS o Settings
  -> EmergencyContactsScreen
  -> PreferencesManager cifrado
  -> confirmación de destinatarios
  -> SosRepository
  -> Render FastAPI
  -> Twilio
```

`LocationActivity` puede reutilizar la lista segura, pero no mantendrá una segunda copia de contactos.

## Manejo de errores

- Entrada inválida: estado local `INVALID` o HTTP 422.
- Proveedor no configurado o cero entregas: HTTP 503 con `UNAVAILABLE`.
- Error parcial comprobado: `DEGRADED` con conteos no sensibles.
- Error de autenticación: no ejecutar la operación y solicitar iniciar sesión nuevamente.
- Timeout o desconexión: mostrar `UNAVAILABLE`; conservar la posibilidad de reintentar sin afirmar éxito.
- Ninguna excepción del cliente puede convertirse en “sin amenazas” o “SOS enviado”.

## Estrategia de pruebas

### Scan Files

- Prueba de URI `content://` con lectura por stream.
- Prueba de hash SHA-256 conocido.
- Pruebas de archivo vacío, ilegible y demasiado grande.
- Pruebas de backend para hash conocido, desconocido, proveedor ausente, rate limit y autenticación.
- Prueba de entitlement: `FREE` bloqueado; `STARTER+` permitido solo cuando fue verificado por servidor.
- Prueba que confirme que error y `404` nunca producen resultado limpio.

### SOS

- Pruebas de validación E.164, máximo de cinco contactos y persistencia cifrada.
- Prueba de migración sin duplicados.
- Prueba de navegación cuando la lista está vacía.
- Pruebas de cero, algunas y todas las entregas confirmadas.
- Prueba que confirme que los logs no incluyen teléfonos ni ubicación precisa.

### Validación final

- Pruebas unitarias Android y backend.
- `assembleDebug`, lint y compilación backend.
- Instalación y navegación en dispositivo físico.
- Selección y consulta controlada de un archivo de prueba no sensible.
- Prueba SOS únicamente con un contacto autorizado.
- Evidencia de Render y proveedores redactada en el reporte diario.

## Fuera de alcance

- Escaneo antivirus en tiempo real de todo el almacenamiento.
- Cuarentena o eliminación automática de archivos.
- Lectura automática de la libreta de contactos.
- Garantías de que un archivo es completamente seguro.
- Publicación, deploy, commit o push sin autorización explícita.

## Criterios de aceptación

- Existe una sola experiencia accesible de Scan Files y no puede evitarse el entitlement desde otra ruta.
- Ningún fallo, hash desconocido o proveedor ausente se representa como archivo limpio.
- Android no contiene ni solicita una clave de VirusTotal.
- Los contactos configurados son los mismos que utiliza el SOS.
- El usuario puede administrar contactos desde Tracking/SOS y Settings.
- SOS no informa éxito sin confirmación real del backend.
- Existe un reporte Markdown del día con comandos y resultados verificables, sin secretos ni datos personales.
