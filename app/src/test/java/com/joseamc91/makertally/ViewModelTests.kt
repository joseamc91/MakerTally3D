package com.joseamc91.makertally

import androidx.lifecycle.SavedStateHandle
import com.joseamc91.makertally.data.*
import com.joseamc91.makertally.domain.*
import com.joseamc91.makertally.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

private class FakeRepository : AppRepository {
    var settings=AppSettings();var filaments=Defaults.filaments();var fail=false
    override suspend fun load()=LoadedApp(settings,filaments)
    override suspend fun saveSettings(settings:AppSettings){if(fail)throw IOException();this.settings=settings}
    override suspend fun saveFilaments(profiles:List<Filament>){if(fail)throw IOException();filaments=profiles}
}
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ViewModelTests {
    @Test fun validTaxPersistsAndIsLoadedAfterRestart() = check { model,repo ->
        model.electricity("0,122");model.electricityTax("27,1864");runCurrent()
        assertEquals(decimal("27.1864"),repo.settings.electricityTaxPercent)
        assertEquals("9.42",model.state.value.result!!.suggestedSalePrice.toPlainString())
        val restarted=MakerTallyViewModel(repo);runCurrent()
        assertEquals("27,1864",restarted.state.value.electricityTaxText)
        assertEquals(model.state.value.result,restarted.state.value.result)
    }
    @Test fun invalidTaxDoesNotPersistAndResultReturnsWhenCorrected() = check { model,repo ->
        model.electricityTax("21");runCurrent();val lastValid=repo.settings
        listOf("",",",".","-1","abc").forEach { text ->
            model.electricityTax(text);runCurrent()
            assertEquals(text,model.state.value.electricityTaxText);assertFalse(model.state.value.electricityTaxValid)
            assertNull(model.state.value.result);assertEquals(lastValid,repo.settings)
        }
        model.electricityTax("0");runCurrent()
        assertTrue(model.state.value.electricityTaxValid);assertEquals("9.35",model.state.value.result!!.suggestedSalePrice.toPlainString())
    }
    @Test fun localeChangesBothElectricityInputsWithoutLosingTaxPrecision() = check { _,repo ->
        val saved=SavedStateHandle();val model=MakerTallyViewModel(repo,saved);runCurrent()
        val exact=decimal("27.186400000000000000000000000000123456789")
        model.electricity("0,122");model.electricityTax(exact.toPlainString().replace('.',','));runCurrent()
        model.language("en-US");runCurrent()
        assertEquals("0.122",model.state.value.electricityText)
        assertEquals(exact.toPlainString(),model.state.value.electricityTaxText)
        assertEquals(exact.toPlainString(),saved.get<String>("electricityTax"));assertEquals(exact,repo.settings.electricityTaxPercent)
        model.language("es-ES");runCurrent()
        assertEquals(exact.toPlainString().replace('.',','),model.state.value.electricityTaxText)
        assertEquals(exact,repo.settings.electricityTaxPercent)
    }
    @Test fun savedStateRestoresIncompleteTaxWhileKeepingLastValidSetting() = check { _,repo ->
        val saved=SavedStateHandle();val model=MakerTallyViewModel(repo,saved);runCurrent()
        model.electricityTax("27,1864");runCurrent();model.electricityTax(",");runCurrent()
        val recreated=MakerTallyViewModel(repo,saved);runCurrent()
        assertEquals(",",recreated.state.value.electricityTaxText);assertNull(recreated.state.value.result)
        assertEquals(decimal("27.1864"),recreated.state.value.settings.electricityTaxPercent)
        recreated.language("en-US");runCurrent();assertEquals(",",recreated.state.value.electricityTaxText)
        recreated.electricityTax("27.1864");runCurrent();assertNotNull(recreated.state.value.result)
    }
    @Test fun failedTaxWriteKeepsPersistedSettingAndReportsStorageNotice() = check { model,repo ->
        repo.fail=true;model.electricityTax("21");runCurrent()
        assertEquals(AppSettings(),repo.settings);assertEquals(AppSettings(),model.state.value.settings)
        assertEquals(StorageNotice.SaveFailed,model.state.value.notice)
    }
    @Test fun sortPreferencePersistsWithoutChangingCalculationsAndFailedSaveKeepsPreviousMode() = check { model,repo ->
        val result=model.state.value.result
        model.sort(FilamentSort.PriceAscending);model.language("en-US");model.theme(ThemeMode.Dark);runCurrent()
        assertEquals(FilamentSort.PriceAscending,repo.settings.filamentSort)
        assertEquals(FilamentSort.PriceAscending,model.state.value.settings.filamentSort)
        assertEquals(result,model.state.value.result)
        val recreated=MakerTallyViewModel(repo);runCurrent()
        assertEquals(FilamentSort.PriceAscending,recreated.state.value.settings.filamentSort)
        repo.fail=true;model.sort(FilamentSort.PriceDescending);runCurrent()
        assertEquals(FilamentSort.PriceAscending,model.state.value.settings.filamentSort)
        assertEquals(StorageNotice.SaveFailed,model.state.value.notice)
    }
    private fun check(block:suspend TestScope.(MakerTallyViewModel,FakeRepository)->Unit)=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { val repo=FakeRepository();val model=MakerTallyViewModel(repo);runCurrent();block(model,repo) }
        finally { Dispatchers.resetMain() }
    }
    @Test fun initialAsaPriceDetailsAndRecreationState() = check { model,repo ->
        assertFalse(model.state.value.detailsOpen);assertEquals("9.35",model.state.value.result!!.suggestedSalePrice.toPlainString())
        model.toggleDetails();assertTrue(model.state.value.detailsOpen);model.toggleDetails();assertFalse(model.state.value.detailsOpen)
        val saved=SavedStateHandle(mapOf("weight" to "99,5","hours" to "2","minutes" to "30","details" to true,"tab" to 1))
        val recreated=MakerTallyViewModel(repo,saved);runCurrent()
        assertEquals("99,5",recreated.state.value.input.weight);assertEquals(Destination.Filaments,recreated.state.value.destination);assertTrue(recreated.state.value.detailsOpen)
    }
    @Test fun nativeSystemPerAppLocaleIsRespectedOnRestart() = check { _,repo ->
        val model=MakerTallyViewModel(repo,SavedStateHandle(),"en-US");runCurrent()
        assertEquals("en-US",model.state.value.settings.language);assertEquals("en-US",repo.settings.language)
        assertEquals("0.1349",model.state.value.electricityText)
    }
    @Test fun crudAndActivationKeepLibraryAndSelectionConsistent() = check { model,repo ->
        val asa=model.state.value.selected!!
        model.toggleActive(asa);runCurrent();assertFalse(repo.filaments.first{it.id==asa.id}.active);assertNotEquals(asa.id,model.state.value.selectedId)
        model.edit();model.editor(EditorDraft(material="PLA",brand="Test",variant="Matte",price="10,12"));model.saveEditor();runCurrent()
        val f=repo.filaments.last();assertEquals("PLA Matte · Test",f.displayName);assertNull(model.state.value.editor)
        model.edit(f);model.editor(model.state.value.editor!!.copy(price="11.25"));model.saveEditor();runCurrent()
        assertEquals(f.id,repo.filaments.last().id);assertEquals(0,decimal("11.25").compareTo(repo.filaments.last().purchasePrice))
        model.askDelete(f);model.cancelDelete();assertEquals(8,repo.filaments.size)
        model.askDelete(f);model.confirmDelete();runCurrent();assertEquals(7,repo.filaments.size)
    }
    @Test fun failedSaveKeepsLibraryEditorAndShowsNotice() = check { model,repo ->
        val original=repo.filaments;repo.fail=true
        model.edit();model.editor(EditorDraft(material="PLA",brand="Test",price="10"));model.saveEditor();runCurrent()
        assertEquals(original,model.state.value.filaments);assertEquals(original,repo.filaments)
        assertNotNull(model.state.value.editor);assertEquals(StorageNotice.SaveFailed,model.state.value.notice);assertFalse(model.state.value.saving)
    }
    @Test fun invalidInputsDoNotSaveButLocaleAndThemeStillPersist() = check { model,repo ->
        model.electricity(",");runCurrent();assertNull(model.state.value.result);assertEquals(AppSettings(),repo.settings)
        model.language("en-US");model.theme(ThemeMode.Dark);runCurrent()
        assertEquals(",",model.state.value.electricityText);assertEquals("en-US",repo.settings.language);assertEquals(ThemeMode.Dark,repo.settings.theme)
        model.electricity("0.1349");runCurrent();assertNotNull(model.state.value.result)
    }
    @Test fun allFourStepsPersistAndUpdateCalculationExactly() = check { model,repo ->
        SettingStep.entries.forEach{model.step(it,true)};runCurrent()
        assertEquals(0,decimal("1300").compareTo(repo.settings.heatingPower));assertEquals(0,decimal("2").compareTo(repo.settings.heatingMinutes))
        assertEquals(0,decimal("0.30").compareTo(repo.settings.machineRate));assertEquals(0,decimal("3.5").compareTo(repo.settings.saleMultiplier))
        assertEquals(CalculatorInput().calculate(model.state.value.selected,repo.settings),model.state.value.result)
    }
    @Test fun invalidEditorCannotCommitAndEmptyActiveListHasNoResult() = check { model,repo ->
        model.edit();model.saveEditor();runCurrent();assertEquals(7,repo.filaments.size)
        repo.filaments.toList().forEach { model.toggleActive(it);runCurrent() }
        assertNull(model.state.value.selected);assertNull(model.state.value.result)
    }
}

