# BYE PRO Finance — Native Data Dictionary

**Scope:** Current Native SQLite compatibility layer, schema version 2  
**Implementation:** `LegacyFinanceDatabase`, `DashboardRepository`, `FinanceWriteRepository`

This document describes the database columns and the financial rules implemented by the current Native code. It is not permission to change the schema. Any schema change requires an explicit versioned migration, a protected copy of the previous database, and regression tests.

## 1. Storage conventions

| Convention | Contract |
|---|---|
| Database file | `bye_pro_finance.sqlite` |
| Android directory | `context.getDir("flutter", Context.MODE_PRIVATE)` |
| SQLite user version | `2` |
| IDs | Text identifiers; primary keys are unique |
| Money | Integer values in the app's stored minor-unit scale; never floating-point |
| Timestamps | Epoch milliseconds |
| Month values | Separate integer `due_year` and `due_month` where the record is due-month based |
| Foreign keys | Enabled by `LegacyFinanceDatabase.onConfigure` |

Do not assume a decimal scale for a currency from the column name alone. Preserve the integer value passed by the existing application.

## 2. Table and column reference

### `work_entries`

Stores work-side salary, advance and payment records.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `kind` | TEXT | `salary`, `advance` or `payment`; the writer accepts `salaryPayment` and normalizes it to `payment` |
| `amount_minor_units` | INTEGER | Positive amount |
| `due_year` | INTEGER | Valid year, 1–9999 |
| `due_month` | INTEGER | Valid month, 1–12 |
| `recorded_at_epoch_millis` | INTEGER | Record timestamp |
| `note` | TEXT nullable | Optional note |

### `work_home_transfers`

Tracks money moved from the work-side received balance into the home-side budget.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `amount_minor_units` | INTEGER | Positive amount |
| `transferred_at_epoch_millis` | INTEGER | Determines the transfer month |
| `note` | TEXT nullable | Optional note |

### `home_expenses`

Stores home expenses.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `amount_minor_units` | INTEGER | Positive amount |
| `category_id` | TEXT | Required category identifier |
| `expense_date_epoch_millis` | INTEGER | Expense date and month |
| `due_year` | INTEGER | Valid year, 1–9999 |
| `due_month` | INTEGER | Valid month, 1–12 |
| `note` | TEXT nullable | Optional note |

### `home_bills`

Stores monthly bills and rent obligations.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `title` | TEXT | Required bill name |
| `kind` | TEXT | SQL check: `monthly` or `rent` |
| `amount_minor_units` | INTEGER | SQL check: greater than zero |
| `due_year` | INTEGER | Valid year, 1–9999 in writer |
| `due_month` | INTEGER | SQL check: 1–12 |
| `note` | TEXT nullable | Optional note |

### `home_bill_payments`

Stores payment history for a bill.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `bill_id` | TEXT | Required foreign key to `home_bills.id`; cascades on parent deletion at SQLite level |
| `amount_minor_units` | INTEGER | SQL check: greater than zero |
| `paid_at_epoch_millis` | INTEGER | Payment timestamp |
| `note` | TEXT nullable | Optional note |

Application rule: the Native writer prevents deleting a bill that has payment history, even though the underlying foreign key has cascade behaviour.

### `debts`

Stores both money owed to the user and money the user owes.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `direction` | TEXT | `creditor` or `debtor` |
| `original_amount_minor_units` | INTEGER | Positive original amount |
| `outstanding_amount_minor_units` | INTEGER | Between zero and original amount; must reconcile with payment history |
| `due_year` | INTEGER | Valid year, 1–9999 |
| `due_month` | INTEGER | Valid month, 1–12 |
| `created_at_epoch_millis` | INTEGER | Creation timestamp |
| `counterparty` | TEXT | Required person/entity name |
| `note` | TEXT nullable | Optional note |

### `debt_payments`

Stores debt payment history.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `id` | TEXT | Required primary key |
| `debt_id` | TEXT | Required foreign key to `debts.id`; cascades at SQLite level |
| `amount_minor_units` | INTEGER | Must be positive in the writer |
| `paid_at_epoch_millis` | INTEGER | Payment timestamp |
| `note` | TEXT nullable | Optional note |

Application rule: the Native writer prevents deleting a debt that has payment history.

### `app_settings`

Stores simple app preferences.

| Column | SQLite type | Rule / meaning |
|---|---|---|
| `setting_key` | TEXT | Primary key |
| `setting_value` | TEXT | Required value |

Current Native preferences include `locale` and `theme_mode`. Startup removes only these obsolete appearance keys: `theme_id`, `custom_themes`, and `appearance_preset`. Unknown settings and finance data are retained.

## 3. Relationships

```text
debts 1 ───── * debt_payments
home_bills 1 ───── * home_bill_payments

work_entries ── contributes to monthly received-work balance
work_home_transfers ── allocates received funds to home
home_expenses ── consumes home allocation
home_bills ── creates an outstanding home obligation
home_bill_payments ── settles part of a bill obligation
```

The database helper enables SQLite foreign-key enforcement. Payment rows must be reconciled with their parent balance; they are not disposable UI details.

## 4. Dashboard calculations

All sums below use integer minor units.

### Work side

For the selected `YearMonth`:

- `salaryTotal` = sum of work entries whose kind is `salary`.
- `advancesTotal` = sum of entries whose kind is `advance`.
- `paymentsTotal` = sum of entries whose kind is `payment` or `salaryPayment`.
- `currentSalaryOutstanding` = `max(salaryTotal - advancesTotal - paymentsTotal, 0)`.
- `cashReceived` = sum of work entries except `salary` (advances and payments received).
- `workRemaining` = `cashReceived - transfersTotal`.

### Home side

For the selected month:

- `transfersTotal` = transfers whose timestamp falls in the month.
- `homeExpensesTotal` = expenses whose timestamp falls in the month.
- `billsPaid` = bill-payment rows whose payment timestamp falls in the month.
- `billsOutstanding` = sum of each bill due in the selected month, less all payments for that bill made before the end of the selected month, floored at zero.
- `homeAvailableBalance` = `transfersTotal - homeExpensesTotal - billsPaid - billsOutstanding`.

### Debt balances

- `creditorBalance` = sum of outstanding amounts for all debts with direction `creditor`.
- `debtorBalance` = sum of outstanding amounts for all debts with direction `debtor`.

These debt totals are not restricted to the selected month. The selected month controls the monthly work/home figures and activity grouping.

## 5. Write invariants and validation

All multi-step financial operations and their balance checks execute inside SQLite transactions.

### Work entries and transfers

- Work-entry amounts must be positive and due year/month values valid.
- Supported work kinds are salary, advance and payment (with `salaryPayment` accepted as a legacy input and normalized).
- A change that reduces received work cash must not leave monthly transfers greater than received cash.
- A new or increased transfer must not exceed the month's received-work amount. Existing overdrawn legacy data is not automatically rejected unless the proposed change increases that overdraw.
- A transfer change must not leave home commitments greater than the resulting transfer total.

### Home expenses

- Expense amount must be positive; ID, category ID and due year/month are required.
- New or increased expense must fit the available home balance after transfers, existing expenses, bill payments and outstanding bills.

### Debts and payments

- Debt direction must be `creditor` or `debtor`.
- Original debt must be positive; outstanding amount must be between zero and original.
- If payment history exists, outstanding amount must equal original amount minus recorded payments.
- A debt payment cannot exceed the outstanding balance.
- Undoing a payment restores the outstanding amount and must not exceed the original debt.
- Deleting a debt with payment history is rejected.

### Bills and payments

- Bill kind must be `monthly` or `rent`; title and ID are required.
- Bill amount must be positive and due month valid.
- An edited bill cannot be reduced below the sum already paid.
- A payment cannot exceed the bill's remaining amount and must fit the available home budget.
- Deleting a bill with payment history is rejected.

The mapped errors are represented by `FinanceRuleException.Rule` in `FinanceWriteRepository.kt`. New financial rules must be added there and covered by tests; the UI must not reimplement or bypass them.

## 6. Schema migration policy

- Current database version: 2.
- The version-2 upgrade creates `debt_payments` if it is missing.
- Extended tables (`home_bills`, `home_bill_payments`, `app_settings`) are ensured in `onOpen`, matching the existing Drift before-open behavior.
- Never lower the schema version or delete the database to resolve a migration issue.
- For a schema change: document old and new structures, create an explicit migration, test from a copy of the previous database, validate record and payment counts, and update this dictionary.

