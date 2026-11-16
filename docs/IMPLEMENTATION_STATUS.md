# Native Implementation Status

**Repository:** `abalsaleh0-cloud/bye-pro-finance-android-native`  
**Baseline:** standalone Kotlin Native extraction, version configured as `1.1.57 (158)`

This file separates implemented work from incomplete work. A task is not marked complete solely because code exists or a debug build succeeds.

## Implemented in the current source

- [x] Standalone Native Android project at repository root.
- [x] Kotlin Android Views implementation.
- [x] Five destinations: Dashboard, Work, Home, Debts and AI.
- [x] Central design token object for spacing, typography, radii and dimensions.
- [x] Shared Light and Dark semantic palettes.
- [x] Shared components for app bar, navigation, section heading, month selector, numbered editorial category index, metric row, record row, grouped list, card, empty state and divider.
- [x] Four Home routes in order: monthly bills, monthly rent (الإيجارات الشهرية), household expenses and received from Work. The fourth route reads real `work_home_transfers` records and displays their monthly total.
- [x] Five Work routes in order: Salary, Payment, Advance, Remaining and Home Transfers, with four Add actions only. Remaining is a calculated view.
- [x] Work and Home use the same numbered editorial index instead of segmented tabs.
- [x] Non-card Welcome and Login compositions with original finance-orbit artwork and open underlined login fields.
- [x] Local login initialized with `admin` / `admin` when no saved profile exists.
- [x] Settings flow to change username/password and enable or disable login protection.
- [x] Login profile persists in `app_settings`; password is stored only as a salted PBKDF2 verifier.
- [x] Automated four-route Home mapping tests, five-route Work mapping tests and local-authentication behaviour tests.
- [x] CI parity checker for Kotlin palette vs Android XML resources, core dimensions and screen-local styling guardrails.
- [x] Appearance modes: System, Light and Dark.
- [x] Arabic default strings and English string resources.
- [x] SQLite helper targeting the existing Flutter/Drift database file and schema version 2.
- [x] Repository separation: Native development is isolated from Flutter and web repositories.
- [x] Debug application ID suffix `.nativepreview`.
- [x] CI workflow for unit tests and Native debug APK assembly.

## Present but limited

- [ ] AI destination: currently a migration notice, not the completed assistant.
- [ ] Calculator: currently adds two entered values only; not parity with the richer Flutter calculator.
- [ ] UI test coverage: current unit tests cover design tokens, palette contrast and appearance rules; full screen/device screenshot coverage is not present.
- [ ] Financial-rule regression coverage: repository operations exist, but full automated coverage of all balance, debt, bill and payment paths is still required.
- [ ] Navigation and form interactions: require full device walkthrough in both themes and both languages.
- [ ] Welcome swipe, login keyboard/masking, login toggle on fresh process launch, all four Home routes/Add flows, all five Work routes/four Add actions and balance totals require real-device acceptance.

## Current CI status and immediate verification

- The latest completed run before the current changes (`43941131356`) passed unit tests and Kotlin compilation but exposed a false positive in the geometry/font-size guard. The guard was corrected to distinguish numeric drawing ratios from text sizes.
- The current run (`43942352286`) must pass unit tests, the Design System checker, application identity validation and APK assembly before the updated preview is considered verified.
- Native version is `1.1.57 (158)` for this development update. The current CI run (`43942352286`) is pending; do not mark the latest APK as verified until all steps pass. No production APK has been approved or issued.

## Not yet proven / release blockers

- [ ] Production signing key continuity verified against installed production APK.
- [ ] In-place update test with production application ID.
- [ ] Existing finance records verified after Native update using a protected test copy.
- [ ] Existing login/session state continuity verified.
- [ ] Backup and restore tested.
- [ ] Full feature parity review against the Flutter source.
- [ ] Real-device visual and functional acceptance.
- [ ] User approval for production release.

## Rules for updating this file

- Change `[ ]` to `[x]` only after the named check has passed.
- Add the evidence beside the item: test name, CI run, or device test record.
- Keep incomplete work visible. Do not describe a migration blocker as complete.
- Any new UI token or component must also be documented in [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).
