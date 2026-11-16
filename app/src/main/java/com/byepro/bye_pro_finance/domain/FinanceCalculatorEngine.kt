package com.byepro.bye_pro_finance.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Calculator engine ported from the Flutter project's calculator semantics
 * and adapted for the Native Android application.
 */
object FinanceCalculatorEngine {
    fun evaluate(source: String): Double {
        require(source.isNotBlank()) { "Expression is empty" }
        val parser = ExpressionParser(source)
        val result = parser.parse()
        require(result.isFinite()) { "Result is not finite" }
        return result
    }

    fun format(value: Double): String {
        require(value.isFinite()) { "Result is not finite" }
        if (value == 0.0) return "0"
        if (kotlin.math.abs(value) >= 1e12 || kotlin.math.abs(value) < 1e-8) {
            return String.format(Locale.US, "%.10g", value)
        }
        val formatter = DecimalFormat(
            "0.##########",
            DecimalFormatSymbols(Locale.US)
        ).apply {
            roundingMode = RoundingMode.HALF_UP
            isGroupingUsed = false
        }
        return formatter.format(value)
    }

    fun asInsertableAmount(value: Double): String? {
        if (!value.isFinite() || value < 0.0 || value != kotlin.math.floor(value)) return null
        if (value > Long.MAX_VALUE.toDouble()) return null
        return value.toLong().toString()
    }

    fun percentage(amount: BigInteger, percent: String): BigInteger? {
        if (amount.signum() < 0) return null
        val rate = parsePercent(percent) ?: return null
        return BigDecimal(amount).multiply(rate)
            .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP).toBigIntegerExact()
    }

    fun discount(amount: BigInteger, percent: String): BigInteger? {
        if (amount.signum() < 0) return null
        val rate = parsePercent(percent) ?: return null
        if (rate < BigDecimal.ZERO || rate > BigDecimal("100")) return null
        val deduction = BigDecimal(amount).multiply(rate)
            .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP).toBigIntegerExact()
        return amount.subtract(deduction)
    }

    fun split(amount: BigInteger, people: BigInteger?): Pair<BigInteger, BigInteger>? {
        if (amount.signum() < 0 || people == null || people.signum() <= 0) return null
        val division = amount.divideAndRemainder(people)
        return division[0] to division[1]
    }

    private fun parsePercent(source: String): BigDecimal? {
        val normalized = source.trim().replace(',', '.')
        if (!Regex("^\\d+(?:\\.\\d{1,8})?$").matches(normalized)) return null
        return normalized.toBigDecimalOrNull()
    }

    private class ExpressionParser(private val source: String) {
        private var position = 0

        fun parse(): Double {
            val value = parseExpression()
            skipWhitespace()
            require(position == source.length) { "Unexpected input" }
            require(value.isFinite()) { "Result is not finite" }
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when {
                    match('+') -> value += parseTerm()
                    match('-') || match('−') -> value -= parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                when {
                    match('*') || match('×') -> value *= parseUnary()
                    match('/') || match('÷') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Division by zero" }
                        value /= divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            return when {
                match('+') -> parseUnary()
                match('-') || match('−') -> -parseUnary()
                else -> parsePower()
            }
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            if (!match('^')) return base
            return Math.pow(base, parseUnary())
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (match('%')) value /= 100.0
            return value
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (match('√')) {
                val radicand = parsePrimary()
                require(radicand >= 0.0) { "Square root of a negative value" }
                return kotlin.math.sqrt(radicand)
            }
            if (match('(')) {
                val value = parseExpression()
                require(match(')')) { "Missing closing parenthesis" }
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): Double {
            skipWhitespace()
            val start = position
            var hasDigit = false
            while (position < source.length && source[position] in '0'..'9') {
                hasDigit = true
                position++
            }
            if (position < source.length && source[position] == '.') {
                position++
                while (position < source.length && source[position] in '0'..'9') {
                    hasDigit = true
                    position++
                }
            }
            require(hasDigit) { "A number was expected" }
            return source.substring(start, position).toDouble()
        }

        private fun match(expected: Char): Boolean {
            skipWhitespace()
            if (position >= source.length || source[position] != expected) return false
            position++
            return true
        }

        private fun skipWhitespace() {
            while (position < source.length && source[position].isWhitespace()) position++
        }
    }
}
