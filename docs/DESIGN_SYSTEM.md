# BYE PRO Finance — Native Design System

**Status:** Required implementation contract  
**Scope:** Every screen and every Android UI surface in this repository  
**Code owners:** `FinanceDesignTokens`, `FinanceDesignSystem`, and `FinanceComponents`

This document is the binding visual and interaction specification for the Native Android application. The screen implementation must follow it; a screen must not invent its own palette, typography, dimensions, control shape, or component style.

## 1. Non-negotiable principles

1. **One system, all screens.** Dashboard, Work, Home, Debts, AI, settings, forms, dialogs, popups, empty states, and navigation use the same tokens and shared components.
2. **No one-off styling.** Do not introduce a screen-specific color, text size, radius, padding, button height, field treatment, or icon size.
3. **Light and Dark are color modes only.** Changing appearance must not rearrange pages, mirror the UI, change component sizes, or change interaction behaviour.
4. **Language and direction are separate.** Arabic is the default language and the layout remains RTL. English changes text only; it must not reverse or mirror the whole interface.
5. **Components before custom views.** Reuse `FinanceComponents`. A new reusable visual pattern must be added there before screens use it.
6. **Document and test every system change.** A new token, component, color, or exception requires an update to this document and a test or CI check.

## 2. Source of truth

| Responsibility | Authoritative file |
|---|---|
| Spacing, dimensions, typography, radii, timing | `app/src/main/java/com/byepro/bye_pro_finance/ui/FinanceDesignSystem.kt` — `FinanceDesignTokens` |
| Light and Dark semantic palettes; text, shape, input, button, ripple and surface helpers | Same file — `FinanceDesignSystem` |
| Shared visual components | `app/src/main/java/com/byepro/bye_pro_finance/ui/FinanceComponents.kt` |
| Appearance modes and obsolete preference cleanup | `FinanceAppearance.kt` |
| Compatibility facade for older callers | `FinancePalette.kt` |
| Android framework theme resources | `app/src/main/res/values/colors.xml`, `dimens.xml`, `styles.xml`, and `values-night/styles.xml` |

The Kotlin palette is the visual contract for app-created views. Android XML resources are the framework bridge used by native dialogs, popup themes, and platform-owned controls. Their semantic names and values must stay aligned with the Kotlin palette. Do not add literal color values to individual layouts or screens.

## 3. Color system

All colors are semantic. Screens must request a role from `FinanceDesignSystem.colors(isDark)`; they must not parse their own hex values.

### Dark palette

| Role | Hex | Usage |
|---|---:|---|
| Background | `#17212B` | Main window and page background |
| Surface | `#202B36` | Grouped lists, navigation and dialogs |
| Raised | `#263442` | Inputs, tracks and nested control surfaces |
| Text | `#F2F5F8` | Primary content |
| Secondary text | `#91A0AE` | Captions, subtitles and secondary labels |
| Accent | `#5AA7E8` | Primary actions and selected states |
| On accent | `#17212B` | Text/icons on accent-filled controls |
| Soft accent | `#28445D` | Quiet selected backgrounds |
| Divider | `#2D3A47` | Separators |
| Danger | `#E06C75` | Destructive and warning states |
| Positive | `#63B88C` | Positive financial state |

### Light palette

| Role | Hex | Usage |
|---|---:|---|
| Background | `#F1F3F5` | Main window and page background |
| Surface | `#FFFFFF` | Cards, grouped lists and navigation |
| Raised | `#E9EEF3` | Inputs and nested control surfaces |
| Text | `#17212B` | Primary content |
| Secondary text | `#626F7A` | Captions and secondary labels |
| Accent | `#236DAB` | Primary actions and selected states |
| On accent | `#FFFFFF` | Text/icons on accent-filled controls |
| Soft accent | `#DCEBFA` | Quiet selected backgrounds |
| Divider | `#E5E9ED` | Separators |
| Danger | `#D9535D` | Destructive and warning states |
| Positive | `#23885C` | Positive financial state |

**Rules:** no gradients, decorative neon, arbitrary transparency, per-screen accent colors, or theme catalogues. Danger and positive colors are reserved for meaning, not decoration. Text must pass the contrast assertions in `FinanceDesignSystemTest`.

## 4. Typography

Use semantic typography tokens from `FinanceDesignTokens`. These sizes are in sp.

| Token | Size | Intended use |
|---|---:|---|
| `TEXT_NAVIGATION_SP` | 11 | Bottom navigation labels |
| `TEXT_CAPTION_SP` | 12 | Supporting text, subtitles and metadata |
| `TEXT_LABEL_SP` | 13 | Compact labels |
| `TEXT_BODY_SP` | 14 | Standard body copy and form values |
| `TEXT_INPUT_SP` | 15 | Input text |
| `TEXT_ACTION_SP` | 15 | Button/action text |
| `TEXT_AMOUNT_SP` | 15 | Record amounts |
| `TEXT_VALUE_SP` | 16 | Financial metric values |
| `TEXT_SECTION_TITLE_SP` | 16 | Section headings |
| `TEXT_PAGE_TITLE_SP` | 20 | Main app-bar title |

Use `FinanceDesignSystem.text` or the relevant shared component. Do not set `TextView.textSize` directly in a screen. Weight is limited to normal and medium/bold emphasis; do not introduce decorative font families.

## 5. Spacing and geometry

All dimensions are dp unless the token name ends in `_SP` or explicitly says otherwise.

### Spacing scale

| Token | Value |
|---|---:|
| `SPACE_2` | 2 |
| `SPACE_4` | 4 |
| `SPACE_5` | 5 |
| `SPACE_6` | 6 |
| `SPACE_8` | 8 |
| `SPACE_12` | 12 |
| `SPACE_14` | 14 |
| `SPACE_16` | 16 |
| `SPACE_18` | 18 |
| `SPACE_24` | 24 |

### Structural sizes

| Element | Token | Value |
|---|---|---:|
| Page horizontal inset | `PAGE_HORIZONTAL_PADDING` | 16 |
| Page vertical inset | `PAGE_VERTICAL_PADDING` | 12 |
| Standard section gap | `SECTION_SPACING` | 12 |
| Card inner padding | `CARD_PADDING` | 16 |
| App bar height | `APP_BAR_HEIGHT` | 56 |
| Card radius | `CARD_RADIUS` | 16 |
| Control radius | `CONTROL_RADIUS` | 12 |
| Dialog radius | `DIALOG_RADIUS` | 20 |
| Button / standard field height | `BUTTON_HEIGHT` | 48 |
| Minimum touch target | `TOUCH_TARGET_SIZE` | 44 |
| Bottom navigation height | `NAVIGATION_BAR_HEIGHT` | 68 |
| Section heading height | `SECTION_HEADER_HEIGHT` | 36 |
| Metric and record row minimum height | `METRIC_TILE_MIN_HEIGHT` / `RECORD_ROW_HEIGHT` | 64 |
| Icon container | `RECORD_ICON_BOX_SIZE` | 40 |
| Standard icon | `STANDARD_ICON_SIZE` | 20 |
| Divider thickness | `DIVIDER_HEIGHT` | 1 |
| Motion duration | `MOTION_DURATION_MS` | 160 ms |

Multiline fields and comparison bars have dedicated tokens in the same object. Add a token there instead of repeating a new numeric value in screen code.

## 6. Approved component catalogue

Use these components; do not recreate their visual styling inside a screen.

| Component | API | Purpose |
|---|---|---|
| App bar | `FinanceComponents.topBar` | Page title and consistent icon actions |
| Bottom navigation | `FinanceComponents.bottomNavigation` | Five equal destinations with a shared selected/unselected state |
| Section heading | `FinanceComponents.sectionHeader` | Consistent section hierarchy |
| Month selector | `FinanceComponents.monthSelector` | Caption plus selected month and shared touch target |
| Metric row | `FinanceComponents.metricRow` | Financial label, icon and amount |
| Record row | `FinanceComponents.recordRow` | Transaction/debt/bill row and optional actions |
| Grouped list | `FinanceComponents.groupedList` | Shared list surface with consistent separators |
| Content card | `FinanceComponents.card` | Padded surface for chart/panel content |
| Empty state | `FinanceComponents.emptyState` | Shared empty-result message treatment |
| Divider | `FinanceComponents.bottomDivider` | Shared one-pixel separator |
| Text, input, button, surface and icon helpers | `FinanceDesignSystem` | Primitive styling building blocks |

### Component rules

- The bottom navigation always contains exactly five destinations, in this order: **الرئيسية، العمل، المنزل، الديون، الذكاء الاصطناعي**.
- Each destination receives equal width. Selection is communicated with the accent color; no screen may invent its own selection treatment.
- Grouped list rows keep their own consistent horizontal inset. Do not wrap them in an additional padded card.
- Form fields use `FinanceDesignSystem.input`; actions use `FinanceDesignSystem.button` when a custom action control is needed.
- Native AlertDialogs and PopupMenus must use the shared light/dark styles and the shared dialog background drawables.
- Month selection is a text/title control, not a large dashboard card.
- Reusable additions must be implemented in `FinanceComponents` and covered by tests before being used by multiple screens.

## 7. Screen composition rules

The app has five top-level destinations:

1. **الرئيسية (Dashboard):** monthly selector, financial metric groups, cash-flow comparison and recent activity.
2. **العمل (Work):** salary, advances and work-payment records.
3. **المنزل (Home):** transfers, home expenses and bills.
4. **الديون (Debts):** creditor/debtor records and payment actions.
5. **الذكاء الاصطناعي (AI):** the current Native build displays a migration notice; full assistant integration is not yet complete.

A section may arrange approved components differently to suit its data, but may not create a new visual language. Keep Work and Home as separate destinations; do not combine their data into one section.

## 8. Interaction, RTL and accessibility

- Arabic is the default locale. English is a translation-only switch; keep RTL geometry and the same navigation order.
- Every icon-only action must have a meaningful content description.
- Controls must be at least 44dp in the touch dimension; standard buttons and fields are 48dp high.
- Provide visible feedback using the shared ripple/accent treatment.
- Preserve screen state and selected month across normal recreation where already supported.
- Do not change financial values or persistence logic as part of a visual styling change.
- Keep labels readable, avoid truncation of amounts, and allow long transaction titles to wrap where appropriate.
- Test both Light and Dark modes and both languages at phone-sized widths.

## 9. Prohibited patterns

The following are design-system violations:

- Hex/RGB color literals in screen code or one-off XML styles.
- Raw dp/sp dimensions in screen code where a token exists.
- Direct styling of screen-specific `TextView`, `EditText`, backgrounds, radii or button states instead of shared helpers.
- A new card, button, input, popup or dialog treatment used on only one screen without a documented and approved shared component.
- Extra theme systems, per-screen themes, color palettes or layout mirroring when English is selected.
- Duplicating a shared component to make a small local visual adjustment.

If a legitimate new use case cannot fit the catalogue, add a semantic token/component, update this document, add a test, and only then use it.

## 10. Required review checklist

Before any UI change is considered complete:

- [ ] All affected views use shared tokens and approved components.
- [ ] No new screen-local color, text size, dp value, radius or button style was introduced.
- [ ] Light and Dark appearances retain identical geometry.
- [ ] Arabic and English retain the same RTL layout and section order.
- [ ] Navigation still has exactly five destinations in the approved order.
- [ ] All dialogs, popups, fields, buttons, empty states and icons follow the catalogue.
- [ ] Contrast and design-token unit tests pass.
- [ ] Native debug APK builds successfully.
- [ ] Documentation is updated for every new token or component.

**No exception is implied by a passing build.** A build proves compilation; it does not prove visual consistency on a device.
