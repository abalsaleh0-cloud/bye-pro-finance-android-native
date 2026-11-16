package com.byepro.bye_pro_finance.domain

/**
 * Stable domain contract for the four Home destinations.
 *
 * Monthly bills and rent are separate bill kinds in the existing home_bills
 * table. Household expenses are stored separately in home_expenses. Work-to-home
 * receipts are existing transfer rows in work_home_transfers.
 * This enum owns the mapping so UI labels/tabs cannot silently change the
 * data source used by a category.
 */
enum class HomeRecordSource {
    BILLS,
    HOME_EXPENSES,
    WORK_TRANSFERS
}

enum class HomeCategory(
    val source: HomeRecordSource,
    val billKind: String? = null
) {
    MONTHLY_BILLS(HomeRecordSource.BILLS, billKind = "monthly"),
    RENT(HomeRecordSource.BILLS, billKind = "rent"),
    EXPENSES(HomeRecordSource.HOME_EXPENSES),
    RECEIVED_FROM_WORK(HomeRecordSource.WORK_TRANSFERS);

    companion object {
        /** The stable display/navigation order required by the Home section. */
        val ordered: List<HomeCategory> = values().toList()
    }
}
