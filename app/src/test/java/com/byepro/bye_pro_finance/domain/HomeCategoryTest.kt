package com.byepro.bye_pro_finance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCategoryTest {
    @Test
    fun homeHasExactlyFourCategoriesInTheRequiredOrder() {
        assertEquals(
            listOf(
                HomeCategory.MONTHLY_BILLS,
                HomeCategory.RENT,
                HomeCategory.EXPENSES,
                HomeCategory.RECEIVED_FROM_WORK
            ),
            HomeCategory.ordered
        )
        assertEquals(4, HomeCategory.values().size)
    }

    @Test
    fun eachHomeCategoryUsesItsOwnDataSourceAndFilter() {
        assertEquals(HomeRecordSource.BILLS, HomeCategory.MONTHLY_BILLS.source)
        assertEquals("monthly", HomeCategory.MONTHLY_BILLS.billKind)

        assertEquals(HomeRecordSource.BILLS, HomeCategory.RENT.source)
        assertEquals("rent", HomeCategory.RENT.billKind)

        assertEquals(HomeRecordSource.HOME_EXPENSES, HomeCategory.EXPENSES.source)
        assertNull(HomeCategory.EXPENSES.billKind)

        assertEquals(HomeRecordSource.WORK_TRANSFERS, HomeCategory.RECEIVED_FROM_WORK.source)
        assertNull(HomeCategory.RECEIVED_FROM_WORK.billKind)
    }
}
