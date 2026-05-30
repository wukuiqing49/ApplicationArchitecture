package com.wkq.util.com.wkq.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 数字展示格式化工具。
 *
 * 默认使用国际展示规则：
 * 999 -> 999
 * 1000 -> 1K
 * 12500 -> 12.5K
 * 1000000 -> 1M
 */
object NumberFormatUtil {

    enum class UnitStyle {
        INTERNATIONAL,
        CHINESE_W
    }

    private const val DEFAULT_INVALID_TEXT = "0"
    private val NUMBER_CLEAN_REGEX = Regex("[,\\s_]")

    /**
     * 格式化数量，兼容 String / Number / null。
     *
     * 非法数据返回 [invalidText]。
     */
    fun formatCount(
        value: Any?,
        invalidText: String = DEFAULT_INVALID_TEXT,
        keepNegative: Boolean = true,
        maxDecimalPlaces: Int = 2,
        unitStyle: UnitStyle = UnitStyle.INTERNATIONAL
    ): String {
        val number = parseNumber(value) ?: return invalidText
        return formatCount(number, keepNegative, maxDecimalPlaces, unitStyle)
    }

    fun formatCount(
        value: Long,
        keepNegative: Boolean = true,
        maxDecimalPlaces: Int = 2,
        unitStyle: UnitStyle = UnitStyle.INTERNATIONAL
    ): String {
        return formatCount(BigDecimal.valueOf(value), keepNegative, maxDecimalPlaces, unitStyle)
    }

    fun formatCount(
        value: Int,
        keepNegative: Boolean = true,
        maxDecimalPlaces: Int = 2,
        unitStyle: UnitStyle = UnitStyle.INTERNATIONAL
    ): String {
        return formatCount(value.toLong(), keepNegative, maxDecimalPlaces, unitStyle)
    }

    fun formatCount(
        value: Double,
        keepNegative: Boolean = true,
        maxDecimalPlaces: Int = 2,
        unitStyle: UnitStyle = UnitStyle.INTERNATIONAL
    ): String {
        if (value.isNaN() || value.isInfinite()) return DEFAULT_INVALID_TEXT
        return formatCount(BigDecimal.valueOf(value), keepNegative, maxDecimalPlaces, unitStyle)
    }

    private fun formatCount(
        rawValue: BigDecimal,
        keepNegative: Boolean,
        maxDecimalPlaces: Int,
        unitStyle: UnitStyle
    ): String {
        val suffixValue = if (keepNegative) rawValue else rawValue.abs()
        val sign = if (suffixValue.signum() < 0) "-" else ""
        val absValue = suffixValue.abs()
        val safeDecimalPlaces = maxDecimalPlaces.coerceIn(0, 6)

        val divisorAndSuffix = resolveUnit(absValue, unitStyle)

        if (divisorAndSuffix == null) {
            return sign + stripTrailingZero(absValue, safeDecimalPlaces)
        }

        val (divisor, suffix) = divisorAndSuffix
        val scaledValue = absValue.divide(divisor, safeDecimalPlaces, RoundingMode.DOWN)
        return sign + stripTrailingZero(scaledValue, safeDecimalPlaces) + suffix
    }

    private fun resolveUnit(value: BigDecimal, unitStyle: UnitStyle): Pair<BigDecimal, String>? {
        return when (unitStyle) {
            UnitStyle.INTERNATIONAL -> when {
                value >= BigDecimal("1000000000") -> BigDecimal("1000000000") to "B"
                value >= BigDecimal("1000000") -> BigDecimal("1000000") to "M"
                value >= BigDecimal("1000") -> BigDecimal("1000") to "K"
                else -> null
            }

            UnitStyle.CHINESE_W -> when {
                value >= BigDecimal("10000") -> BigDecimal("10000") to "w"
                value >= BigDecimal("1000") -> BigDecimal("1000") to "K"
                else -> null
            }
        }
    }

    private fun parseNumber(value: Any?): BigDecimal? {
        return when (value) {
            null -> null
            is BigDecimal -> value
            is Byte -> BigDecimal.valueOf(value.toLong())
            is Short -> BigDecimal.valueOf(value.toLong())
            is Int -> BigDecimal.valueOf(value.toLong())
            is Long -> BigDecimal.valueOf(value)
            is Float -> {
                if (value.isNaN() || value.isInfinite()) null else BigDecimal.valueOf(value.toDouble())
            }
            is Double -> {
                if (value.isNaN() || value.isInfinite()) null else BigDecimal.valueOf(value)
            }
            is String -> parseStringNumber(value)
            else -> parseStringNumber(value.toString())
        }
    }

    private fun parseStringNumber(value: String): BigDecimal? {
        val normalized = value.trim()
            .replace(NUMBER_CLEAN_REGEX, "")
            .removeSuffix("+")
        if (normalized.isBlank()) return null

        return runCatching {
            BigDecimal(normalized)
        }.getOrNull()
    }

    private fun stripTrailingZero(value: BigDecimal, maxDecimalPlaces: Int): String {
        return value.setScale(maxDecimalPlaces, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }
}
