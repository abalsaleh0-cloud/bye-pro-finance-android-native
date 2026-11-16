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

**Rules:** no gradients, decorative neon, arbitrary transparency, per-screen accent colors, or theme catalogues. Danger and positive colors are reserved for meaning, not decoration. Text must pass the contrast assertions in `FinanceDesignSystemTest`. CI also runs `scripts/check_design_system.py` to compare the Kotlin palette with Android XML color resources and validate the shared core dimensions.

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

All dimensions are dp unless the token name ends in `_SP` or explicitly says otherwise. This section is the complete public inventory of `FinanceDesignTokens`; do not introduce another numeric style system.

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

### Page, component and input geometry

| Token | Value | Meaning |
|---|---:|---|
| `PAGE_HORIZONTAL_PADDING` | 16 | Horizontal page inset |
| `PAGE_VERTICAL_PADDING` | 12 | Vertical page inset |
| `SECTION_SPACING` | 12 | Standard spacing between sections |
| `CARD_PADDING` | 16 | Inner padding for content cards |
| `INPUT_HORIZONTAL_PADDING` | 14 | Horizontal text-field inset |
| `INPUT_VERTICAL_PADDING` | 12 | Vertical text-field inset |
| `BUTTON_HORIZONTAL_PADDING` | 18 | Horizontal action-button inset |
| `COMPONENT_HORIZONTAL_PADDING` | 12 | Standard row/empty-state horizontal inset |
| `COMPONENT_VERTICAL_PADDING` | 8 | Standard metric/record-row vertical inset |
| `MULTILINE_INPUT_HEIGHT` | 80 | Minimum multiline field height |

### App structure and controls

| Token | Value | Meaning |
|---|---:|---|
| `APP_BAR_HEIGHT` | 56 | Top app bar |
| `CARD_RADIUS` | 16 | Standard card/grouped-list corners |
| `CONTROL_RADIUS` | 12 | Inputs, buttons and icon-container corners |
| `DIALOG_RADIUS` | 20 | Dialog background corners |
| `BUTTON_HEIGHT` | 48 | Standard action-button and input height |
| `TOUCH_TARGET_SIZE` | 44 | Minimum touch target |
| `SECTION_TAB_HEIGHT` | 48 | Standard section-tab control height |
| `SECTION_HEADER_HEIGHT` | 36 | Section heading row |
| `METRIC_TILE_MIN_HEIGHT` | 64 | Minimum financial metric row |
| `RECORD_ROW_HEIGHT` | 64 | Minimum transaction row |
| `RECORD_ICON_BOX_SIZE` | 40 | Icon container within a record/metric |
| `STANDARD_ICON_SIZE` | 20 | Standard icon glyph |
| `DIVIDER_HEIGHT` | 1 | Separator thickness |
| `LIST_DIVIDER_START_INSET` | 56 | Start inset for grouped-list separators |
| `EMPTY_STATE_VERTICAL_PADDING` | 24 | Vertical empty-state breathing room |

### Bottom navigation geometry

| Token | Value | Meaning |
|---|---:|---|
| `NAVIGATION_BAR_HEIGHT` | 68 | Full reserved navigation height |
| `NAVIGATION_HORIZONTAL_PADDING` | 4 | Horizontal outer inset |
| `NAVIGATION_VERTICAL_PADDING` | 4 | Vertical outer inset |
| `NAVIGATION_ITEM_PADDING` | 2 | Each destination's inner inset |
| `NAVIGATION_ICON_SLOT_WIDTH` | 38 | Icon slot width |
| `NAVIGATION_ICON_SLOT_HEIGHT` | 30 | Icon slot height |
| `NAVIGATION_LABEL_HEIGHT` | 20 | Navigation label height |
| `NAVIGATION_CONTENT_HEIGHT` | 60 | Item content height after outer vertical padding |

### Comparison-chart geometry

| Token | Value | Meaning |
|---|---:|---|
| `CHART_LABEL_ROW_HEIGHT` | 24 | Label/value row |
| `CHART_BAR_HEIGHT` | 7 | Comparison track/fill height |
| `CHART_BAR_RADIUS_DP` | 5 | Track/fill corner radius |
| `CHART_BAR_TOP_GAP` | 5 | Space before the track |
| `CHART_HORIZONTAL_INSET` | 72 | Available-width adjustment |
| `CHART_MIN_VISIBLE_FILL` | 3 | Minimum visible non-zero fill |

### Motion

| Token | Value | Meaning |
|---|---:|---|
| `MOTION_DURATION_MS` | 160 ms | Standard short UI transition |

If a new value is needed, add a semantic token in `FinanceDesignTokens`, add an assertion to `FinanceDesignSystemTest`, update this inventory, and ensure the CI parity checker still passes. Never copy a raw number into a screen as a local shortcut.

## 6. Approved component catalogue

Use these components; do not recreate their visual styling inside a screen.

| Component | API | Purpose |
|---|---|---|
| App bar | `FinanceComponents.topBar` | Page title and consistent icon actions |
| Bottom navigation | `FinanceComponents.bottomNavigation` | Five equal destinations with a shared selected/unselected state |
| Section heading | `FinanceComponents.sectionHeader` | Consistent section hierarchy |
| Month selector | `FinanceComponents.monthSelector` | Selected month, saved-month list and calendar action shared by all main sections |
| Metric row | `FinanceComponents.metricRow` | Financial label, icon and amount |
| Record row | `FinanceComponents.recordRow` | Transaction/debt/bill row and optional actions |
| Grouped list | `FinanceComponents.groupedList` | Shared list surface with consistent separators |
| Content card | `FinanceComponents.card` | Padded surface for chart/panel content |
| Empty state | `FinanceComponents.emptyState` | Shared empty-result message treatment |
| Category tabs | `FinanceComponents.categoryTabs` | Independent horizontal Native tabs, each with its own icon and translated label |
| Category description | `FinanceComponents.categoryDescription` | Consistent explanatory text below the Home category selector |
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
2. **العمل (Work):** five independent routes using the separate Native icon tabs:
   - **الراتب:** salary records for the selected month.
   - **الدفعة:** payments deducted from salary, including legacy `salaryPayment` records.
   - **السلفة:** advances recorded against salary.
   - **الباقي:** main figure is cash remaining in Work after transfers to Home; the unpaid portion of salary (salary minus payments and advances) appears as a secondary figure. It is a view, not an Add action.
   - **تحويلات المنزل:** work-to-home transfer records.
3. **المنزل (Home):** four independent routes using the same icon-bearing Native tabs:
   - **الفواتير الشهرية:** electricity, water, gas, internet, phone, recurring services and subscriptions. Reads `home_bills.kind = monthly`.
   - **الإيجارات الشهرية:** housing rent obligations, due amount, paid installments and payment history. Reads `home_bills.kind = rent`.
   - **المصروفات:** groceries, utilities, housing, cleaning, maintenance, transport, personal purchases, health and other household expenses. Reads `home_expenses`.
   - **الوارد من العمل:** monthly sum and individual records actually transferred from Work to Home. Reads `work_home_transfers`, filtered to the selected month; the total comes from `DashboardSummary.transfersTotal`.

   Work Add always offers salary, payment, advance and transfer to Home. Home Add always offers monthly bill, monthly rent and expense, regardless of the selected route. The "Remaining" and "From Work" routes display existing or calculated data and do not create another money record.
4. **الديون (Debts):** creditor/debtor records and payment actions.
5. **الذكاء الاصطناعي (AI):** the current Native build displays a migration notice; full assistant integration is not yet complete.

A section may arrange approved components differently to suit its data, but may not create a new visual language. Keep Work and Home as separate destinations; do not combine their data into one section.

### Signature Welcome and Login entry surfaces

- Every fresh process starts on Welcome.
- Welcome is an editorial, full-surface composition: BYE PRO signature, original finance-orbit artwork, strong statement typography, a progress rule and an entry action. It is not a centered card.
- Login has a separate access-focused composition, the same custom artwork language and open underlined fields. It is not a generic centered login card or boxed stock form.
- The Welcome action and horizontal swipe both continue. Login follows only while local login protection is enabled.
- The artwork is drawn natively from the shared light/dark semantic palette; Arabic/English keep the same RTL geometry.

## 8. Interaction, RTL and accessibility

- Arabic is the default locale. English is a translation-only switch; keep RTL geometry and the same navigation order.
- Every icon-only action must have a meaningful content description.
- Controls must be at least 44dp in the touch dimension; standard buttons and fields are 48dp high.
- Provide visible feedback using the shared ripple/accent treatment.
- Preserve screen state and selected month across normal recreation where already supported.
- Do not change financial values or persistence logic as part of a visual styling change.
- Keep labels readable, avoid truncation of amounts, and allow long transaction titles to wrap where appropriate.
- Test both Light and Dark modes and both languages at phone-sized widths.
- Check the Welcome swipe and Continue button, Login error state, masked password field, and the exact three Home categories.
- Confirm each Home category's Add action writes to its correct data source.

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


## 11. Native tabs, icons, month and entry requirements

- Work and Home render separate horizontal rows of independent Native tabs. Numbered editorial indexes, shared index rails, and Flutter tab layouts are not used.
- Work routes are Salary, Payment, Advance, Remaining and Home Transfers. Home routes are Monthly Bills, Monthly Rent, Expenses and Received from Work.
- Every route has its own icon. Bill and rent use different icons. Each of the nine expense categories has its own icon.
- Material icon semantics were selected from the Flutter source, but the Native app ships raster PNG resources only. Gradle extracts the compact icon sheet into generated drawable-nodpi PNG resources. No VectorDrawable icon resources remain.
- The selected-month row appears on Dashboard, Work, Home, Debts and AI. It offers stored/available month selection and the Android calendar picker, constrained to 1900–2100. Month keys and displayed values use ASCII digits in YYYY-MM format.
- Every data-entry field has a persistent localized label. Numeric fields use only ASCII digits (0123456789) and stay left-to-right in both languages.
- Numeric fields expose the shared calculator. It supports scientific expressions, percentages, discounts, splitting, calculation history, copy and result insertion.
