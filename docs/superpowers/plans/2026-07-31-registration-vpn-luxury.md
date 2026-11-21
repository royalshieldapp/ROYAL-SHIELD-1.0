# Registration and VPN Luxury UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar el background dorado aprobado y la identidad black-and-gold a Registration, y transformar el control principal del VPN en un dial premium sin cambiar lógica funcional.

**Architecture:** Registration mantiene sus callbacks y estado actuales, pero compone el contenido sobre un nuevo drawable local y dos paneles decorativos reutilizables. VPN conserva `AurumPowerButton(vpnState, enabled, onClick)` y cambia exclusivamente su dibujo Canvas.

**Tech Stack:** Kotlin, Jetpack Compose, Canvas, Android drawables, Orbitron.

## Global Constraints

- No cambiar Firebase, autenticación, navegación, permisos, estados ni WireGuard.
- No tocar Splash, Dashboard ni Cyber Screen.
- Usar literalmente el copy aprobado.
- Sin commit, push o deploy.

---

### Task 1: Registration gold AI background and luxury panels

**Files:**
- Create: `app/src/main/res/drawable/bg_registration_gold_ai.png`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`

**Interfaces:**
- Consumes: `R.drawable.bg_registration_gold_ai`, `R.drawable.royal_shield_logo`, `Orbitron`.
- Produces: `LuxuryRegistrationPanel(modifier, content)` y el mismo `RegistrationScreen(onRegistrationSuccess, onNavigateToPhoneAuth)`.

- [ ] **Step 1: Copiar el background aprobado** desde el adjunto temporal a `bg_registration_gold_ai.png` sin recomprimirlo ni alterar sus píxeles.
- [ ] **Step 2: Sustituir solo el background de Registration** por `bg_registration_gold_ai` con `ContentScale.Crop`, manteniendo parallax y una capa negra vertical de alpha aproximado 0.32/0.48/0.68.
- [ ] **Step 3: Crear `LuxuryRegistrationPanel`** con fondo negro translúcido, esquinas de 28 dp, borde dorado exterior, línea interior dorada tenue y brillo mediante `drawBehind`.
- [ ] **Step 4: Aumentar el logo** a un tamaño responsive acotado entre 190 y 260 dp mediante `BoxWithConstraints`, sin recortarlo.
- [ ] **Step 5: Sustituir el encabezado** por el texto exacto `Powered by AI. Built for your safety.` en Orbitron dorado y el subtítulo exacto aprobado en blanco suave.
- [ ] **Step 6: Agrupar opciones de registro** dentro del panel luxury conservando cada callback actual y el scroll vertical.
- [ ] **Step 7: Ejecutar compilación enfocada** con `.\gradlew.bat :app:compileDebugKotlin`; debe terminar `BUILD SUCCESSFUL`.

### Task 2: VPN premium dial

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/VpnScreen.kt`

**Interfaces:**
- Consumes: `vpnStateColor(vpnState)` y los parámetros existentes `vpnState`, `enabled`, `onClick`.
- Produces: la misma firma `AurumPowerButton(vpnState: VpnState, enabled: Boolean, onClick: () -> Unit)`.

- [ ] **Step 1: Mantener intacta la lógica clickable** y el cálculo de estado/pulso.
- [ ] **Step 2: Dibujar dial concéntrico** con halo, bisel dorado segmentado, centro negro con gradiente visual, indicador superior y arcos/líneas laterales inspirados en la referencia.
- [ ] **Step 3: Mantener el icono Power** y derivar su color únicamente de `vpnStateColor` para que conexión/error sigan visibles.
- [ ] **Step 4: Ejecutar compilación enfocada** con `.\gradlew.bat :app:compileDebugKotlin`; debe terminar `BUILD SUCCESSFUL`.

### Task 3: Regression and device evidence

**Files:**
- Modify: `docs/superpowers/specs/2026-07-31-registration-vpn-luxury-design.md` solo si la implementación obliga a documentar una desviación aprobada.

**Interfaces:**
- Consumes: APK debug y Samsung autorizado por ADB.
- Produces: evidencia separada de build, instalación y revisión visual.

- [ ] **Step 1: Ejecutar suite y APK** con `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`; debe terminar `BUILD SUCCESSFUL`.
- [ ] **Step 2: Revisar alcance** con `git diff --check` y `git diff -- RegistrationScreen.kt VpnScreen.kt`; verificar que callbacks, Firebase y WireGuard no cambiaron.
- [ ] **Step 3: Instalar APK** con `adb -s RFCXC0DGQ4Z install -r app/build/outputs/apk/debug/app-debug.apk`; debe devolver `Success`.
- [ ] **Step 4: Abrir y capturar Registration** en dispositivo, comprobando background dorado, logo grande, copy exacto y paneles sin clipping.
- [ ] **Step 5: Navegar a VPN cuando sea posible** y comprobar visualmente el dial sin afirmar conectividad si no existe handshake/túnel.
