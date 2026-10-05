@file:UseSerializers(DecimalSerializer::class)
package com.joseamc91.makertally.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun decimal(value: String) = BigDecimal(value)
val DECIMAL_LIMIT = decimal("79228162514264337593543950335")
private val ZERO = BigDecimal.ZERO
fun BigDecimal.isSupported() = abs() <= DECIMAL_LIMIT && stripTrailingZeros().scale() <= 28

object DecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("ExactDecimal", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())
    override fun deserialize(decoder: Decoder) = BigDecimal(decoder.decodeString())
}

@Serializable enum class ThemeMode { System, Light, Dark }
@Serializable enum class FilamentSort { Name, PriceAscending, PriceDescending }
@Serializable data class AppSettings(
    val language: String = "es-ES",
    val theme: ThemeMode = ThemeMode.System,
    val electricityPrice: BigDecimal = decimal("0.1349"),
    val heatingPower: BigDecimal = decimal("1200"),
    val heatingMinutes: BigDecimal = BigDecimal.ONE,
    val machineRate: BigDecimal = decimal("0.25"),
    val saleMultiplier: BigDecimal = decimal("3"),
    val filamentSort: FilamentSort = FilamentSort.Name
) {
    fun isValid() = language in listOf("es-ES", "en-US") &&
        listOf(electricityPrice, heatingPower, heatingMinutes, machineRate, saleMultiplier)
            .all { it >= ZERO && it.isSupported() }
}

@Serializable data class Filament(
    val id: String = UUID.randomUUID().toString(),
    val material: String = "",
    val brand: String = "",
    val variant: String = "",
    val spoolWeight: BigDecimal = decimal("1000"),
    val purchasePrice: BigDecimal = ZERO,
    val printPower: BigDecimal = decimal("120"),
    val active: Boolean = true
) {
    val displayName: String get() = material.trim() +
        (if (variant.isBlank()) "" else " " + variant.trim()) + " · " + brand.trim()
    val pricePerGram: BigDecimal get() = if (spoolWeight <= ZERO) ZERO else Ratio(purchasePrice, spoolWeight).decimal()
    val pricePerKg: BigDecimal get() = if (spoolWeight <= ZERO) ZERO else Ratio(purchasePrice * decimal("1000"), spoolWeight).decimal()
    fun isValid(): Boolean = runCatching {
        val uuid = UUID.fromString(id)
        uuid != UUID(0, 0) && material.isNotBlank() && brand.isNotBlank() && spoolWeight > ZERO &&
            listOf(spoolWeight, purchasePrice, printPower).all { it >= ZERO && it.isSupported() } &&
            Ratio(purchasePrice * decimal("1000"), spoolWeight).withinRange()
    }.getOrDefault(false)
}

object Defaults {
    fun filaments(): List<Filament> = listOf(
        Filament(material="PLA", brand="BambuLab", variant="Spool", purchasePrice=decimal("13.20")),
        Filament(material="PLA", brand="BambuLab", purchasePrice=decimal("11.69")),
        Filament(material="PETG", brand="BambuLab", purchasePrice=decimal("11.69"), printPower=decimal("140")),
        Filament(material="PLA", brand="Jayo", purchasePrice=decimal("10.90")),
        Filament(material="ASA", brand="eSun", purchasePrice=decimal("17.50"), printPower=decimal("180")),
        Filament(material="PLA", brand="Sakata", variant="850", purchasePrice=decimal("16.00")),
        Filament(material="PETG", brand="Elegoo", purchasePrice=decimal("15.00"), printPower=decimal("140"))
    )
}

// Exact fraction of finite BigDecimals. No intermediate division/monetary rounding.
// Infinite decimal expansions are projected only for returned technical values.
internal data class Ratio(val numerator: BigDecimal, val denominator: BigDecimal = BigDecimal.ONE) {
    init { require(denominator > ZERO) }
    operator fun plus(other: Ratio) = Ratio(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
    operator fun minus(other: Ratio) = Ratio(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)
    operator fun times(value: BigDecimal) = Ratio(numerator * value, denominator)
    fun decimal(): BigDecimal = try { numerator.divide(denominator) } catch (_: ArithmeticException) {
        numerator.divide(denominator, MathContext.DECIMAL128)
    }
    fun roundUp(places: Int) = numerator.divide(denominator, places, RoundingMode.UP)
    fun withinRange() = numerator.abs() <= DECIMAL_LIMIT * denominator
}

object ExcelRounding {
    fun roundUp(value: BigDecimal, places: Int = 2): BigDecimal {
        require(places in 0..28)
        return value.setScale(places, RoundingMode.UP)
    }
}

object NumericInput {
    private val pattern = Regex("[+-]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)")
    fun parse(text: String): BigDecimal? {
        val trimmed = text.trim()
        if (trimmed.length > 80 || !pattern.matches(trimmed)) return null
        return trimmed.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.isSupported() }
    }
    fun nonNegative(text: String, positive: Boolean = false, integer: Boolean = false, maximum: Int? = null): BigDecimal? =
        parse(text)?.takeIf { (if (positive) it > ZERO else it >= ZERO) &&
            (!integer || it.stripTrailingZeros().scale() <= 0) && (maximum == null || it <= maximum.toBigDecimal()) }
}

data class PrintDuration(val hours: BigDecimal, val minutes: BigDecimal = ZERO) {
    init {
        require(hours >= ZERO && hours.stripTrailingZeros().scale() <= 0 && hours.isSupported())
        require(minutes >= ZERO && minutes <= decimal("59") && minutes.stripTrailingZeros().scale() <= 0)
        require(Ratio(hours * decimal("60") + minutes, decimal("60")).withinRange())
    }
    val totalMinutes: BigDecimal get() = hours * decimal("60") + minutes
    fun toHours(): BigDecimal = Ratio(totalMinutes, decimal("60")).decimal()
}

