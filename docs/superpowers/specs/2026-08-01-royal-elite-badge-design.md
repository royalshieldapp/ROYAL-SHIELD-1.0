# Royal Elite Badge Design

## Objetivo

Integrar la insignia entregada por el usuario como identificador visual oficial de Royal Shield Elite sin conceder ni aparentar acceso premium a cuentas sin entitlement verificado.

## Asset

- Fuente: `C:\Users\Anonymous\Pictures\ChatGPT Image Jul 15, 2026, 08_30_16 PM.png`.
- Conservar sin redibujar la corona, el escudo, el marco negro/dorado y el texto exacto `ROYAL ELITE`.
- Eliminar únicamente el fondo blanco, la franja negra lateral y el espacio vacío exterior.
- Producir un PNG recortado con transparencia, bordes limpios y resolución suficiente para pantallas Android de alta densidad.
- Guardar el asset final como recurso nuevo; no sobrescribir los emblemas Starter, Gold o Ultimate existentes.

## Ubicaciones aprobadas

1. `PremiumEliteScreen` y/o la tarjeta `ULTIMATE/ELITE`: mostrar la insignia como vista previa del beneficio, sin presentarla como adquirida.
2. Dashboard: mostrarla como insignia activa solo cuando el estado de suscripción confirmado corresponda a Elite/Ultimate.
3. Profile: mostrar la insignia activa junto al estado del plan solo para Elite/Ultimate confirmado.
4. Settings: usarla en la entrada `Royal Shield Elite`, diferenciando vista promocional de estado activo.
5. Loyalty: mostrar la insignia bloqueada para cuentas no Elite y desbloqueada únicamente con entitlement Elite/Ultimate confirmado.

## Contrato de entitlement

- La imagen no concede acceso ni modifica el plan.
- No usar `DEV_SUPERPOWERS`, precios fallback, `PreferencesManager` aislado ni valores hardcodeados como prueba de compra.
- El estado activo debe derivarse del flujo de entitlement vigente; mientras la verificación server-side de Google Play siga pendiente, la UI debe evitar afirmar que una compra es válida solo por datos locales.
- La tarjeta comercial puede mostrar la insignia como vista previa, claramente separada de la indicación `ELITE ACTIVE`.

## Presentación visual

- Mantener estilo black & gold premium.
- No añadir rojo, cyan ni marcos multicolor alrededor de la insignia.
- Usar brillo dorado moderado; no deformar ni recortar la corona o el texto.
- Proporcionar `contentDescription` accesible en cada uso significativo.

## Validación

- Verificar el canal alfa, las esquinas transparentes y ausencia de franja negra/fondo blanco.
- Añadir una prueba enfocada para confirmar que el recurso se empaqueta y que las rutas aprobadas lo consumen con estados bloqueado/activo diferenciados.
- Ejecutar `:app:testDebugUnitTest :app:assembleDebug`.
- Revisar `git diff --check` y diff enfocado.
- Instalar en Samsung y comprobar las rutas accesibles sin efectuar una compra real.
- No declarar Billing ni Elite E2E verificados hasta probar una compra de licencia mediante una build instalada desde Google Play.

## Fuera de alcance

- Cambiar IDs o precios de productos.
- Conceder Elite manualmente.
- Modificar Firebase, Firestore, backend o Play Console.
- Commit, push, deploy o compra real.
