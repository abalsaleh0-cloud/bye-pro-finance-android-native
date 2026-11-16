# BYE PRO Finance Native — Requirements Traceability

**Repository:** `abalsaleh0-cloud/bye-pro-finance-android-native`  
**Development baseline:** `1.1.62 (163)`  
**Scope:** Native Android only. The Flutter and web source repositories are outside this change.

This document is the project-level evidence map. A requirement is not considered fully accepted merely because the Kotlin source exists; automated checks and real-device acceptance are listed separately.

## 1. Mandatory Design System

| Requirement | Implementation | Automated evidence | Acceptance status |
|---|---|---|---|
| One Design System for all app screens and surfaces | `ui/FinanceDesignSystem.kt`, `ui/FinanceComponents.kt`, `MainActivity.kt` | `FinanceDesignSystemTest`, `scripts/check_design_system.py` | Source integrated; real-device review remains required |
| No screen-local colors or sizes | Semantic color and dimension tokens; framework XML bridge | CI parity and screen guardrails | Must pass CI on the current branch |
| Light/Dark changes colors only | `FinanceAppearance.kt` and the shared palette | `FinanceAppearanceTest`, palette/contrast tests | Device review pending |
| Arabic default; English changes text only; RTL remains fixed | `values/strings.xml`, `values-en/strings.xml`, fixed RTL screens | Resource checks in `check_design_system.py` | Source integrated; device review pending |
| Shared controls for forms, fields, buttons, popups, dialogs, cards, navigation and empty states | `FinanceComponents.kt`, `FinanceDesignSystem.kt`, shared Android theme resources | Design-system unit tests and CI checker | Device review pending |

### Design System rules

- Every new visual value must be a semantic token, not a local numeric or color literal.
- A reusable visual pattern must be added to `FinanceComponents` before screen usage.
- A token/component change must include a unit test, a documentation update and a passing CI check.
- Screen geometry, component order, dimensions and navigation order must remain consistent between Light/Dark and Arabic/English.

## 2. Home section — exactly four primary categories

The Home section remains separate from Work. Its tab order is fixed and owned by `domain/HomeCategory.kt`; each destination is a separate Native icon tab, not a numbered index.

| Order | Main category | What it includes | Data source and filter | Add action |
|---:|---|---|---|---|
| 1 | **الفواتير الشهرية** | Electricity, water, gas, internet, phone, monthly services and subscriptions | `home_bills` where `kind = monthly` | Add Monthly Bill |
| 2 | **الإيجارات الشهرية** | Housing rent, amount due, paid installments and payment history | `home_bills` where `kind = rent` | Add Monthly Rent |
| 3 | **المصروفات** | Groceries, utilities, housing, cleaning, maintenance, transport, personal purchases, health and other household expenses | `home_expenses` | Add Expense |
| 4 | **الوارد من العمل** | Actual transfers received into Home, with a monthly total and the underlying transfer history | `work_home_transfers`, filtered by selected month; total from `DashboardSummary.transfersTotal` | Created in Work → Transfer to Home |

Implementation points:

- Category declaration and source mapping: `domain/HomeCategory.kt`.
- Independent Native icon tabs and data loading: `FinanceComponents.categoryTabs()` and `MainActivity.renderHome()`.
- The fourth route reads the existing Work transfer repository data; it does not create duplicate Home expense rows.
- Monthly/rent bill persistence: `FinanceWriteRepository.saveBill()`.
- Household expense persistence and category selection: `FinanceWriteRepository.saveHomeExpense()` and `MainActivity.showHomeExpenseForm()`.
- Arabic/English labels and descriptions: `res/values/strings.xml` and `res/values-en/strings.xml`.

Automated coverage:

- `HomeCategoryTest.homeHasExactlyFourCategoriesInTheRequiredOrder`
- `HomeCategoryTest.eachHomeCategoryUsesItsOwnDataSourceAndFilter`
- The design-system CI checker confirms four Home routes, five Work routes, Native icon-tab integration, raster icon mappings and bilingual route labels.

## 3. Welcome and local login

### Entry sequence

1. A fresh application process enters `MainActivity.onCreate()`.
2. Existing preferences and the local-authentication profile are loaded without replacing an existing credential profile.
3. `showWelcomeScreen()` is the first visible screen.
4. The user continues by pressing the shared Continue button or using the horizontal swipe gesture.
5. If login protection is enabled, `showLoginScreen()` appears; if disabled, the main application opens.
6. Login errors keep the user on the login screen.

### Initial profile

| Setting | Initial value |
|---|---|
| Username | `admin` |
| Password | `admin` |
| Login protection | Enabled |

The default credentials are created only when no complete local credential profile exists. They are bootstrap credentials and should be changed immediately after the first successful login.

### Change credentials and toggle login

Settings → Login and Security provides:

- Change username and password.
- Verify the current password before saving new credentials.
- Require username length of at least 3 characters.
- Require password length of at least 6 characters.
- Confirm the new password.
- Enable or disable login protection after a confirmation dialog.
- Preserve the existing credential profile when login protection is disabled.

### Password storage

`LocalAuthentication` stores a PBKDF2-HMAC-SHA256 verifier with a random 16-byte salt. The readable password is not stored. The local profile keys are documented in `DATA_DICTIONARY.md`.

### Automated coverage

- `LocalAuthenticationTest.firstLaunchCreatesRequestedDefaultCredentialsAndHashOnly`
- `LocalAuthenticationTest.credentialsCanBeChangedOnlyWithCurrentPassword`
- `LocalAuthenticationTest.credentialsValidationUsesDocumentedMinimumLengths`
- `LocalAuthenticationTest.loginCanBeDisabledAndReenabledWithoutLosingCredentials`
- `LocalAuthenticationTest.initializationDoesNotResetExistingCredentialsOrDisabledState`

## 4. Version and repository boundaries

- Native version for this development update: `1.1.62 (163)`.
- The debug APK uses the `.nativepreview` application ID suffix and a separate Android sandbox.
- Production application ID remains `com.byepro.bye_pro_finance`.
- Do not change the Flutter repository or the web source project.
- Do not merge or issue a production APK without explicit user approval.
- A debug preview does not prove production signing, shared private app storage, or in-place update compatibility.

## 5. Required real-device acceptance

These checks cannot be proven by static source inspection alone:

- [ ] Install and launch the preview APK on the target Android phone.
- [ ] Verify Welcome is first and both swipe and Continue work.
- [ ] Verify `admin` / `admin` login, incorrect-credential feedback and password masking.
- [ ] Change the username/password, force-stop/reopen, and verify the new values.
- [ ] Disable login, force-stop/reopen, and verify the login screen is skipped.
- [ ] Re-enable login, force-stop/reopen, and verify the saved credentials are required.
- [ ] Check all four Home categories and each Add action against the correct data source.
- [ ] Repeat in Light/Dark and Arabic/English without layout mirroring.
- [ ] Verify the existing finance data has not changed.
- [ ] Review touch targets, keyboard overlap, swipe recognition and long text on the actual screen size.

## 6. Current verification record

- Earlier CI run `43929573545` failed during Kotlin Gradle plugin resolution before tests ran. No source/test result was produced by that run.
- CI run `43933532090` on commit `c33cd65b209b79b5185ff01224074a9eaeb93144` passed: unit tests, Design System guardrails, application identity check, debug APK assembly and artifact upload.
- The preview artifact is `bye-pro-finance-native-preview` (ID `14278020563`, 1,039,064 bytes, expiry 2026-11-23).
- Physical-device acceptance remains open; CI does not prove touchscreen, keyboard, screen-size, real database migration, production signing or in-place update behaviour.


## 3. Work section — exactly five routes

| Order | Route | What it displays | Data source | Add action |
|---:|---|---|---|---|
| 1 | الراتب | Salary records | `work_entries.kind = salary` | Add Salary |
| 2 | الدفعة | Work payments | `work_entries.kind = payment` plus legacy `salaryPayment` | Add Payment |
| 3 | السلفة | Advances | `work_entries.kind = advance` | Add Advance |
| 4 | الباقي | Actual cash left in Work after household transfers, with the unpaid salary amount shown separately | `workRemaining = cashReceived - transfersTotal`; secondary `currentSalaryOutstanding = max(salary - payments - advances, 0)` | None — calculated view |
| 5 | تحويلات المنزل | Work-to-Home transfer history | `work_home_transfers` | Transfer to Home |

Work Add always exposes four actions in the requested order: salary, payment, advance, Home transfer. Both Work and Home use the same Native icon-tab component as separate tab sets; no numbered index or Flutter tab UI is used.


## Native tabs, icons and shared month tools

- Work and Home have separate horizontal tab rows designed natively; no Flutter tab layout is copied.
- Every tab has a unique icon and localized title. Monthly bills and rent are visually distinct. All nine expense categories have their own icons.
- Icon meanings are mapped from the Flutter icon choices, while this Native app uses generated PNG resources, not VectorDrawable artwork.
- Month choice is shared across Dashboard, Work, Home, Debts and AI, with both the available-month list and native calendar picker.
- Every form field has a persistent label in Arabic and English. Numeric entry accepts ASCII digits only (123) and remains LTR in both locales.
- The calculator supports scientific expressions, percentage, discount and amount splitting, with copy and insertion into the active form field.
