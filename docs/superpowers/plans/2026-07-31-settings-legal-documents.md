# Settings Legal Documents Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar Privacy Policy y Terms & Conditions completos desde sus botones actuales de Settings.

**Architecture:** Empaquetar ambos documentos como HTML local en `app/src/main/assets` y reemplazar el diálogo vacío por un visor WebView limitado, sin JavaScript. Settings decide qué asset cargar mediante `SettingsContentWindow` y conserva los callbacks existentes.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, assets HTML locales.

## Global Constraints

- No inventar texto legal, contactos ni capacidades.
- No habilitar JavaScript ni acceso a archivos externos en WebView.
- Conservar navegación y diseño black & gold.
- No commit, push ni deploy.

---

### Task 1: Empaquetar Privacy Policy

**Files:**
- Create: `app/src/main/assets/privacy_policy_embed.html`

- [ ] Copiar el documento entregado en UTF-8 y agregar metadatos mínimos de viewport/fondo.
- [ ] Confirmar que las once secciones y los enlaces de contacto están presentes.

### Task 2: Conectar ambos botones al visor legal

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/SettingsScreen.kt`
- Reuse: `app/src/main/assets/terms_embed.html`

- [ ] Asociar `PrivacyPolicy` con `privacy_policy_embed.html`.
- [ ] Asociar `TermsAndConditions` con `terms_embed.html`.
- [ ] Reemplazar el diálogo vacío por un diálogo amplio con WebView, borde dorado y cierre accesible.
- [ ] Mantener JavaScript y acceso a archivos/contenido desactivados.

### Task 3: Verificación

**Files:**
- Verify: `app/src/main/assets/privacy_policy_embed.html`
- Verify: `app/src/main/assets/terms_embed.html`
- Verify: `app/src/main/java/com/royalshield/app/ui/screens/SettingsScreen.kt`

- [ ] Ejecutar `:app:compileDebugKotlin`.
- [ ] Ejecutar `:app:testDebugUnitTest :app:assembleDebug`.
- [ ] Instalar el APK en el Samsung y abrir ambos documentos desde Settings.
- [ ] Ejecutar `git diff --check` y revisar el diff enfocado.
