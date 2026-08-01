# Royal Shield VPN Flags, Solution Engine Gold Controls and Registration Icons

## Alcance aprobado

Actualizar exclusivamente la presentación visual de Royal VPN, `SolutionEngineScreen`, `SolutionEngineResolutionScreen` y los iconos de los botones de `RegistrationScreen`. No cambiar autenticación, navegación, análisis, callbacks, resolución de problemas ni lógica WireGuard.

## Royal VPN

- Derivar cada bandera del `countryCode` real de `VpnServerProfile`.
- Normalizar códigos ISO antes de crear la bandera, incluyendo `UK` a `GB`.
- Mostrar la bandera tanto en la tarjeta del servidor seleccionado como en cada fila de la lista.
- Aplicar un marco negro/dorado y un reflejo inferior mínimo, estático y discreto.
- No descargar imágenes ni depender de red para renderizar banderas.
- No presentar una ubicación inventada como servidor real; los datos obtenidos del backend tienen prioridad.

## Solution Engine

- Aplicar el recurso `button_gold_bg.png` como fondo visual de los botones principales y secundarios de `SolutionEngineScreen` y `SolutionEngineResolutionScreen`.
- Mantener dimensiones táctiles, callbacks, estados habilitado/deshabilitado y contenido semántico existentes.
- Usar texto e iconos oscuros sobre el botón dorado para conservar contraste.
- Aplicar bordes dorados coherentes a tarjetas, paneles, campos, diálogos, controles circulares y filas interactivas dentro de ambas pantallas.
- Conservar colores funcionales rojo, verde y cyan cuando comuniquen estados, alertas o resultados reales; el dorado se usa como estructura visual.

## Registration Screen

- Convertir la referencia entregada en iconos individuales con fondo transparente y estilo circular negro/dorado.
- Asignar teléfono, Google Play, Apple y Git a Phone, Google, Apple y GitHub respectivamente.
- Mantener Facebook y correo con iconos compatibles en negro/dorado sin inventar proveedores nuevos.
- Conservar textos, callbacks, orden de botones, accesibilidad y bordes gold neon ya aprobados.

## Validación

- Ejecutar `:app:compileDebugKotlin`.
- Ejecutar `:app:testDebugUnitTest :app:assembleDebug`.
- Ejecutar `git diff --check` y revisar que las firmas/callbacks no hayan cambiado.
- Instalar el APK debug en el Samsung autorizado.
- Revisar visualmente Registration, Royal VPN y las dos pantallas de Solution Engine.

## Fuera de alcance

- Cambios de backend, Firebase, Render, Railway o WireGuard.
- Nuevas ubicaciones VPN o proveedores de autenticación.
- Cambios en Dashboard, Splash o Cyber.
- Commit, push o deploy.
