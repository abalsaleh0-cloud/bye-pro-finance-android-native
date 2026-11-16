package com.byepro.bye_pro_finance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCategoryTest {
    @Test
    fun workHasFiveRoutesInRequestedOrder() {
        assertEquals(
            listOf(
                WorkCategory.SALARY,
                WorkCategory.PAYMENT,
                WorkCategory.ADVANCE,
                WorkCategory.REMAINING,
                WorkCategory.HOME_TRANSFERS
            ),
            WorkCategory.ordered
        )
        assertEquals(5, WorkCategory.values().size)
    }

    @Test
    fun eachWorkRouteHasItsCorrectSourceOrCalculation() {
        assertEquals("salary", WorkCategory.SALARY.recordKind)
        assertEquals("payment", WorkCategory.PAYMENT.recordKind)
        assertEquals("advance", WorkCategory.ADVANCE.recordKind)
        assertTrue(WorkCategory.REMAINING.calculated)
        assertTrue(WorkCategory.HOME_TRANSFERS.transfers)
        assertNull(WorkCategory.REMAINING.recordKind)
        assertNull(WorkCategory.HOME_TRANSFERS.recordKind)
    }
}