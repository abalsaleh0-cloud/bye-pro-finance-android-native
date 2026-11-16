package com.byepro.bye_pro_finance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCategoryTest {
    @Test
    fun homeHasExactlyThreeCategoriesInTheRequiredOrder() {
        assertEquals(
            listOf(
                HomeCategory.MONTHLY_BILLS,
                HomeCategory.RENT,
                HomeCategory.EXPENSES
            ),
            HomeCategory.ordered
        )
        assertEquals(3, HomeCategory.values().size)
    }

    @Test
    fun eachHomeCategoryUsesItsOwnDataSourceAndFilter() {
        assertEquals(HomeRecordSource.BILLS, HomeCategory.MONTHLY_BILLS.source)
        assertEquals("monthly", HomeCategory.MONTHLY_BILLS.billKind)

        assertEquals(HomeRecordSource.BILLS, HomeCategory.RENT.source)
        assertEquals("rent", HomeCategory.RENT.billKind)

        assertEquals(HomeRecordSource.HOME_EXPENSES, HomeCategory.EXPENSES.source)
        assertNull(HomeCategory.EXPENSES.billKind)
    }
}
