# Royal Shield Registration and VPN Luxury Design

## Alcance aprobado

Actualizar solo la presentación visual de `RegistrationScreen` y del botón principal de conexión de `VpnScreen`. No cambiar autenticación, navegación, permisos, estados ni lógica WireGuard.

## Registration Screen

- Sustituir el fondo actual por la imagen dorada de rostro IA adjunta (`codex-clipboard-20cbb69c-9d05-44ff-a135-19a11edb7436.png`), copiada como recurso propio de Registration y mostrada a pantalla completa con `ContentScale.Crop`.
- Aplicar una capa negra translúcida ligera para conservar la presencia visual de la imagen sin perder legibilidad.
- Conservar el logo transparente `royal_shield_logo`.
- Aumentar el logo de 120 dp a una presencia hero responsive, claramente dominante.
- Encerrar el contenido introductorio y los controles de registro en superficies negras translúcidas con esquinas amplias, doble contorno dorado y brillo contenido, siguiendo la primera referencia.
- Usar `Orbitron` para el mensaje principal y etiquetas destacadas.
- Mostrar literalmente:
  - `Powered by AI. Built for your safety.`
  - `Sign up to activate advanced cybersecurity features and personalized protection`
- Mantener accesibilidad, scroll en pantallas pequeñas y todos los callbacks existentes.

## VPN Screen

- Mantener `AurumPowerButton`, su tamaño funcional, estados y `onClick`.
- Cambiar únicamente su tratamiento visual a un dial de lujo negro/dorado inspirado en la segunda referencia: anillos concéntricos, bisel metálico, centro oscuro, líneas laterales decorativas y estado visible.
- Los colores de estado continúan derivados de `vpnStateColor`; conexión real, errores y estados intermedios no se ocultan.

## Validación

- Compilar `:app:compileDebugKotlin`.
- Ejecutar `:app:testDebugUnitTest :app:assembleDebug`.
- Instalar en Samsung y revisar Registration y VPN visualmente cuando la navegación disponible lo permita.
- Confirmar mediante diff que no cambió la lógica de autenticación ni VPN.

## Fuera de alcance

- Nuevos proveedores de autenticación.
- Cambios en Firebase, backend, Render o WireGuard.
- No tocar ni rediseñar Splash, Dashboard o Cyber Screen en esta entrega.
- Commit, push o deploy.
