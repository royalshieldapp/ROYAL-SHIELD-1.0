# Royal Shield — Registro de trabajo 012

Fecha: 2026-08-09 (America/New_York)

## Alcance

Validar la corrección existente de la prueba base Royal Elite y revisar, sin
simular éxito, las rutas de backend, VPN, facturación y dispositivo relevantes
para la Fase A de publicación. No se hicieron commits, pushes, despliegues,
compras ni publicación en tiendas.

## Estado inicial preservado

El checkout ya tenía cambios no confirmados de trabajo anterior. Se preservaron
sin sobrescribirlos. En particular, `RoyalEliteBadgeAssetTest.kt` está en
`HEAD`, mientras que `royal_elite_badge.png` existe solamente como archivo no
versionado. Por eso una rama limpia del commit `16cd42c3` no contiene el asset
y la prueba habría fallado por recurso ausente; el árbol actual sí lo contiene.

## Evidencia obtenida

| Flujo | Evidencia | Estado |
|---|---|---|
| Prueba base Royal Elite | `:app:testDebugUnitTest --tests "com.royalshield.app.ui.components.RoyalEliteBadgeAssetTest"` terminó exit 0; asset presente, PNG RGBA de 578437 bytes y SHA-256 `B9734E05D734F899B0541DC2F747DA57496D6AAF6BC36DE1A3448E294805F59B` | VERIFICADO EN ÁRBOL ACTUAL |
| Suite Android unitaria | XML nuevo de `testDebugUnitTest`: 24 pruebas, 0 fallos, 0 errores, 0 omitidas | VERIFICADO EN FUENTE |
| APK debug | La ejecución conjunta de `:app:testDebugUnitTest :app:assembleDebug` excedió 184 s y no produjo un resultado final legible; no se usa un APK previo como evidencia | NO VERIFICADO EN ESTA EJECUCIÓN |
| Backend unificado | `python -m compileall api config services -q` exit 0. El arranque local con TestClient se bloquea antes de crear la app por ausencia local de `firebase_admin`; `requirements-render.txt` sí declara `firebase-admin>=6.5.0` | COMPILABLE; RUNTIME LOCAL NO VERIFICADO |
| VPN | El router unificado devuelve explícitamente `not_configured` / `VPN_NOT_CONFIGURED` cuando faltan proveedor, clave pública, servidores o control plane. La infraestructura y el registro real de peers no se verificaron. La UI aún conserva `staticServers` y ping `28 ms` cuando hay conexión, por lo que no es evidencia de servidores o latencia reales | UNAVAILABLE / BLOQUEADOR DE PUBLICACIÓN |
| Facturación | Existe `api/routes/billing.py`, pero `api/main.py` no importa ni registra `billing.router`; por tanto el backend que Render arranca no expone `/api/billing/*`. Además el estado por usuario en ese router sigue devolviendo FREE fijo y no hay compra/restauración/validación Google Play E2E | NO CONFIGURADO / BLOQUEADOR DE PUBLICACIÓN |
| Dispositivo | `adb devices -l` no mostró dispositivos. No se instaló APK, no se abrió la app y no se intentó compra, túnel VPN ni login | NO VERIFICADO E2E |

## Bloqueadores priorizados

1. Incluir el asset y los archivos Royal Elite pendientes en el próximo commit autorizado; sin ello la prueba vuelve a quedar bloqueada en una rama limpia.
2. Sustituir servidores y latencia estáticos de `VpnScreen` por estado `UNAVAILABLE` hasta tener servidor WireGuard y control plane de registro de peers operativos.
3. Registrar y probar de forma segura el router de billing del backend unificado, con persistencia de entitlement validada server-side; no publicar mientras se devuelva FREE fijo o el endpoint sea 404.
4. Construir el entorno backend desde `requirements-render.txt` y ejecutar startup/health/API con configuración no secreta para obtener evidencia de runtime.
5. Conectar un dispositivo físico ADB y verificar instalación, mensajes de indisponibilidad VPN y flujo de Google Play con cuenta de prueba, sin activar ni cobrar producción.

## Límites

- Compilación, una ruta `/health` o una prueba local no son prueba E2E de Render,
  WireGuard, Google Play Billing o dispositivo.
- No se revisaron ni modificaron Registration, Splash, Dashboard ni Cyber Screen.
