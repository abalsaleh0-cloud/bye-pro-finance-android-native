# Changelog — BYE PRO Finance Native

## 1.1.72 (173) — Calculator parity correction

- Matched the Native calculator money-field insertion limit to Flutter: results are accepted only through the maximum exactly representable IEEE-754 integer (9,007,199,254,740,991).
- Added unit coverage for the safe-integer boundary and the first rejected value above it.
- Version code and version name incremented for this fix. The last verified debug preview remains 1.1.71 until this revision passes CI.

## 1.1.62 (163) — Native navigation, icons and verification

- Replaced the rejected numbered category index with independent Native icon tabs: five for Work and four for Home.
- Added dedicated icons for every route, bills, rent and all nine household-expense categories.
- Added a PNG sprite-sheet resource pipeline; icons are raster PNG resources and VectorDrawable icon artwork is not used.
- Persisted the shared selected month with the `selected_month` key used by the Flutter source.
- Numeric entry is restricted to ASCII digits and remains LTR in Arabic and English.
- Ported calculator expression, percentage, discount and split-amount behaviour to the Native calculator.
- CI run `43966393925` passed all 28 unit tests, the Design System checker, application ID check, debug APK assembly and artifact upload.
- Preview artifact: `bye-pro-finance-native-preview`, ID `14278020563`; expires 2026-11-23.
- This is a debug preview with the `.nativepreview` suffix, not a production release. Real-device acceptance and production signing continuity remain open.

## 1.1.57 (158) — Historical navigation and entry redesign

- Introduced the fifth Work route, fourth Home route and Welcome/Login entry screens.
- The first iteration used a shared numbered index; this was later rejected and replaced by separate Native icon tabs in 1.1.62.
- Existing finance tables, records and local-authentication profile are preserved by the source changes.
