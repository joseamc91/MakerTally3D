package com.joseamc91.makertally.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object Formatting {
    fun money(value: BigDecimal?, language: String, places: Int = 2): String {
        if (value == null) return "—"
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag(language)).apply {
            currency = Currency.getInstance("EUR")
            minimumFractionDigits = places
            maximumFractionDigits = places
            roundingMode = RoundingMode.HALF_EVEN
        }.format(value)
    }
    fun number(value: BigDecimal, language: String, minimumDecimals: Int = 0, maximumDecimals: Int = 4): String =
        NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
            isGroupingUsed = false
            minimumFractionDigits = minimumDecimals
            maximumFractionDigits = maximumDecimals
            roundingMode = RoundingMode.HALF_EVEN
        }.format(value)
    fun input(value: BigDecimal, language: String) = value.stripTrailingZeros().toPlainString().let {
        if (language == "es-ES") it.replace('.', ',') else it
    }
}

object SettingsSteps {
    fun change(value: BigDecimal, delta: BigDecimal, minimum: BigDecimal = BigDecimal.ZERO): BigDecimal {
        require(value >= BigDecimal.ZERO && value.isSupported())
        val changed = (value + delta).max(minimum)
        require(changed.isSupported())
        return changed
    }
}

data class CalculatorInput(val weight: String = "141", val hours: String = "6", val minutes: String = "0") {
    fun calculate(selected: Filament?, settings: AppSettings, validSettings: Boolean = true): CalculationResult? = runCatching {
        if (!validSettings || selected == null) return null
        val grams = NumericInput.nonNegative(weight) ?: return null
        val h = NumericInput.nonNegative(hours, integer = true) ?: return null
        val m = NumericInput.nonNegative(minutes, integer = true, maximum = 59) ?: return null
        CalculationEngine().calculate(selected, grams, PrintDuration(h, m), settings)
    }.getOrNull()
    fun localized(language: String) = copy(weight = reformat(weight, language), hours = reformat(hours, language), minutes = reformat(minutes, language))
    private fun reformat(text: String, language: String) = NumericInput.parse(text)?.let { Formatting.input(it, language) } ?: text
}

fun preferredFilament(filaments: List<Filament>, preferredId: String? = null): Filament? =
    filaments.firstOrNull { it.active && it.id == preferredId } ?: filaments.firstOrNull { it.active }

data class EditorDraft(
    val original: Filament? = null,
    val material: String = original?.material ?: "",
    val brand: String = original?.brand ?: "",
    val variant: String = original?.variant ?: "",
    val weight: String = original?.spoolWeight?.toPlainString() ?: "1000",
    val price: String = original?.purchasePrice?.toPlainString() ?: "",
    val power: String = original?.printPower?.toPlainString() ?: "120",
    val active: Boolean = original?.active ?: true
) {
    fun build(): Filament? = runCatching {
        val result = Filament(id = original?.id ?: java.util.UUID.randomUUID().toString(),
            material = material.trim(), brand = brand.trim(), variant = variant.trim(),
            spoolWeight = NumericInput.nonNegative(weight, positive = true) ?: return null,
            purchasePrice = NumericInput.nonNegative(price) ?: return null,
            printPower = NumericInput.nonNegative(power) ?: return null, active = active)
        result.takeIf { it.isValid() }
    }.getOrNull()
}
