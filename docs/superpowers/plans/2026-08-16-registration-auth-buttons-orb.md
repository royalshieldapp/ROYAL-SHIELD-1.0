# Registration Auth Buttons and Orb Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar assets premium a Registration sin habilitar accesos falsos y aumentar ligeramente el orbe.

**Architecture:** `RegistrationScreen` seguirá concentrando la composición visual y delegará disponibilidad a `RegistrationAccessPolicy`. Los fondos e iconos serán drawables locales sin secretos ni dependencias nuevas.

**Tech Stack:** Kotlin, Jetpack Compose, Android resources, JUnit.

## Global Constraints

- No rediseñar la estructura de Registration.
- No habilitar Instagram, Facebook, GitHub, Apple o email sin autenticación real.
- Preservar los cambios locales existentes y no hacer commit, push ni deploy.

---

### Task 1: Assets y contrato visual

**Files:**
- Create: `app/src/main/res/drawable-nodpi/bg_button_gold.png`
- Create: `app/src/main/res/drawable-nodpi/ic_registration_*_gold.png`
- Modify: `app/src/test/java/com/royalshield/app/ui/screens/RegistrationAccessPolicyTest.kt`

- [ ] Escribir una prueba que exija que Instagram no se ofrezca y que los proveedores sin integración sigan deshabilitados.
- [ ] Ejecutar la prueba y comprobar el fallo esperado si la política no cumple.
- [ ] Preparar los assets transparentes y aplicar la mínima política necesaria.
- [ ] Ejecutar la prueba enfocada hasta obtener PASS.

### Task 2: Botones y orbe

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/RegistrationScreen.kt`

- [ ] Escribir una prueba de presentación que requiera un fondo compartido y el tamaño de orbe aprobado.
- [ ] Ejecutarla y comprobar el fallo esperado.
- [ ] Renderizar el PNG como fondo de los botones, actualizar iconos y cambiar el orbe a 170 dp.
- [ ] Ejecutar pruebas unitarias y `assembleDebug`.

### Task 3: Inicio de sesión

**Files:**
- Modify only after reproduction: authentication files implicated by logs/tests.

- [ ] Reproducir el fallo en dispositivo o con un error concreto.
- [ ] Crear una prueba que reproduzca la causa.
- [ ] Implementar únicamente la corrección confirmada.
- [ ] Verificar inicio de sesión real y navegación posterior.
