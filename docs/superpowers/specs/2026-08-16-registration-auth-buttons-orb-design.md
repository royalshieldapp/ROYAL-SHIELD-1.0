# Registration Auth Buttons and Orb Design

## Objetivo

Mantener Registration funcional y honesto, aplicar el fondo dorado suministrado a sus botones, incorporar solo iconos que correspondan a proveedores reales o claramente deshabilitados y aumentar ligeramente el orbe.

## Diseño aprobado

- Los botones usan `bg_button_gold.png` como fondo visual, con contenido legible encima.
- Teléfono y Google conservan acciones reales. Facebook, GitHub y email permanecen deshabilitados hasta contar con integración real; Instagram no se muestra.
- Los iconos suministrados se separan como PNG transparentes para teléfono, email, GitHub y Facebook. Google y Apple conservan temporalmente sus assets actuales.
- El orbe bloqueado de Registration aumenta de 155 dp a 170 dp sin cambiar la estructura general de la pantalla.
- No se representa como disponible ningún proveedor sin autenticación implementada.

## Verificación

- Pruebas unitarias de política de proveedores y presentación de Registration.
- Compilación `testDebugUnitTest` y `assembleDebug`.
- Inspección visual en dispositivo cuando ADB esté disponible.
