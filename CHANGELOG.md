# Changelog — BYE PRO Finance Native

## 1.1.57 (158) — Home/Work navigation and entry redesign

- Added a fourth Home route: **الوارد من العمل**, showing the monthly total and actual transfer history from Work into Home.
- Renamed Rent to **الإيجارات الشهرية / Monthly Rent** without changing the stored `rent` kind.
- Home and Work now share one numbered editorial category index; the old segmented tabs have been removed.
- Work has five routes: Salary, Payment, Advance, Remaining and Home Transfers. Remaining shows cash still held in Work, with unpaid salary balance as a secondary value.
- Work Add offers four operations only: Add Salary, Add Payment, Add Advance and Transfer to Home. Remaining is calculated, not entered.
- Home Add always offers Add Monthly Bill, Add Monthly Rent and Add Expense, regardless of the current route.
- Welcome and Login are rebuilt as distinct non-card compositions with original finance-orbit artwork and open underlined login fields.
- Authentication rules, financial values, table relationships and existing records are preserved. This is a development preview, not a production release.
