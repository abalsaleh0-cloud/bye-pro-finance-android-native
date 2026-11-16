package com.byepro.bye_pro_finance.domain

/**
 * Stable domain contract for the three and only three Home destinations.
 *
 * Monthly bills and rent are separate bill kinds in the existing home_bills
 * table. Household expenses are stored separately in home_expenses.
 * This enum owns the mapping so UI labels/tabs cannot silently change the
 * data source used by a category.
 */
enum class HomeRecordSource {
    BILLS,
    HOME_EXPENSES
}

enum class HomeCategory(
    val source: HomeRecordSource,
    val billKind: String? = null
) {
    MONTHLY_BILLS(HomeRecordSource.BILLS, billKind = "monthly"),
    RENT(HomeRecordSource.BILLS, billKind = "rent"),
    EXPENSES(HomeRecordSource.HOME_EXPENSES);

    companion object {
        /** The stable display/navigation order required by the Home section. */
        val ordered: List<HomeCategory> = values().toList()
    }
}
