# Native Android — Development and Release Procedure

## 1. Requirements

- Android Studio with Android SDK 36
- JDK 17
- Gradle 9.5.0 (CI installs this version)
- Android Gradle Plugin 9.3.1
- Kotlin Gradle Plugin 2.4.20

## 2. Build locally

Run from the repository root:

```bash
gradle :app:testDebugUnitTest --no-daemon
gradle :app:assembleDebug --no-daemon
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/
```

The debug package has the `.nativepreview` suffix and is isolated from the installed production app.

## 3. CI contract

Workflow: `.github/workflows/native-android-ci.yml`

On pushes to `main` and pull requests, CI:

1. Checks out the repository.
2. Sets up Java 17.
3. Installs Android SDK platform 36 and build tools 36.0.0.
4. Sets up Gradle 9.5.0.
5. Runs `:app:testDebugUnitTest`.
6. Runs `python3 scripts/check_design_system.py` to compare Kotlin palette values with Android XML resources, verify core dimensions, and reject screen-local styling.
7. Confirms the Native release application ID is `com.byepro.bye_pro_finance`.
8. Assembles the Native debug APK.
9. Uploads `bye-pro-finance-native-preview` for seven days.

A passing CI run demonstrates that the configured tests pass and the debug APK assembles. It does not demonstrate production signing, real-device correctness, production data visibility, or migration completeness.

## 4. Required workflow for every change

1. Identify whether the change affects UI, data, financial rules, build configuration, or migration.
2. Read the relevant contract in `DESIGN_SYSTEM.md`, `ARCHITECTURE.md`, and `DATA_DICTIONARY.md` for data/financial changes.
3. Make the smallest change in the Native repository only.
4. Add or update automated tests for the changed behaviour.
5. Update the relevant documentation and `IMPLEMENTATION_STATUS.md`.
6. Run the local commands above or rely on the corresponding completed CI checks.
7. Review both Light and Dark modes, Arabic and English, and the five navigation destinations on an Android device for UI changes.
8. Never merge or release a change that has not been explicitly approved for that purpose.

## 5. UI contribution checklist

- Use existing semantic tokens and shared components.
- No new direct hex colors or arbitrary per-screen styles.
- Do not change component sizes only on one screen.
- Keep RTL geometry and navigation order in both languages.
- Check form fields, popups, dialogs, empty states, icon touch areas and long values.
- Update [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) when adding a token or component.
- Extend `FinanceDesignSystemTest` when changing a token or palette rule.

## 6. Financial/data contribution checklist

- Keep reads in `DashboardRepository` and writes/rules in `FinanceWriteRepository`.
- Validate amounts, identifiers and month/year fields.
- Wrap multi-row writes in SQLite transactions.
- Preserve payment history and foreign-key relationships.
- Add tests for both valid and invalid financial cases.
- Do not change database schema without a versioned migration plan.
- Verify behaviour against a copy of the old database before any production migration.

## 7. Versioning and release gate

Current Native release configuration: `1.1.55 (156)`.

Every accepted app update must increment the version code; version name should also be updated according to the release sequence. Debug preview builds remain separate and are not production releases.

A production APK must not be issued until all of the following have been proven:

- The original production signing key and alias are available and verified.
- The release package ID and signing lineage match the installed app.
- Existing records, preferences and sign-in state survive an in-place update.
- Backup and restore have been tested.
- Financial workflows have regression coverage.
- AI integration is either completed or clearly excluded from the release scope.
- Real-device checks pass in Light/Dark and Arabic/English.
- The user has explicitly approved the release/merge step.

Never ask the user to share signing passwords or private keystore files in chat.
