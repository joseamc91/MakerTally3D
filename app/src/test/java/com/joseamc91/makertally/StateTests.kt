package com.joseamc91.makertally
import com.joseamc91.makertally.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class StateTests {
    @Test fun stepsAreExactAndClampToMinima() {
        listOf("100","1","0.05").forEach { step->
            val value=decimal("0.0123");val next=SettingsSteps.change(value,decimal(step))
            assertEquals(0,(value+decimal(step)).compareTo(next))
            assertEquals(0,value.compareTo(SettingsSteps.change(next,-decimal(step))))
            assertEquals(0,BigDecimal.ZERO.compareTo(SettingsSteps.change(value,-decimal(step))))
            assertThrows(IllegalArgumentException::class.java){SettingsSteps.change(DECIMAL_LIMIT,decimal(step))}
        }
    }
    @Test fun globalMultiplierHalfStepsMinimumAndRecalculation() {
        val input=CalculatorInput();val asa=Defaults.filaments().first{it.material=="ASA"}
        val lower=AppSettings().copy(saleMultiplier=SettingsSteps.change(decimal("3"),decimal("-0.5"),BigDecimal.ONE))
        assertEquals(0,decimal("8.04").compareTo(input.calculate(asa,lower)!!.suggestedSalePrice))
        var multiplier=decimal("3")
        repeat(20){multiplier=SettingsSteps.change(multiplier,decimal("-0.5"),BigDecimal.ONE)}
        assertEquals(0,BigDecimal.ONE.compareTo(multiplier))
        assertEquals(0,BigDecimal.ONE.compareTo(SettingsSteps.change(BigDecimal.ZERO,decimal("0.5"),BigDecimal.ONE)))
    }
    @Test fun invalidTypingHidesResultsUntilFixed() {
        val asa=Defaults.filaments().first{it.material=="ASA"}
        listOf("",",",".","-1","60","1.5","1,5").forEach {assertNull(CalculatorInput(minutes=it).calculate(asa,AppSettings()))}
        assertNull(CalculatorInput(hours="6.5").calculate(asa,AppSettings()))
        assertNull(CalculatorInput(hours=DECIMAL_LIMIT.toPlainString(),minutes="59").calculate(asa,AppSettings()))
        assertEquals(0,decimal("1.17").compareTo(CalculatorInput(minutes="30").calculate(asa,AppSettings())!!.printingEnergy))
        assertNotNull(CalculatorInput(hours="0",minutes="0").calculate(asa,AppSettings()))
        assertNull(CalculatorInput().calculate(asa,AppSettings(),validSettings=false))
    }
    @Test fun activeSelectionKeepsPreferenceThenFallsBackOrEmpty() {
        val fs=Defaults.filaments();val asa=fs.first{it.material=="ASA"}
        assertEquals(asa,preferredFilament(fs,asa.id))
        val disabled=fs.map{if(it.id==asa.id)it.copy(active=false)else it}
        assertEquals(fs.first(),preferredFilament(disabled,asa.id))
        assertNull(preferredFilament(fs.map{it.copy(active=false)}));assertNull(preferredFilament(emptyList()))
    }
    @Test fun editorDefaultsAndRequiredPriceTypeBrand() {
        val editor=EditorDraft();assertEquals("1000",editor.weight);assertEquals("120",editor.power);assertEquals("",editor.price);assertTrue(editor.active);assertNull(editor.build())
        val valid=editor.copy(material="PLA",brand=" Brand ",variant=" Matte ",price="12,5")
        assertEquals("PLA Matte · Brand",valid.build()!!.displayName)
        assertNull(valid.copy(material=" ").build());assertNull(valid.copy(brand=" ").build())
        assertNull(valid.copy(weight="0").build());assertNull(valid.copy(price="-1").build());assertNull(valid.copy(power="-1").build())
        assertNotNull(valid.copy(price="0",power="0").build())
        assertNull(valid.copy(weight="0.0000000000000000000000000001",price=DECIMAL_LIMIT.toPlainString()).build())
    }
    @Test fun editingPreservesIdAndActivation() {
        val original=Defaults.filaments().first().copy(active=false)
        val edited=EditorDraft(original).copy(variant="HS").build()!!
        assertEquals(original.id,edited.id);assertFalse(edited.active);assertEquals("HS",edited.variant)
    }
    @Test fun formattingEuroSpanishAndEnglishWithoutFloatingPoint() {
        assertTrue(Formatting.money(decimal("17.5"),"es-ES").contains("17,50"))
        assertEquals("€17.50",Formatting.money(decimal("17.5"),"en-US"))
        assertEquals("€0.1349",Formatting.money(decimal("0.1349"),"en-US",4))
        assertTrue(Formatting.money(decimal("0.1349"),"es-ES",4).contains("0,1349"))
        assertEquals("—",Formatting.money(null,"es-ES"))
    }
    @Test fun languageReformatsValidInputsAndRetainsInvalidInput() {
        val field=CalculatorInput(weight="141.5",minutes=",").localized("es-ES")
        assertEquals("141,5",field.weight);assertEquals(",",field.minutes)
        assertEquals("141.5",field.localized("en-US").weight)
    }
}
