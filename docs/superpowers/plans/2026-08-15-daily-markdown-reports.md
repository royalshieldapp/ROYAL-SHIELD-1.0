# Daily Markdown Reports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Maintain one redacted Markdown evidence report for every day Royal Shield work is performed.

**Architecture:** Reports live under `docs/daily-reports/`, use a fixed template, and are updated at meaningful checkpoints. A validation script checks required headings, timestamp format, and obvious sensitive-value patterns before completion.

**Tech Stack:** Markdown, PowerShell, Git, `rg`.

## Global Constraints

- Use `America/New_York` and timestamp format `yyyy-MM-dd HH:mm:ss zzz`.
- Never record tokens, credentials, phone numbers, precise locations, private email addresses, account identifiers, or raw provider bodies.
- A report records evidence but is not proof of deployment, provider delivery, or physical-device behavior.
- Do not commit reports without explicit authorization.

---

### Task 1: Template and validator

**Files:**
- Create: `docs/daily-reports/TEMPLATE.md`
- Create: `scripts/validate-daily-report.ps1`

**Interfaces:**
- Produces: `scripts/validate-daily-report.ps1 -Path <report>` returning exit code 0 only for structurally valid reports.

- [ ] **Step 1: Create the template with exact headings**

```markdown
# Royal Shield Daily Report — YYYY-MM-DD
## Objetivo del día
## Estado inicial
## Línea de tiempo
## Archivos modificados
## Validaciones realizadas
## Estado de funciones
## Riesgos o bloqueadores
## Próximo paso recomendado
```

- [ ] **Step 2: Implement validator checks for all headings and timestamps matching `^### [0-2][0-9]:[0-5][0-9]:[0-5][0-9]`**

- [ ] **Step 3: Add conservative scans for `Bearer `, API-key assignments, JWT-like strings, E.164 phone numbers, and latitude/longitude pairs; print only file and line number, not the matched secret**

- [ ] **Step 4: Verify the validator fails on an incomplete temporary fixture and passes on the template-derived fixture**

### Task 2: Create and maintain today's report

**Files:**
- Create: `docs/daily-reports/2026-08-15-royal-shield-daily-report.md`

**Interfaces:**
- Consumes: current Git state, commands, test outputs, and decisions from this session.

- [ ] **Step 1: Capture `git status --short --branch`, current commit, and current local time without recording secret values**

- [ ] **Step 2: Record the approved scope and existing Antigravity changes as preserved external work**

- [ ] **Step 3: Add one timeline entry for each completed task with exact command and pass/fail result**

- [ ] **Step 4: Classify Scan Files, SOS, and reporting as `REAL`, `SIMULADA`, `NO_CONFIGURADA`, or `NO_VERIFICADA` based on current evidence**

- [ ] **Step 5: Run `scripts/validate-daily-report.ps1` and `git diff --check`**

### Task 3: Daily operating procedure

**Files:**
- Modify: `README.md`
- Modify daily: `docs/daily-reports/YYYY-MM-DD-royal-shield-daily-report.md`

- [ ] **Step 1: Document that a report is created on the first work action of each local calendar day**

- [ ] **Step 2: Document checkpoint updates after code changes, verification, external checks, and blockers**

- [ ] **Step 3: Document end-of-session validation and redaction requirements**

- [ ] **Step 4: Verify the README links to the template and the current report**
