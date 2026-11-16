# BYE PRO Finance — Native Android

Standalone Kotlin/Android implementation of BYE PRO Finance. Native development is isolated in this repository; the Flutter and web source repositories remain separate and must not be modified as part of Native-only work.

## Documentation index

These documents are the required reference for all development:

| Document | Covers |
|---|---|
| [Design System](docs/DESIGN_SYSTEM.md) | Mandatory colors, typography, spacing, dimensions, reusable components, RTL rules and UI review checklist |
| [Architecture](docs/ARCHITECTURE.md) | Project structure, data flow, SQLite compatibility, security and migration boundaries |\n| [Data Dictionary](docs/DATA_DICTIONARY.md) | Every SQLite table/column, relationships, dashboard formulas, financial validations and schema migration policy |
| [Development](docs/DEVELOPMENT.md) | Local build commands, CI, contribution workflow, testing and production release gate |
| [Implementation Status](docs/IMPLEMENTATION_STATUS.md) | Implemented work, known limitations and remaining release blockers |

**Development rule:** read the relevant document before changing code. All screen styling must use the shared Design System. Screen-local visual rules are not allowed.

## Current project baseline

- Native UI: Kotlin with Android framework Views.
- App destinations: Dashboard, Work, Home, Debts and AI.
- Appearance: System, Light and Dark; geometry remains the same across modes.
- Default language: Arabic. English changes text only; the interface remains RTL.
- Release application ID: `com.byepro.bye_pro_finance`.
- Current Native version configuration: `1.1.55 (156)`.
- Debug preview package suffix: `.nativepreview`, isolated from the production app sandbox.
- Database: compatible SQLite file `bye_pro_finance.sqlite`, schema version 2.

## Design System

The mandatory implementation contract is [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md). Its code entry points are:

- `FinanceDesignTokens` and `FinanceDesignSystem` in `ui/FinanceDesignSystem.kt`.
- Reusable components in `ui/FinanceComponents.kt`.
- Appearance modes in `ui/FinanceAppearance.kt`.
- Platform theme resources in `app/src/main/res/values`.

The Kotlin palette and Android resource theme bridge must remain aligned. Add new tokens and shared components centrally, with tests and documentation.

## Build and test

Requirements: JDK 17, Android SDK 36 and Gradle 9.5.0.

```bash
gradle :app:testDebugUnitTest --no-daemon
gradle :app:assembleDebug --no-daemon
```

The Native CI workflow runs unit tests and assembles the isolated debug preview APK. A successful debug build does not prove production signing, data continuity, login continuity or feature parity.

## Important migration boundary

The Native app is still being brought to feature parity. The AI section currently displays a migration notice, and the current Native calculator supports basic two-value addition only. Production replacement is blocked until signing, data/session continuity, backup/restore, financial regression coverage and real-device checks are verified.

See [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) for the evidence-based checklist and [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the release gate.
