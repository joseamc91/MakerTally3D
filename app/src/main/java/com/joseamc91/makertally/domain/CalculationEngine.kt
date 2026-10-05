package com.joseamc91.makertally.domain

import java.math.BigDecimal

data class CalculationResult(
    val pricePerGram: BigDecimal, val materialCost: BigDecimal,
    val heatingEnergy: BigDecimal, val printingEnergy: BigDecimal, val totalEnergy: BigDecimal,
    val electricityCost: BigDecimal, val pieceCost: BigDecimal, val machineCost: BigDecimal,
    val rawSalePrice: BigDecimal, val suggestedSalePrice: BigDecimal,
    val rawGrossMargin: BigDecimal, val grossMargin: BigDecimal
)

class CalculationEngine {
    fun calculate(filament: Filament, weight: BigDecimal, duration: PrintDuration, settings: AppSettings): CalculationResult {
        require(filament.isValid() && settings.isValid() && weight >= BigDecimal.ZERO && weight.isSupported())
        val price = Ratio(filament.purchasePrice, filament.spoolWeight)
        val material = price * weight
        val heating = Ratio(settings.heatingPower * settings.heatingMinutes, decimal("60000"))
        val printing = Ratio(filament.printPower * duration.totalMinutes, decimal("60000"))
        val energy = heating + printing
        val electricity = energy * settings.electricityPrice
        val piece = material + electricity
        val machine = Ratio(settings.machineRate * duration.totalMinutes, decimal("60"))
        val rawSale = piece * settings.saleMultiplier + machine
        val sale = rawSale.roundUp(2)
        val rawGross = Ratio(sale) - piece - machine
        val gross = rawGross.roundUp(2)
        require(listOf(price, material, heating, printing, energy, electricity, piece, machine, rawSale, rawGross).all { it.withinRange() })
        require(sale.abs() <= DECIMAL_LIMIT && gross.abs() <= DECIMAL_LIMIT)
        return CalculationResult(price.decimal(), material.decimal(), heating.decimal(), printing.decimal(),
            energy.decimal(), electricity.decimal(), piece.decimal(), machine.decimal(), rawSale.decimal(),
            sale, rawGross.decimal(), gross)
    }
}
