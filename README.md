# BYE PRO Finance — Native Android

Standalone Kotlin/Android implementation of BYE PRO Finance. Native development is isolated in this repository. The Flutter and web source repositories remain separate and must not be modified as part of Native-only work.

## Documentation index

Read the relevant contract before changing code.

| Document | Covers |
|---|---|
| [Design System](docs/DESIGN_SYSTEM.md) | Single visual source of truth: colors, typography, spacing, geometry, shared components, RTL and UI checklist |
| [Architecture](docs/ARCHITECTURE.md) | Runtime flow, layers, project structure, SQLite compatibility, security and migration boundaries |
| [Data Dictionary](docs/DATA_DICTIONARY.md) | SQLite tables/columns, relationships, login/settings keys, formulas and financial validation rules |
| [Login and Security](docs/LOGIN_AND_SECURITY.md) | Welcome flow, swipe/continue behaviour, initial credentials, credential changes and login toggle |
| [Requirements Traceability](docs/REQUIREMENTS_TRACEABILITY.md) | Each requested requirement mapped to source code, tests, documentation and remaining acceptance checks |
| [Migration Audit](docs/MIGRATION.md) | Source snapshot, file-by-file transfer audit, repository boundaries and migration evidence |
| [Changelog](CHANGELOG.md) | Version-by-version record of Native changes |
| [Development](docs/DEVELOPMENT.md) | Toolchain, local build commands, CI, contribution workflow and production release gate |
| [Implementation Status](docs/IMPLEMENTATION_STATUS.md) | Implemented work, proven checks, known limitations and release blockers |

## Project baseline

- Native UI: Kotlin with Android framework Views.
- Top-level destinations: **الرئيسية، العمل، المنزل، الديون، الذكاء الاصطناعي**.
- Home has four independent Native icon tabs: Monthly Bills, Monthly Rent, Expenses and From Work (actual Home transfers). Work has five independent Native icon tabs: Salary, Payment, Advance, Remaining and Home Transfers. Every tab has its own icon; the rejected numbered index is not used.
- Design: one shared Design System for all screens, dialogs, popups, forms, navigation and empty states.
- Appearance: System, Light and Dark. Only colors change; geometry stays fixed.
- Language: Arabic by default. English changes text only; the interface remains RTL.
- Release application ID: `com.byepro.bye_pro_finance`.
- Native version: `1.1.62 (163)`.
- Debug preview suffix: `.nativepreview`; its app sandbox is isolated from production.
- Database: compatible SQLite file `bye_pro_finance.sqlite`, schema version 2.

## Mandatory development rules

1. All UI styling must use `FinanceDesignTokens`, `FinanceDesignSystem`, and `FinanceComponents`. Do not introduce local colors, font sizes, spacing, radii, or control styles.
2. Every visible user-facing string must exist in both Arabic and English resources.
3. Work and Home remain separate sections. All four Home categories must keep their documented data-source mapping.
4. Financial reads belong in `DashboardRepository`; writes and validation belong in `FinanceWriteRepository`.
5. Any new component, token, data rule, or screen flow requires tests and documentation updates.
6. Do not change the Flutter repository or web source project during Native work.
7. Do not merge or issue a production release without the user's explicit approval.

## Main code entry points

- `ui/FinanceDesignSystem.kt`: semantic tokens, palettes and primitive view helpers.
- `ui/FinanceComponents.kt`: native icon tabs, raster icon pipeline, original finance-orbit entry art, editorial fields and shared UI components.
- `domain/HomeCategory.kt` and `domain/WorkCategory.kt`: four Home routes and five Work routes with data-source mapping.
- `data/LocalAuthentication.kt`: device-local login and password verifier.
- `MainActivity.kt`: navigation, welcome/login entry and screen composition.

## Navigation, month, icons and calculator

- Work and Home have separate Native horizontal tabs with five and four destinations respectively; no numbered index or Flutter tab layout is used.
- Tabs, bills, rent and all nine expense categories map to dedicated raster PNG icons generated from `app/src/main/assets/byepro-icons-sheet.png` by `scripts/generate_raster_icons.py`. VectorDrawable icon artwork is not used.
- The selected month is shared across all sections and persisted under `app_settings.selected_month`, matching the Flutter preference key.
- The native calendar picker and available-month list follow the Flutter month-selection semantics. Numeric input uses ASCII digits `0123456789` and remains LTR.
- The calculator supports expressions, percentages, discounts and split amounts, with history, copy and insertion into the active field.

## Build and test

Requirements: JDK 17, Android SDK 36, Gradle 9.5.0, Android Gradle Plugin 9.3.1 and Kotlin Gradle Plugin 2.4.21.

```bash
gradle :app:testDebugUnitTest --no-daemon
python3 scripts/check_design_system.py
gradle :app:assembleDebug --no-daemon
```

A passing debug build is not proof of production signing, data continuity, device-level UI correctness or feature parity. See the traceability and release documents before treating any stage as complete.
