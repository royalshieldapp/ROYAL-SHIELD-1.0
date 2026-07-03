# Royal Shield v2.0 - Guía de Funciones y Pasos Siguientes

Este documento detalla cada función implementada en la interfaz de la aplicación, explicando su estado actual y los pasos necesarios para que sea completamente funcional.

## Estado General

La aplicación está construida con Jetpack Compose y cuenta con tres pantallas principales:

1. **Panel Principal**: Centro de control para las funciones de seguridad.
2. **Suscripción Elite**: Pantalla para mostrar y gestionar suscripciones premium.
3. **Registro de Usuario**: Flujo de inicio de sesión con Google y Firebase.

La interfaz es interactiva, pero la mayoría de las funciones son **simulaciones visuales**. Esto significa que los botones actualizan el estado de la aplicación para que veas cómo respondería la UI, pero no ejecutan la lógica real (ej. no se conectan a un servidor, no usan hardware, etc.).

---

## 1. Panel Principal (Dashboard)

Esta es la pantalla principal, que contiene varios módulos de control.

### Acciones de Seguridad

| Botón | Función Actual (Simulación) | ¿Qué falta para que funcione? |
| :--- | :--- | :--- |
| **Activar/Desactivar Alarma** | Cambia el texto de "Alarma activa" a "Activar alarma" y actualiza el estado general. | Conectar con un **servicio de backend** o una API de hardware que gestione el sistema de alarma perimetral. |
| **Activar/Desactivar Modo Nocturno** | Actualiza la UI para reflejar que el modo nocturno está activo o inactivo. | Implementar la lógica que **controla luces, sensores o configuraciones del sistema** (ej. a través de una API de domótica). |
| **Cámaras en vivo/Reactivar** | Cambia el estado visual para mostrar si las cámaras están "en vivo" o "fuera de servicio". | Conectar con un **servicio de streaming de video** (como WebRTC o un stream RTSP) para mostrar la transmisión real de las cámaras. |
| **Pedir Patrulla** | Incrementa un contador de "acompañamientos" y actualiza el último evento. | Conectar con un **servicio de emergencias o una API de tu backend** que despache una unidad o envíe una alerta a un centro de monitoreo. |

### Centro de Emergencias

| Botón | Función Actual (Simulación) | ¿Qué falta para que funcione? |
| :--- | :--- | :--- |
| **Botón de Pánico** | Cambia el estado a "Alerta enviada" y actualiza el último evento. | Implementar la lógica de emergencia: obtener la **ubicación GPS**, enviar una alerta al backend (usando Twilio, por ejemplo) y notificar a los contactos de emergencia. |
| **Compartir Ubicación** | Actualiza la UI para mostrar "Ubicación enviada". | Obtener la **ubicación GPS** del dispositivo y enviarla a los contactos de emergencia o a un servidor a través de una API. |

### Panel de Automatizaciones Avanzadas (Próximas Funciones)

Todas las funciones de este panel son **simulaciones visuales** que actualizan la UI para mostrar que la acción se ha ejecutado.

| Botón | Función Actual (Simulación) | ¿Qué falta para que funcione? |
| :--- | :--- | :--- |
| **URL Checker** | Incrementa un contador de "Bloqueos". | Implementar una llamada de red al **endpoint `/api/scan-url` de tu backend** (que usa VirusTotal) y mostrar el resultado. |
| **Data Breach Monitor** | Activa o desactiva un estado visual de "Vigilando filtraciones". | Conectar con un **servicio de monitoreo de brechas** (como Have I Been Pwned o el endpoint de tu backend) para verificar el email del usuario. |
| **App Scanner** | Muestra un estado de "Escaneando" y luego "Listo". | Implementar una lógica que **verifique las aplicaciones instaladas** en el dispositivo contra una base de datos de malware (requiere permisos especiales). |
| **Detección de Sonido** | Cambia el estado a "Escuchando" o "Apagado". | Implementar una función que use el **micrófono del dispositivo** para analizar el audio en busca de patrones (gritos, etc.) y dispare una alerta. |
| **Modo Batería Baja** | Activa o desactiva el "protocolo activo". | Implementar un `BroadcastReceiver` que **escuche los cambios en el nivel de batería** y, si baja de un umbral (ej. 15%), envíe una última ubicación. |
| **PIN Falso** | Cambia el estado de "Armado" a "Desarmado". | Implementar una lógica a nivel del sistema o de la pantalla de bloqueo (muy complejo) que detecte un PIN específico y **envíe una alerta silenciosa**. |
| **Asistente de Voz IA** | Cambia el estado de "Escuchando" a "Dormido". | Integrar una **librería de reconocimiento de voz** (como la de Google) para escuchar comandos de activación como "Help". |
| **Grabación Automática** | Cambia el estado de "Grabando" a "Listo". | Implementar una función que **use el micrófono para grabar audio** y guardarlo localmente o subirlo a un servidor. |
| **Ruta Segura** | Cambia el estado de "Monitoreando" a "Inactivo". | Implementar una lógica que use el **GPS para trazar una ruta** y dispare una alerta si el usuario se desvía de ella. |

---

## 2. Pantalla de Suscripción (Elite)

| Botón | Función Actual (Simulación) | ¿Qué falta para que funcione? |
| :--- | :--- | :--- |
| **Suscribirme / Desbloquear prueba VIP** | Muestra un diálogo emergente (`AlertDialog`) con una oferta promocional. | Integrar el **SDK de Google Play Billing**. Al pulsar el botón, se debe iniciar el flujo de compra de una suscripción gestionada por Google Play. |

---

## 3. Pantalla de Registro (Google/Firebase)

| Botón | Función Actual | ¿Qué falta para que funcione? |
| :--- | :--- | :--- |
| **Continuar con Google** | Inicia el flujo de inicio de sesión de Google y se comunica con Firebase para autenticar al usuario. | Esta función está **casi completa**. Solo falta la **configuración del proyecto en Firebase** del lado del desarrollador. |

### Pasos para activar el Registro con Google

1. **Crea un proyecto en Firebase Console** ([console.firebase.google.com](https://console.firebase.google.com/)).
2. Dentro de tu proyecto, ve a **Authentication** → **Sign-in method** y habilita **Google** como proveedor.
3. Descarga el archivo `google-services.json` que Firebase te proporciona y **cópialo en la carpeta `app/`** de tu proyecto en Android Studio.
4. En la configuración de tu proyecto de Firebase, busca el **ID de cliente web** (`Web client ID`).
5. Copia ese ID y pégalo en el archivo `app/src/main/res/values/strings.xml`, reemplazando el valor de `firebase_web_client_id`.

Una vez completados estos pasos, el botón de registro funcionará completamente.
