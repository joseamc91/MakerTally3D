package com.joseamc91.makertally

import com.joseamc91.makertally.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class DomainTests {
    private val filament = Defaults.filaments().single { it.material == "ASA" }
    private fun calc(settings: AppSettings = AppSettings(), weight: String = "141", h: String = "6", m: String = "0", f: Filament = filament) =
        CalculationEngine().calculate(f, decimal(weight), PrintDuration(decimal(h), decimal(m)), settings)
    private fun eq(expected: String, actual: BigDecimal) = assertEquals(expected, 0, decimal(expected).compareTo(actual))

    @Test fun asaReferenceEveryFormulaExact() {
        val r = calc()
        eq("0.0175", r.pricePerGram); eq("2.4675", r.materialCost)
        eq("0.020", r.heatingEnergy); eq("1.080", r.printingEnergy); eq("1.100", r.totalEnergy)
        eq("0.14839", r.electricityCost); eq("2.61589", r.pieceCost); eq("1.50", r.machineCost)
        eq("9.34767", r.rawSalePrice); eq("9.35", r.suggestedSalePrice)
        eq("5.23411", r.rawGrossMargin); eq("5.24", r.grossMargin)
    }
    @Test fun spoolWeightDeterminesPerGramKgAndMaterial() {
        val f = filament.copy(spoolWeight=decimal("750"), purchasePrice=decimal("15"))
        eq("0.02", f.pricePerGram); eq("20", f.pricePerKg); eq("2.82", calc(f=f).materialCost)
    }
    @Test fun fractionalTimeAndHeatingWithoutIntermediateRounding() {
        val r=calc(AppSettings(heatingMinutes=decimal("1.5")), h="5", m="45")
        eq("1.035", r.printingEnergy); eq("0.03", r.heatingEnergy); eq("1.4375", r.machineCost)
    }
    @Test fun zeroCostsProduceZeroPriceAndMargin() {
        val r=calc(AppSettings(electricityPrice=BigDecimal.ZERO, heatingMinutes=BigDecimal.ZERO, machineRate=BigDecimal.ZERO, saleMultiplier=BigDecimal.ZERO), weight="0", h="0")
        listOf(r.materialCost,r.totalEnergy,r.electricityCost,r.pieceCost,r.machineCost,r.suggestedSalePrice,r.grossMargin).forEach {eq("0",it)}
    }
    @Test fun zeroMultiplierAndMachineAllowsNegativeMarginWithoutDivisionByZero() {
        val r=calc(AppSettings(machineRate=BigDecimal.ZERO,saleMultiplier=BigDecimal.ZERO))
        eq("0",r.suggestedSalePrice); eq("-2.62",r.grossMargin);eq("-2.61589",r.rawGrossMargin)
    }
    @Test fun zeroTimeKeepsConfiguredWarmup() {
        val r=calc(weight="0",h="0")
        eq("0.02",r.totalEnergy);eq("0.002698",r.electricityCost);eq("0",r.machineCost)
    }
    @Test fun invalidSpoolDoesNotDivideByZeroAndIsRejected() {
        listOf("0","-1").forEach { val f=filament.copy(spoolWeight=decimal(it));eq("0",f.pricePerGram);eq("0",f.pricePerKg);assertThrows(IllegalArgumentException::class.java){calc(f=f)} }
    }
    @Test fun negativeCalculationValuesRejected() {
        assertThrows(IllegalArgumentException::class.java){calc(weight="-1")}
        assertThrows(IllegalArgumentException::class.java){calc(h="-1")}
        assertThrows(IllegalArgumentException::class.java){calc(AppSettings(saleMultiplier=decimal("-1")))}
        assertThrows(IllegalArgumentException::class.java){calc(AppSettings(electricityPrice=decimal("-1")))}
    }
    @Test fun zeroPriceAndPowerAreValid() {
        val r=calc(AppSettings(heatingPower=BigDecimal.ZERO),f=filament.copy(purchasePrice=BigDecimal.ZERO,printPower=BigDecimal.ZERO))
        eq("0",r.materialCost);eq("0",r.electricityCost);eq("1.5",r.suggestedSalePrice);eq("0",r.grossMargin)
    }
    @Test fun multiplierOneUsesBothRoundUpRules() {val r=calc(AppSettings(saleMultiplier=BigDecimal.ONE));eq("4.12",r.suggestedSalePrice);eq("0.01",r.grossMargin)}
    @Test fun multiplierTwoAndHalfUsesRoundedSaleForMargin() {val r=calc(AppSettings(saleMultiplier=decimal("2.5")));eq("8.039725",r.rawSalePrice);eq("8.04",r.suggestedSalePrice);eq("3.92411",r.rawGrossMargin);eq("3.93",r.grossMargin)}
    @Test fun excelRoundUpRequiredBoundaryFixturesAndNegativeCounterparts() {
        val cases=listOf("9.34000" to "9.34","9.34001" to "9.35","9.34101" to "9.35","9.34767" to "9.35","9.34999" to "9.35","9.35000" to "9.35","5.23000" to "5.23","5.23001" to "5.24","5.23411" to "5.24","0" to "0")
        cases.forEach { (value,expected)->eq(expected,ExcelRounding.roundUp(decimal(value)));eq("-"+expected,ExcelRounding.roundUp(decimal("-"+value))) }
    }
    @Test fun roundUpLargeIntegralDoesNotOverflow() {eq(DECIMAL_LIMIT.toPlainString(),ExcelRounding.roundUp(DECIMAL_LIMIT))}
    @Test fun durationExactReferenceCases() {listOf(Triple("6","0","6"),Triple("6","30","6.5"),Triple("5","45","5.75"),Triple("0","0","0")).forEach{eq(it.third,PrintDuration(decimal(it.first),decimal(it.second)).toHours())}}
    @Test fun durationRejectsNegativeOutOfRangeAndFractionalValues() {
        listOf("-1" to "0","1" to "-1","1" to "60","1.5" to "0","1" to "0.5").forEach{assertThrows(IllegalArgumentException::class.java){PrintDuration(decimal(it.first),decimal(it.second))}}
    }
    @Test fun repeatingMinuteDivisionDoesNotInventFractionalCent() {
        val r=calc(AppSettings(electricityPrice=BigDecimal.ZERO,heatingPower=BigDecimal.ZERO,machineRate=decimal("0.60"),saleMultiplier=BigDecimal.ONE),weight="0",h="0",m="1")
        eq("0.01",r.machineCost);eq("0.01",r.rawSalePrice);eq("0.01",r.suggestedSalePrice);eq("0",r.grossMargin)
    }
    @Test fun repeatingSpoolDivisionCancelsExactlyBeforeSaleRounding() {
        val r=calc(AppSettings(electricityPrice=BigDecimal.ZERO,machineRate=BigDecimal.ZERO,saleMultiplier=BigDecimal.ONE),weight="3",h="0",f=filament.copy(spoolWeight=decimal("3"),purchasePrice=BigDecimal.ONE))
        eq("1",r.materialCost);eq("1",r.suggestedSalePrice);eq("0",r.grossMargin)
    }
    @Test fun numericSeparatorsAndWhitespaceAccepted() {
        listOf("6" to "6","6.5" to "6.5","6,5" to "6.5"," 0,1349 " to "0.1349",".5" to "0.5","0" to "0").forEach{eq(it.second,NumericInput.parse(it.first)!!)}
    }
    @Test fun incompleteGroupingExponentAndTooLargeNumbersRejected() {
        listOf("",",",".","-","abc","1,2.3","1 000","1e9","999999999999999999999999999999999999999999999").forEach{assertNull(it,NumericInput.parse(it))}
    }
    @Test fun everyDefaultMatchesMaui() {
        val s=AppSettings();assertEquals("es-ES",s.language);assertEquals(ThemeMode.System,s.theme)
        eq("0.1349",s.electricityPrice);eq("1200",s.heatingPower);eq("1",s.heatingMinutes);eq("0.25",s.machineRate);eq("3",s.saleMultiplier)
        val fs=Defaults.filaments(); assertEquals(7,fs.size);assertEquals(7,fs.map{it.id}.distinct().size)
        assertEquals(listOf("PLA Spool · BambuLab","PLA · BambuLab","PETG · BambuLab","PLA · Jayo","ASA · eSun","PLA 850 · Sakata","PETG · Elegoo"),fs.map{it.displayName})
        val prices=listOf("13.20","11.69","11.69","10.90","17.50","16.00","15.00")
        val watts=listOf("120","120","140","120","180","120","140")
        fs.forEachIndexed {i,f->assertTrue(f.isValid());assertTrue(f.active);eq("1000",f.spoolWeight);eq(prices[i],f.purchasePrice);eq(watts[i],f.printPower)}
    }
    @Test fun displayNameTrimsAndVariantIsOptional() {
        listOf("" to "PLA · Bambu Lab"," Matte " to "PLA Matte · Bambu Lab"," " to "PLA · Bambu Lab").forEach{assertEquals(it.second,Filament(material=" PLA ",brand="Bambu Lab",variant=it.first).displayName)}
    }
    @Test fun settingsValidationRejectsInvalidLanguageAndNegatives() {assertFalse(AppSettings(language="fr-FR").isValid());assertFalse(AppSettings(machineRate=decimal("-1")).isValid())}
}
