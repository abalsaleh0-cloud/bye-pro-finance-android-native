package com.byepro.bye_pro_finance.domain

/**
 * Five independently browsable Work routes. Remaining is a calculated
 * balance view, so it deliberately has no Add operation.
 */
enum class WorkCategory(
    val recordKind: String? = null,
    val calculated: Boolean = false,
    val transfers: Boolean = false
) {
    SALARY(recordKind = "salary"),
    PAYMENT(recordKind = "payment"),
    ADVANCE(recordKind = "advance"),
    REMAINING(calculated = true),
    HOME_TRANSFERS(transfers = true);

    companion object {
        val ordered: List<WorkCategory> = values().toList()
    }
}