# BYE PRO Finance — Native Architecture and Data Contract

## 1. Purpose and boundaries

This repository is the standalone Kotlin/Android implementation of BYE PRO Finance. It is intentionally separated from the Flutter source repository:

- Native source: `abalsaleh0-cloud/bye-pro-finance-android-native`
- Flutter source remains separate: `abalsaleh0-cloud/bye-pro-finance-android-flutter`
- This Native repository must not modify, merge into, or replace the Flutter repository.
- The web project is outside this repository and is not modified by Native work.

The Native version is a separate implementation, not proof that all Flutter features have already been migrated.

## 2. Platform and application identity

| Setting | Current value |
|---|---|
| UI/platform | Kotlin with Android framework Views |
| Package / release application ID | `com.byepro.bye_pro_finance` |
| Namespace | `com.byepro.bye_pro_finance` |
| Current release version name | `1.1.55` |
| Current release version code | `156` |
| Minimum SDK | 30 |
| Compile SDK | 36 |
| Target SDK | 36 |
| Java target | 17 |
| Kotlin JVM target | 17 |
| Android Gradle Plugin | 9.3.1 |
| Kotlin Gradle Plugin | 2.4.21 |

The debug build adds `.nativepreview` to the application ID. This is deliberate: a debug preview installs alongside the production app and has a separate Android sandbox. It does not read the production app's private database or login state.

A release build has the production application ID. It must not be installed over an existing production app until signing-key continuity, database continuity, sign-in continuity, backups and device tests have all been demonstrated.

## 3. Source layout

| Path | Responsibility |
|---|---|
| `MainActivity.kt` | Single activity, screen navigation, screen composition, dialogs and user actions |
| `ui/FinanceDesignSystem.kt` | Shared design tokens, palette, typography and primitive view factories |
| `ui/FinanceComponents.kt` | Reusable app bar, navigation, rows, cards, month selector, empty state and dividers |
| `ui/FinanceAppearance.kt` | System / Light / Dark preference model |
| `ui/FinancePalette.kt` | Compatibility facade that delegates colors and shapes to the design system |
| `data/LegacyFinanceDatabase.kt` | Opens and manages the compatible SQLite database |
| `data/DashboardRepository.kt` | Reads financial records and constructs dashboard summaries |
| `data/FinanceWriteRepository.kt` | Validates and writes financial operations transactionally |
| `domain/FinanceModels.kt` | Shared data models exposed to the UI |
| `domain/HomeCategory.kt` | Exactly three Home categories and their data-source mapping |
| `app/src/main/res` | Android manifest, string translations, icons, theme and framework resources |
| `app/src/test` | Unit tests for design tokens, palettes, contrast, appearance, local login and Home category mapping |

## 4. Runtime structure and data flow

The current application uses a single `MainActivity` with five destinations: Dashboard, Work, Home, Debts and AI.

### Application-entry flow

1. `onCreate` initializes the existing database, appearance/language preferences and `LocalAuthentication` profile without resetting an existing profile.
2. `showWelcomeScreen` is the first visible screen for each fresh application process.
3. Continue button or horizontal swipe calls `continueFromWelcome`.
4. If local login is enabled, `showLoginScreen` verifies the stored username and password. Otherwise `enterApplication` opens the main shell.
5. The settings dialog exposes appearance, language and login security. Login security allows credential changes and enable/disable confirmation.

Read flow:

1. `MainActivity` owns the currently selected section, selected month, locale and appearance preference.
2. `DashboardRepository` reads records for the selected month and maps them to domain models and summaries.
3. The UI renders those models through the shared components in `FinanceComponents`.
4. The UI must not calculate or mutate stored financial records as part of view rendering.

Write flow:

1. A user action opens the corresponding form or confirmation.
2. The form validates required fields and calls `FinanceWriteRepository`.
3. The write repository applies domain rules and executes related SQL changes inside a database transaction.
4. On success, `MainActivity` refreshes the current screen; on a rule failure, it displays the mapped message.

The UI layer must not add direct SQL. Database queries and financial rules belong in the data layer.

## 5. SQLite compatibility contract

The Native database helper opens the same file used by the existing Flutter/Drift app:

- File: `bye_pro_finance.sqlite`
- Android directory: `context.getDir("flutter", Context.MODE_PRIVATE)`
- Current SQLite user version: `2`
- Foreign-key constraints: enabled when opening the database

The application ID and release signing lineage must remain compatible for an in-place upgrade to retain access to the same private app directory and database file. A debug preview with the `.nativepreview` suffix is isolated and cannot test this production-data path.

### Tables handled by the Native helper

| Table | Purpose |
|---|---|
| `work_entries` | Salary, advances and work payments |
| `work_home_transfers` | Transfers from work balance to home |
| `home_expenses` | Home expense records |
| `home_bills` | Bills and due amounts |
| `home_bill_payments` | Bill payment history |
| `debts` | Creditor and debtor records |
| `debt_payments` | Debt payment history |
| `app_settings` | Locale, appearance and device-local login profile/preferences |

Amounts are stored as integer minor units. Dates and timestamps are stored as epoch-millisecond values or as due year/month fields, as appropriate. Keep these storage units unchanged unless a versioned and tested migration is explicitly introduced.

For the complete field-by-field schema, relationships, dashboard formulas and validation invariants, see [DATA_DICTIONARY.md](DATA_DICTIONARY.md).

### Home category mapping

`domain/HomeCategory.kt` is the single domain contract for Home:

| Category | Stored source | Filter | Contents |
|---|---|---|---|
| Monthly Bills | `home_bills` | `kind = monthly` | Electricity, water, gas, internet, phone, recurring services and subscriptions |
| Monthly Rent (الإيجارات الشهرية) | `home_bills` | `kind = rent` | Rent obligations and installment/payment history |
| Expenses | `home_expenses` | No bill-kind filter | Groceries, utilities, housing, cleaning, maintenance, transport, personal, health and other |
| Received from Work (الوارد من العمل) | `work_home_transfers` | Selected month | Actual money transferred from Work into Home |

The Home numbered index and Add menu must use this mapping. "Received from Work" is a read-only view over existing transfer rows; the transfer is created from the Work Add menu. Work and Home retain distinct financial destinations.\n\n### Data-safety rules

- Never delete or recreate finance tables as part of a UI redesign.
- Never clear the database to fix a visual defect.
- Do not use SQLite `REPLACE` for parent rows when it could cascade-delete payment history. Update existing rows instead.
- Preserve debt and bill payment histories and foreign-key relationships.
- Keep the database version explicit. Any schema change requires a documented migration and tests against a copy of the previous database.
- The current startup cleanup removes only the three confirmed obsolete appearance keys: `theme_id`, `custom_themes`, and `appearance_preset`. It does not remove finance records or unknown settings.

## 6. Financial rules

`FinanceWriteRepository` currently owns operations for:

- Saving and deleting work entries.
- Saving and deleting transfers.
- Saving and deleting home expenses.
- Saving and deleting debts.
- Recording and undoing debt payments.
- Saving and deleting bills.
- Recording and undoing bill payments.

Domain rule failures are reported through `FinanceRuleException`. Examples include invalid amounts/months, insufficient work or home balance, payments exceeding a remaining balance, and attempts to delete records with payment history.

Do not bypass these validations from the UI. A new financial operation must be added to the write repository with transactional and rule-level tests before it is exposed in a form.

## 7. Appearance and localization

Appearance supports exactly three stored values:

- `system`
- `light`
- `dark`

Appearance and language are separate settings. Arabic is the default locale. English changes displayed resources only and does not mirror the fixed RTL layout. All new user-visible text must be added to both `values/strings.xml` and `values-en/strings.xml`.

The UI appearance contract is defined in [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).

## 8. Current migration status

The following items are **not to be represented as completed** merely because the Native APK compiles:

- The AI destination currently displays a migration notice; the full assistant is not migrated.
- The current Native calculator is a basic two-value addition form, not feature parity with the richer calculator in the Flutter implementation.
- A debug preview does not demonstrate production database access because it uses a separate package sandbox.
- Release signing is only configured to accept the existing key through local `key.properties` or environment variables; this repository does not contain the production key.
- Full sign-in continuity, backup/restore, device-level migration checks and complete financial-feature parity still require proof.

Record any completion in `IMPLEMENTATION_STATUS.md` only after tests or device validation have actually demonstrated it.

## 9. Security and secrets

Never commit:

- `key.properties`
- `*.jks`
- `*.keystore`
- passwords, signing aliases with secrets, API tokens or service credentials

Release signing values are read from these environment variables:

- `KEYSTORE_PATH`
- `KEYSTORE_PASSWORD`
- `KEY_PASSWORD`
- `KEY_ALIAS`

Relative keystore paths in `key.properties` resolve from the Native repository's `app/` directory. Store signing material securely outside source control.
### Work category mapping

`domain/WorkCategory.kt` defines five ordered routes: salary, payment, advance, remaining and home transfers. Salary/payment/advance read `work_entries` by kind; Remaining uses the existing monthly salary-outstanding calculation; Home Transfers read `work_home_transfers`. Work Add exposes only the four creation operations: salary, payment, advance and transfer to Home.
