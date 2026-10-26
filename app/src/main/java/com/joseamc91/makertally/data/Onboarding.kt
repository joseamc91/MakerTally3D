package com.joseamc91.makertally.data

import com.joseamc91.makertally.domain.*
import java.math.BigDecimal
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable enum class SetupStep { Welcome, Electricity, Printer, Costs, Complete }
enum class SetupField { Electricity, Tax, HeatingPower, HeatingMinutes, MachineRate, Multiplier }

/** Raw text is intentional: incomplete decimal input must survive process death. */
@Serializable data class OnboardingState(
    val step: SetupStep = SetupStep.Welcome,
    val completed: Boolean = false,
    val newInstallation: Boolean = true,
    val filamentIntroSeen: Boolean = false,
    val language: String = "en-US",
    val electricity: String = "",
    val tax: String = "",
    val heatingPower: String = "",
    val heatingMinutes: String = "",
    val machineRate: String = "",
    val multiplier: String = "",
    val genericId: String = UUID.randomUUID().toString()
) {
    val electricityValid get() = NumericInput.nonNegative(electricity) != null && NumericInput.percentage(tax) != null
    val printerValid get() = listOf(heatingPower, heatingMinutes).all { it.isBlank() || NumericInput.nonNegative(it) != null }
    val costsValid get() = NumericInput.nonNegative(machineRate) != null &&
        (NumericInput.nonNegative(multiplier)?.let { it >= BigDecimal.ONE } == true)
    val canAdvance get() = when(step) {
        SetupStep.Welcome -> true
        SetupStep.Electricity -> electricityValid
        SetupStep.Printer -> printerValid
        SetupStep.Costs, SetupStep.Complete -> electricityValid && printerValid && costsValid
    }
    fun field(field: SetupField, text: String) = when(field) {
        SetupField.Electricity -> copy(electricity=text)
        SetupField.Tax -> copy(tax=text)
        SetupField.HeatingPower -> copy(heatingPower=text)
        SetupField.HeatingMinutes -> copy(heatingMinutes=text)
        SetupField.MachineRate -> copy(machineRate=text)
        SetupField.Multiplier -> copy(multiplier=text)
    }
    fun next() = if(canAdvance) copy(step=SetupStep.entries[(step.ordinal+1).coerceAtMost(SetupStep.Complete.ordinal)]) else this
    fun back() = copy(step=SetupStep.entries[(step.ordinal-1).coerceAtLeast(0)])
    fun settings(): AppSettings {
        require(electricityValid && printerValid && costsValid)
        return neutralSettings(language).copy(
            electricityPrice=NumericInput.nonNegative(electricity)!!,
            electricityTaxPercent=NumericInput.percentage(tax)!!,
            heatingPower=NumericInput.nonNegative(heatingPower) ?: BigDecimal.ZERO,
            heatingMinutes=NumericInput.nonNegative(heatingMinutes) ?: BigDecimal.ZERO,
            machineRate=NumericInput.nonNegative(machineRate)!!,
            saleMultiplier=NumericInput.nonNegative(multiplier)!!)
    }
    fun genericFilament() = Filament(id=genericId,material="PLA",brand="Generic",variant="",
        spoolWeight=decimal("1000"),purchasePrice=decimal("14.99"),printPower=decimal("120"),active=true)

    companion object {
        fun legacy() = OnboardingState(completed=true,newInstallation=false,filamentIntroSeen=true,genericId="")
    }
}

// Explicit first-run values; AppSettings defaults remain the 1.0 decoding contract.
fun neutralSettings(language: String) = AppSettings(language=language,theme=ThemeMode.System,
    electricityPrice=BigDecimal.ZERO,electricityTaxPercent=BigDecimal.ZERO,
    heatingPower=BigDecimal.ZERO,heatingMinutes=BigDecimal.ZERO,machineRate=BigDecimal.ZERO,
    saleMultiplier=BigDecimal.ONE,filamentSort=FilamentSort.Name)
