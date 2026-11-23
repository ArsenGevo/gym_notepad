package com.gymnotepad.app.domain

import com.gymnotepad.app.data.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode

private val GRAMS_PER_POUND = BigDecimal("453.59237")

val WeightUnit.symbol: String get() = if (this == WeightUnit.KILOGRAMS) "кг" else "lb"

fun Long.toWeightText(unit: WeightUnit): String {
    val value = if (unit == WeightUnit.KILOGRAMS) BigDecimal.valueOf(this, 3) else BigDecimal.valueOf(this).divide(GRAMS_PER_POUND, 2, RoundingMode.HALF_UP)
    return value.stripTrailingZeros().toPlainString()
}

fun String.toWeightGrams(unit: WeightUnit): Long? = runCatching {
    val value = replace(',', '.').toBigDecimalOrNull() ?: return null
    if (unit == WeightUnit.KILOGRAMS) value.multiply(BigDecimal(1_000)) else value.multiply(GRAMS_PER_POUND)
}.getOrNull()?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()

fun Long.formatWeight(unit: WeightUnit): String = "${toWeightText(unit)} ${unit.symbol}"

fun Long.toWeightValue(unit: WeightUnit): Float =
    if (unit == WeightUnit.KILOGRAMS) this / 1_000f else BigDecimal.valueOf(this).divide(GRAMS_PER_POUND, 4, RoundingMode.HALF_UP).toFloat()
