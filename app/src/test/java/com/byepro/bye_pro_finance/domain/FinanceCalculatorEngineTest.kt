package com.byepro.bye_pro_finance.domain

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorEngineTest {
    @Test
    fun scientificExpressionUsesOperatorPrecedence() {
        assertEquals(14.0, FinanceCalculatorEngine.evaluate("2+3*4"), 0.0000001)
    }

    @Test
    fun squareRootAndPowerAreSupported() {
        assertEquals(17.0, FinanceCalculatorEngine.evaluate("√(81)+2^3"), 0.0000001)
    }

    @Test
    fun percentagePostfixIsSupported() {
        assertEquals(20.0, FinanceCalculatorEngine.evaluate("200*10%"), 0.0000001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun divisionByZeroIsRejected() {
        FinanceCalculatorEngine.evaluate("15/0")
    }

    @Test
    fun percentageUsesHalfUpRounding() {
        assertEquals(
            BigInteger.valueOf(50),
            FinanceCalculatorEngine.percentage(BigInteger.valueOf(2000), "2.5")
        )
    }

    @Test
    fun discountReturnsAmountAfterDiscount() {
        assertEquals(
            BigInteger.valueOf(1750),
            FinanceCalculatorEngine.discount(BigInteger.valueOf(2000), "12.5")
        )
    }

    @Test
    fun splitPreservesRemainder() {
        assertEquals(
            BigInteger.valueOf(333) to BigInteger.valueOf(2),
            FinanceCalculatorEngine.split(BigInteger.valueOf(1001), BigInteger.valueOf(3))
        )
    }

    @Test
    fun invalidFinancialInputsAreRejected() {
        assertNull(FinanceCalculatorEngine.percentage(BigInteger.valueOf(100), "-2"))
        assertNull(FinanceCalculatorEngine.discount(BigInteger.valueOf(100), "120"))
        assertNull(FinanceCalculatorEngine.split(BigInteger.valueOf(100), BigInteger.ZERO))
    }

    @Test
    fun onlyWholeNonNegativeResultsCanEnterMoneyFields() {
        assertEquals("25", FinanceCalculatorEngine.asInsertableAmount(25.0))
        assertNull(FinanceCalculatorEngine.asInsertableAmount(25.5))
        assertNull(FinanceCalculatorEngine.asInsertableAmount(-1.0))
        assertEquals("9007199254740991", FinanceCalculatorEngine.asInsertableAmount(9007199254740991.0))
        assertNull(FinanceCalculatorEngine.asInsertableAmount(9007199254740992.0))
        assertTrue(FinanceCalculatorEngine.format(1234.5).contains("."))
    }
}
