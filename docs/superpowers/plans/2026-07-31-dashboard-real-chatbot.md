# Dashboard Real Chatbot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer en el Dashboard un chat de texto que use el flujo autenticado existente y nunca produzca respuestas locales simuladas.

**Architecture:** `DashboardScreen` controla la visibilidad del diálogo. `VirtualAgentDialog` renderiza exclusivamente el `AssistantUiState` y delega envíos a `AssistantViewModel`; el ViewModel conserva historial acotado y `AssistantRepository` llama a `POST /api/assistant/chat` con token Firebase. Errores reales se muestran como errores y la UI bloquea envíos duplicados mientras hay una solicitud activa.

**Tech Stack:** Kotlin, Jetpack Compose, MVVM, StateFlow, Firebase Authentication, JUnit/Gradle.

## Global Constraints

- Mantener el diseño black & gold y no rediseñar el Dashboard.
- No cambiar el asistente de voz `DidAgentDialog`.
- No añadir respuestas fijas, retrasos de simulación ni éxitos falsos.
- No incluir secretos, URLs privadas ni credenciales.
- No hacer commit, push o deploy sin autorización explícita.

---

### Task 1: Estado del chat comprobable

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/viewmodels/AssistantViewModel.kt`
- Test: `app/src/test/java/com/royalshield/app/ui/viewmodels/AssistantViewModelTest.kt`

**Interfaces:**
- Consumes: `AssistantRepository.sendMessage(message: String, history: List<AssistantHistoryEntry>)`
- Produces: `AssistantViewModel.sendMessage(message: String)` y `AssistantUiState(messages, isSending)`

- [ ] **Step 1: Escribir prueba fallida** que compruebe: entrada vacía ignorada, mensaje normalizado añadido, envío bloqueado durante solicitud y resultado real/error añadido al estado.
- [ ] **Step 2: Ejecutar prueba y verificar RED** con `./gradlew.bat :app:testDebugUnitTest --tests "com.royalshield.app.ui.viewmodels.AssistantViewModelTest"`.
- [ ] **Step 3: Implementar el mínimo** para permitir una dependencia de repositorio comprobable sin cambiar el contrato público del Dashboard.
- [ ] **Step 4: Ejecutar prueba y verificar GREEN** con el mismo comando.

### Task 2: Diálogo Compose conectado al estado real

**Files:**
- Modify: `app/src/main/java/com/royalshield/app/ui/components/VirtualAgentDialog.kt`
- Modify: `app/src/main/java/com/royalshield/app/ui/screens/DashboardScreen.kt`

**Interfaces:**
- Consumes: `AssistantUiState`, `AssistantMessage`, `AssistantMessageRole`, `AssistantViewModel.sendMessage(String)`
- Produces: `VirtualAgentDialog(onDismiss: () -> Unit, viewModel: AssistantViewModel = viewModel())`

- [ ] **Step 1: Eliminar el estado local simulado** (`AgentMessage`, `delay`, `onSendMessage`, `getAgentResponse`) y representar mensajes del ViewModel.
- [ ] **Step 2: Mostrar estados honestos**: indicador mientras envía, burbuja visual de error para `ERROR`, campo/botón deshabilitados durante envío y sin saludo que afirme conectividad.
- [ ] **Step 3: Añadir entrada visible de chat de texto** junto al control del asistente de voz sin alterar sus rutas ni acciones.
- [ ] **Step 4: Ejecutar compilación enfocada** con `./gradlew.bat :app:compileDebugKotlin`.

### Task 3: Validación y evidencia

**Files:**
- Modify: `ROYAL_SHIELD_SIMULATION_WORKLOG_ENTRY_009_DASHBOARD_REAL_CHATBOT.md`

- [ ] **Step 1: Ejecutar** `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
- [ ] **Step 2: Ejecutar lint** con `./gradlew.bat :app:lintDebug`; si falla por deuda ajena, separar claramente el resultado.
- [ ] **Step 3: Inspeccionar** `git diff --check` y `git diff` preservando cambios previos.
- [ ] **Step 4: Si ADB está autorizado**, instalar el APK, abrir `com.royalshield.app/.MainActivity` y validar una solicitud autenticada; registrar por separado fuente, build, instalación y E2E.
