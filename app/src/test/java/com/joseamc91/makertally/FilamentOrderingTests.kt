package com.joseamc91.makertally

import com.joseamc91.makertally.domain.*
import com.joseamc91.makertally.ui.orderedFilaments
import org.junit.Assert.*
import org.junit.Test

class FilamentOrderingTests {
    private fun profile(name:String,price:String,active:Boolean=true,weight:String="1000") =
        Filament(material=name,brand="Test",purchasePrice=decimal(price),spoolWeight=decimal(weight),active=active)
    private val profiles=listOf(profile("Z","1",false),profile("B","20"),profile("A","30",false),profile("A","10"))
    private fun names(mode:FilamentSort)=orderedFilaments(profiles,mode,"es-ES").map{it.material+":"+it.active}

    @Test fun namesOrderActiveAlphabeticallyBeforeInactiveAlphabetically() {
        assertEquals(listOf("A:true","B:true","A:false","Z:false"),names(FilamentSort.Name))
    }
    @Test fun ascendingPriceKeepsActiveGroupFirstEvenWhenInactiveIsCheaper() {
        assertEquals(listOf("A:true","B:true","Z:false","A:false"),names(FilamentSort.PriceAscending))
    }
    @Test fun descendingPriceKeepsActiveGroupFirstEvenWhenInactiveIsDearer() {
        assertEquals(listOf("B:true","A:true","A:false","Z:false"),names(FilamentSort.PriceDescending))
    }
    @Test fun changingActivationMovesProfileBetweenGroupsForEverySort() {
        val changed=profiles.map{if(it.material=="B")it.copy(active=false)else it}
        FilamentSort.entries.forEach { mode ->
            val ordered=orderedFilaments(changed,mode,"es-ES")
            assertEquals("A",ordered.first().material);assertTrue(ordered.first().active)
            assertTrue(ordered.drop(1).all{!it.active})
            val reactivated=orderedFilaments(changed.map{if(it.material=="Z")it.copy(active=true)else it},mode,"es-ES")
            assertTrue(reactivated.take(2).all{it.active});assertTrue(reactivated.drop(2).all{!it.active})
        }
    }
    @Test fun priceSortUsesPricePerKilogramNotPurchasePriceOrRoundedProjection() {
        val small=profile("A","5",weight="250") // 20/kg; cheaper purchase but higher unit price.
        val large=profile("Z","15",weight="1000") // 15/kg.
        val fraction=profile("B","1",weight="3")
        val fractionHigher=profile("A","1.0000000000000000000000000001",weight="3")
        assertEquals(0,decimal("20").compareTo(small.pricePerKg))
        assertEquals(listOf(large,small),orderedFilaments(listOf(small,large),FilamentSort.PriceAscending,"en-US"))
        assertEquals(listOf(small,large),orderedFilaments(listOf(large,small),FilamentSort.PriceDescending,"en-US"))
        assertEquals(listOf(fraction,fractionHigher),orderedFilaments(listOf(fractionHigher,fraction),FilamentSort.PriceAscending,"en-US"))
    }
    @Test fun sortingDoesNotModifyProfilesOrFreeFormVariantsAndTiesAreDeterministic() {
        val spool=profile("PLA","10").copy(variant="Spool")
        val refill=profile("PLA","10")
        val input=listOf(spool,refill)
        assertEquals("PLA Spool · Test",spool.displayName)
        assertEquals(listOf(refill,spool),orderedFilaments(input,FilamentSort.Name,"es-ES"))
        assertEquals(listOf(spool,refill),input);assertEquals("Spool",spool.variant)
        val tied=spool.copy(id=refill.id)
        assertEquals(orderedFilaments(listOf(tied,spool),FilamentSort.PriceAscending,"en-US"),
            orderedFilaments(listOf(spool,tied),FilamentSort.PriceAscending,"en-US"))
    }
}
