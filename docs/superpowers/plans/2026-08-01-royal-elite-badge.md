# Royal Elite Badge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrar el asset oficial `ROYAL ELITE` como vista promocional o insignia bloqueada, sin mostrarlo como activo hasta disponer de entitlement Elite/Ultimate verificado fuera del almacenamiento local.

**Architecture:** El PNG final se procesa de forma no destructiva y se valida como recurso Android con transparencia. Un componente Compose reutilizable recibe un modo explícito (`PROMOTIONAL`, `LOCKED` o `ACTIVE_VERIFIED`); las pantallas comerciales pueden usar `PROMOTIONAL`, Loyalty puede usar `LOCKED`, y Dashboard/Profile solo podrán usar `ACTIVE_VERIFIED` cuando una fuente verificable lo suministre. En esta fase no se convierte `EntitlementStore`, `PreferencesManager`, `DEV_SUPERPOWERS`, precios fallback ni `hasPremium` en prueba de entitlement server-side.

**Tech Stack:** PNG RGBA, Android resources, Kotlin, Jetpack Compose, JUnit, Gradle Kotlin DSL, ADB.

## Global Constraints

- Preservar el texto exacto `ROYAL ELITE`, la corona, el escudo y el marco black & gold.
- No modificar Registration, Splash, Cyber, autenticación, navegación, Billing, IDs/precios, Firebase, backend ni Play Console.
- No conceder Elite, no realizar compras y no mostrar `ELITE ACTIVE` a partir de datos locales no verificados.
- No hacer commit, push ni deploy.
- Preservar todos los cambios locales existentes.

---

### Task 1: Asset transparente y validación de recurso

**Files:**
- Source: `C:/Users/Anonymous/Pictures/ChatGPT Image Jul 15, 2026, 08_30_16 PM.png`
- Create: `app/src/main/res/drawable-nodpi/royal_elite_badge.png`
- Create: `app/src/test/java/com/royalshield/app/ui/components/RoyalEliteBadgeAssetTest.kt`

**Interfaces:**
- Consumes: imagen fuente aprobada.
- Produces: `R.drawable.royal_elite_badge`, PNG RGBA recortado sin fondo blanco ni franja lateral.

- [ ] **Step 1: Inspeccionar el original** con `view_image` y registrar dimensiones, franja lateral, fondo y límites que deben conservarse.
- [ ] **Step 2: Escribir la prueba RED del recurso** para cargar `app/src/main/res/drawable-nodpi/royal_elite_badge.png`, comprobar que existe, que su modelo de color contiene alpha, que las cuatro esquinas tienen alpha bajo y que conserva dimensiones mínimas de 512 px.
- [ ] **Step 3: Ejecutar la prueba RED** con `gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.components.RoyalEliteBadgeAssetTest"`; debe fallar porque el recurso aún no existe.
- [ ] **Step 4: Editar el PNG con `imagegen`** usando el original como objetivo, preservando literalmente corona, escudo, marco y texto; generar sobre chroma key plano, retirar el chroma con el helper oficial y copiar el resultado final a `drawable-nodpi` sin sobrescribir otros emblemas.
- [ ] **Step 5: Validar visual y técnicamente** el PNG final con `view_image` y la prueba enfocada; si el texto, la corona o el marco cambian, descartar el resultado y repetir una edición dirigida.

### Task 2: Componente Compose con estados honestos

**Files:**
- Create: `app/src/main/java/com/royalshield/app/ui/components/RoyalEliteBadge.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/components/RoyalEliteBadgePresentationTest.kt`

**Interfaces:**
- Produces:

```kotlin
enum class RoyalEliteBadgeMode { PROMOTIONAL, LOCKED, ACTIVE_VERIFIED }

data class RoyalEliteBadgePresentation(
    val mode: RoyalEliteBadgeMode,
    val statusText: String,
    val contentDescription: String,
    val alpha: Float,
    val showLock: Boolean
)

fun royalEliteBadgePresentation(mode: RoyalEliteBadgeMode): RoyalEliteBadgePresentation

@Composable
fun RoyalEliteBadge(
    mode: RoyalEliteBadgeMode,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 1: Escribir pruebas RED** que exijan `Preview of Royal Elite benefit` para `PROMOTIONAL`, `Royal Elite badge locked` y `showLock=true` para `LOCKED`, y `Royal Elite verified active badge` para `ACTIVE_VERIFIED`.
- [ ] **Step 2: Ejecutar la prueba RED** y confirmar que falla porque las APIs no existen.
- [ ] **Step 3: Implementar el mapper puro mínimo** y el composable black & gold que usa `R.drawable.royal_elite_badge`, brillo moderado, alpha reducido/candado para bloqueado y `contentDescription` accesible.
- [ ] **Step 4: Ejecutar prueba enfocada y `:app:compileDebugKotlin`**; ambas deben finalizar correctamente.

### Task 3: Integración promocional y bloqueada

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/PremiumEliteScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/LoyaltyProgramScreen.kt`
- Create: `app/src/test/java/com/royalshield/app/ui/screens/RoyalEliteBadgeUsageTest.kt`

**Interfaces:**
- Consumes: `RoyalEliteBadge(mode, modifier)`.
- Produces: vista previa comercial en Premium/Settings e insignia bloqueada en Loyalty.

- [ ] **Step 1: Escribir la prueba RED de consumo** que exija `PROMOTIONAL` en `PremiumEliteScreen` y `SettingsScreen`, `LOCKED` en `LoyaltyProgramScreen`, y que rechace `ACTIVE_VERIFIED` en esas rutas sin una entrada verificable.
- [ ] **Step 2: Ejecutar la prueba RED** y confirmar el fallo por ausencia de usos.
- [ ] **Step 3: Integrar el componente** sin cambiar callbacks, navegación, catálogo, precios, puntos ni lógica de compra; los textos visibles deben diferenciar `Preview`/`Locked` de estado activo.
- [ ] **Step 4: Ejecutar la prueba enfocada y compilación**; revisar el diff para confirmar que solo cambió presentación.

### Task 4: Gate de Dashboard y Profile

**Files:**
- Modify only after a verified source exists: `app/src/main/java/com/royalshield/app/ui/screens/DashboardScreen.kt`
- Modify only after a verified source exists: `app/src/main/java/com/royalshield/app/ui/screens/ProfileScreen.kt`
- Test: `app/src/test/java/com/royalshield/app/ui/components/RoyalEliteBadgePresentationTest.kt`

**Interfaces:**
- Consumes: una señal futura explícita `isEliteEntitlementVerified`, procedente de Play/backend verificado y no de `PreferencesManager` o `EntitlementStore` aislados.
- Produces: `ACTIVE_VERIFIED` únicamente cuando esa señal sea verdadera.

- [ ] **Step 1: Auditar la fuente actual** de `hasPremium`, `currentProduct` y `SubscriptionTier`; documentar si existe verificación server-side vigente.
- [ ] **Step 2: Si no existe verificación server-side**, no modificar Dashboard/Profile y registrar el bloqueo como `NO VERIFICADA`; mantener estas rutas fuera del lote.
- [ ] **Step 3: Si existe evidencia verificable**, escribir primero una prueba RED que demuestre que `false` no renderiza estado activo y `true` sí selecciona `ACTIVE_VERIFIED`; después realizar el cambio mínimo.

### Task 5: Regresión, APK y evidencia en Samsung

**Files:**
- Modify: `ROYAL_SHIELD_SIMULATION_WORKLOG_ENTRY_010_ROYAL_ELITE_BADGE.md`

**Interfaces:**
- Consumes: recurso y pantallas integradas.
- Produces: evidencia separada de fuente, build, instalación y visualización; Billing permanece `NO VERIFICADO E2E`.

- [ ] **Step 1: Ejecutar** `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 2: Ejecutar** `git diff --check` y diff enfocado; confirmar que Registration y su lote local no cambiaron.
- [ ] **Step 3: Instalar** con `adb -s RFCXC0DGQ4Z install -r app/build/outputs/apk/debug/app-debug.apk`; exigir `Success`.
- [ ] **Step 4: Revisar visualmente** Premium, Settings y Loyalty: transparencia, texto íntegro, sin franja lateral, sin clipping, y estados `Preview`/`Locked` inequívocos.
- [ ] **Step 5: Registrar** que Dashboard/Profile activos y compra/restauración permanecen pendientes mientras no haya entitlement verificable y build instalada desde Google Play.

