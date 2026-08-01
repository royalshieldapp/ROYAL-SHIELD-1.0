# Dashboard Floating 3D Cards Design

Fecha: 2026-08-01

## Objetivo

Hacer que las tarjetas existentes del Dashboard se perciban elevadas y flotantes, y que reaccionen de forma suave al movimiento físico del teléfono, sin rediseñar la pantalla ni alterar las acciones de las tarjetas.

## Alcance

- Aplicar el efecto a las tarjetas principales del Dashboard que ya consumen `rememberDashboardParallaxOffset`.
- Mantener la composición, navegación, textos, imágenes, colores black & gold y acciones actuales.
- Conservar separados el chatbot de texto y `DidAgentDialog`.
- No modificar Registration, Splash ni Cyber Screen.

## Comportamiento visual

- Intensidad media: el efecto debe ser claramente visible sin producir movimientos bruscos.
- Cada tarjeta conserva una inclinación base alternada hacia la izquierda o derecha.
- El movimiento del teléfono añade rotación limitada en los ejes X e Y.
- La imagen interna se desplaza ligeramente en sentido parallax para dar profundidad.
- La tarjeta usa elevación, sombra suave y borde dorado existente para reforzar el aspecto flotante.
- La transición hacia una nueva orientación se suaviza; los valores del sensor no se aplican directamente sin filtrado.

## Sensores y ciclo de vida

- Preferir `TYPE_GAME_ROTATION_VECTOR` y usar `TYPE_ROTATION_VECTOR` como alternativa por ofrecer orientación más estable que el acelerómetro sin filtrar.
- Si no existe un sensor de rotación compatible, usar el acelerómetro con filtro de paso bajo.
- Registrar el listener solamente mientras el composable del Dashboard esté activo.
- Liberar el listener en `onDispose`.
- No solicitar permisos Android nuevos.
- Si no hay sensor disponible, mantener únicamente la inclinación 3D estática.

## Límites de movimiento

- Limitar la contribución dinámica a un máximo de 6 grados por eje.
- Limitar el desplazamiento parallax para que la imagen no deje bordes vacíos visibles.
- Ignorar ruido pequeño alrededor de la posición neutral.
- Establecer una posición neutral al abrir el Dashboard para evitar saltos iniciales.

## Arquitectura

- Mantener `DashboardScreen` como consumidor del estado de inclinación.
- Encapsular lectura, normalización, filtrado y liberación del sensor en un helper Compose enfocado.
- Mantener la transformación visual dentro de los componentes de tarjeta mediante `graphicsLayer`.
- Evitar lógica de sensores duplicada dentro de cada tarjeta.

Flujo:

`Sensor del teléfono -> helper de orientación -> inclinación suavizada y acotada -> tarjetas e imagen parallax`

## Accesibilidad y rendimiento

- Respetar la configuración del sistema para reducir animaciones: conservar profundidad estática y reducir o desactivar la reacción continua.
- No mantener sensores activos fuera del Dashboard.
- Evitar recomposiciones innecesarias mediante estado acotado y actualizaciones suavizadas.
- El efecto no debe interferir con scroll, clics, botones ni lectores de pantalla.

## Manejo de errores

- Sensor ausente: usar inclinación estática sin mostrar un error al usuario.
- Lectura inválida o no finita: ignorar la muestra y conservar el último valor válido.
- Cambio de orientación o retorno desde background: recalibrar suavemente sin salto visible.

## Validación

- Pruebas unitarias para normalización, límites, zona muerta y filtrado de valores.
- `:app:testDebugUnitTest` y `:app:assembleDebug` deben pasar.
- `git diff --check` sin errores.
- Instalar el APK actualizado en el Samsung autorizado.
- Verificar en dispositivo: inclinación horizontal y vertical, retorno a neutral, scroll/clics, ausencia de saltos y liberación del sensor al salir del Dashboard.
- La compilación por sí sola no se considera prueba visual E2E.

## Criterios de aceptación

1. Las tarjetas se perciben elevadas aun con el teléfono quieto.
2. Las tarjetas reaccionan de forma visible y suave al inclinar el teléfono en ambos ejes.
3. El movimiento permanece dentro de límites cómodos y vuelve gradualmente a neutral.
4. El efecto no modifica navegación, contenido ni acciones existentes.
5. No se agregan permisos y el sensor deja de escucharse al salir del Dashboard.
6. En un dispositivo sin sensor compatible, el Dashboard funciona con profundidad estática.
