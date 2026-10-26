package com.joseamc91.makertally

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.SavedStateHandle
import com.joseamc91.makertally.data.*
import com.joseamc91.makertally.domain.*
import com.joseamc91.makertally.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingTests {
    private fun configured() = OnboardingState(step=SetupStep.Complete,language="es-ES",
        electricity="0,1349",tax="0",heatingPower="1200",heatingMinutes="1",machineRate="0,25",multiplier="3")

    @Test fun newStorageStartsWelcomeWithoutPersonalSettingsOrAnyFilaments() = runTest {
        disk { store,files ->
            val loaded=PreferencesRepository(store,files,initialLanguage="es-ES").load()
            assertFalse(loaded.onboarding.completed);assertEquals(SetupStep.Welcome,loaded.onboarding.step)
            assertEquals("es-ES",loaded.settings.language);assertEquals(ThemeMode.System,loaded.settings.theme)
            assertEquals(neutralSettings("es-ES"),loaded.settings);assertTrue(loaded.filaments.isEmpty())
            assertFalse(files.data.containsKey("filaments.json"));assertNull(store.data.first()[stringPreferencesKey("settings_json")])
            assertTrue(listOf(loaded.onboarding.electricity,loaded.onboarding.tax,loaded.onboarding.heatingPower,
                loaded.onboarding.heatingMinutes,loaded.onboarding.machineRate,loaded.onboarding.multiplier).all{it.isEmpty()})
        }
    }
    @Test fun welcomeNextAndBackFollowAllStepsWithoutCompletingEarly() {
        var s=configured().copy(step=SetupStep.Welcome)
        SetupStep.entries.forEach { step-> assertEquals(step,s.step);assertFalse(s.completed);s=s.next() }
        assertEquals(SetupStep.Complete,s.step)
        for(step in SetupStep.entries.reversed()) {assertEquals(step,s.step);s=s.back()}
        assertEquals(SetupStep.Welcome,s.step)
    }
    @Test fun requiredFieldsRejectBlankPartialNegativeAndInvalidNumbers() {
        listOf("",",",".","-1","abc").forEach { invalid ->
            assertFalse(configured().copy(step=SetupStep.Electricity,electricity=invalid).canAdvance)
            assertFalse(configured().copy(step=SetupStep.Electricity,tax=invalid).canAdvance)
            assertFalse(configured().copy(step=SetupStep.Costs,machineRate=invalid).canAdvance)
            assertFalse(configured().copy(step=SetupStep.Costs,multiplier=invalid).canAdvance)
        }
        assertFalse(configured().copy(multiplier="0.99").costsValid)
        assertFalse(configured().copy(step=SetupStep.Electricity,electricity=DECIMAL_LIMIT.toPlainString()+"0").canAdvance)
    }
    @Test fun zeroIsAllowedAndOptionalHeatingCanRemainBlankButNotInvalid() {
        val s=configured().copy(electricity="0",tax="0",heatingPower="",heatingMinutes="",machineRate="0",multiplier="1")
        assertTrue(s.canAdvance);assertEquals(neutralSettings("es-ES"),s.settings())
        assertTrue(s.copy(heatingPower="0",heatingMinutes="0").canAdvance)
        listOf("-1",".",",").forEach { assertFalse(s.copy(heatingPower=it).printerValid);assertFalse(s.copy(heatingMinutes=it).printerValid) }
    }
    @Test fun decimalSeparatorsAndTaxPrecisionAreRetainedWithoutRounding() {
        val s=configured().copy(electricity="0.122",tax="27,186400000000000000000000000000001")
        assertEquals(decimal("0.122"),s.settings().electricityPrice)
        assertEquals(decimal("27.186400000000000000000000000000001"),s.settings().electricityTaxPercent)
        assertEquals(s,DataCodec.decodeOnboarding(DataCodec.encodeOnboarding(s)))
    }
    @Test fun draftStepAndIncompleteTextSurviveClosingAndReopeningRealDataStore() = runTest {
        val dir=kotlin.io.path.createTempDirectory("setup-reopen").toFile()
        val files=MemoryFiles();val expected=configured().copy(step=SetupStep.Printer,heatingPower=",",heatingMinutes="")
        try {
            repeat(2) { pass ->
                val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
                try {
                    val store=PreferenceDataStoreFactory.create(scope=scope,produceFile={File(dir,"settings.preferences_pb")})
                    val repo=PreferencesRepository(store,files)
                    if(pass==0) repo.saveOnboarding(expected)
                    else { val loaded=repo.load();assertEquals(expected,loaded.onboarding);assertTrue(loaded.filaments.isEmpty()) }
                } finally {scope.cancel();scope.coroutineContext[Job]?.join()}
            }
            assertTrue(files.data.isEmpty())
        } finally {dir.deleteRecursively()}
    }
    @Test fun completionCreatesOnlyOneExactGenericProfileAndCommitsChosenValues() = runTest {
        disk { store,files ->
            val repo=PreferencesRepository(store,files);val s=configured()
            repo.saveOnboarding(s);assertFalse(files.data.containsKey("filaments.json"))
            val loaded=repo.completeOnboarding(s)
            assertEquals(s.settings(),loaded.settings);assertTrue(loaded.onboarding.completed)
            assertFalse(loaded.onboarding.filamentIntroSeen)
            val f=loaded.filaments.single();assertEquals(s.genericFilament(),f)
            assertEquals("PLA · Generic",f.displayName);assertEquals(decimal("14.99"),f.pricePerKg)
            assertEquals(loaded,repo.load())
        }
    }
    @Test fun partialCompletionRetriedAfterIoFailureNeverDuplicatesOrMarksCompleteEarly() = runTest {
        disk { store,files ->
            val failing=object:DataStore<Preferences> {
                var fail=false
                override val data:Flow<Preferences> = store.data
                override suspend fun updateData(transform:suspend(Preferences)->Preferences):Preferences {
                    if(fail)throw IOException("test write")
                    return store.updateData(transform)
                }
            }
            val repo=PreferencesRepository(failing,files);val s=configured();repo.saveOnboarding(s)
            failing.fail=true
            try {repo.completeOnboarding(s);fail("Expected IO failure")}catch(_:IOException){}
            assertEquals(1,DataCodec.decodeFilaments(files.data.getValue("filaments.json")).size)
            failing.fail=false
            assertFalse(repo.load().onboarding.completed)
            val done=repo.completeOnboarding(s);assertEquals(listOf(s.genericFilament()),done.filaments)
        }
    }
    @Test fun completionDoesNotOverwriteAnUnexpectedExistingLibraryEvenWhenEmpty() = runTest {
        disk { store,files ->
            val repo=PreferencesRepository(store,files);val s=configured();repo.saveOnboarding(s)
            files.data["filaments.json"]="[]"
            assertTrue(repo.completeOnboarding(s).filaments.isEmpty());assertEquals("[]",files.data["filaments.json"])
        }
    }
    @Test fun legacySettingsLibraryActivationLocaleThemeSortAndReferenceMathRemainIdentical() = runTest {
        disk { store,files ->
            val settings=AppSettings(language="en-US",theme=ThemeMode.Dark,filamentSort=FilamentSort.PriceDescending)
            val profiles=Defaults.filaments().mapIndexed {i,f ->f.copy(active=i!=0)}
            val settingsText=DataCodec.encodeSettings(settings);val libraryText=DataCodec.encodeFilaments(profiles)
            store.edit{it[stringPreferencesKey("settings_json")]=settingsText};files.data["filaments.json"]=libraryText
            val repo=PreferencesRepository(store,files,initialLanguage="es-ES");val loaded=repo.load()
            assertTrue(loaded.onboarding.completed);assertTrue(loaded.migratedLegacy);assertFalse(loaded.onboarding.newInstallation)
            assertTrue(loaded.onboarding.filamentIntroSeen);assertEquals(settings,loaded.settings);assertEquals(profiles,loaded.filaments)
            assertEquals(settingsText,store.data.first()[stringPreferencesKey("settings_json")]);assertEquals(libraryText,files.data["filaments.json"])
            val asa=profiles.first{it.material=="ASA"};val result=CalculatorInput().calculate(asa,loaded.settings)!!
            assertEquals("9.35",result.suggestedSalePrice.toPlainString());assertEquals("5.24",result.grossMargin.toPlainString())
            assertFalse(repo.load().migratedLegacy)
        }
    }
    @Test fun legacyIntentionallyEmptyLibraryRemainsEmptyAndConfigured() = runTest {
        disk { store,files ->
            files.data["filaments.json"]="[]"
            store.edit{it[stringPreferencesKey("settings_json")]=DataCodec.encodeSettings(AppSettings())}
            val loaded=PreferencesRepository(store,files).load()
            assertTrue(loaded.onboarding.completed);assertTrue(loaded.filaments.isEmpty());assertEquals("[]",files.data["filaments.json"])
        }
    }
    @Test fun physicalLegacySettingsFileCountsEvenIfItContainsNoSettingsKey() = runTest {
        disk { store,files ->
            val loaded=PreferencesRepository(store,files,priorSettingsFile=true).load()
            assertTrue(loaded.onboarding.completed);assertFalse(loaded.onboarding.newInstallation)
            assertTrue(loaded.filaments.isEmpty())
        }
    }
    @Test fun libraryAloneIncludingEmptyCountsAsLegacyRestoredData() = runTest {
        for(text in listOf("[]",DataCodec.encodeFilaments(Defaults.filaments()))) disk {store,files ->
            files.data["filaments.json"]=text
            val loaded=PreferencesRepository(store,files).load()
            assertTrue(loaded.onboarding.completed);assertEquals(text,files.data["filaments.json"])
        }
    }
    @Test fun unreadableLegacyStorageNeverStartsOnboardingOrOverwritesData() = runTest {
        disk {store,files ->
            files.failRead=true
            val loaded=PreferencesRepository(store,files).load()
            assertTrue(loaded.onboarding.completed);assertEquals(listOf(StorageNotice.ReadFailed),loaded.notices)
            assertTrue(files.data.isEmpty());assertNull(store.data.first()[stringPreferencesKey("onboarding_json")])
        }
    }
    @Test fun introRemainsPendingAcrossReopenUntilAcknowledgedAndNeverReturns() = runTest {
        disk {store,files ->
            val repo=PreferencesRepository(store,files);val setup=configured();repo.saveOnboarding(setup);repo.completeOnboarding(setup)
            val loaded=repo.load();assertFalse(loaded.onboarding.filamentIntroSeen)
            repo.saveOnboarding(loaded.onboarding.copy(filamentIntroSeen=true))
            assertTrue(PreferencesRepository(store,files).load().onboarding.filamentIntroSeen)
        }
    }
    @Test fun freshViewModelStartsBlankAndFinishesInFilamentsWithNoResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {disk {store,files ->
            val repo=PreferencesRepository(store,files);val model=MakerTallyViewModel(repo);runCurrent()
            // Repository IO uses Dispatchers.IO; await its ready state without arbitrary sleeps.
            awaitState(model) { it.ready }
            val loaded=model.state.value
            assertEquals(CalculatorInput("","","0"),loaded.input);assertNull(loaded.result)
            model.onboardingNext();advanceUntilIdle()
            awaitState(model) { !it.saving }
            assertEquals(SetupStep.Electricity,model.state.value.onboarding.step)
            model.onboardingField(SetupField.Electricity,"0");model.onboardingField(SetupField.Tax,"0")
            advanceUntilIdle();awaitState(model) { !it.saving };model.onboardingNext();advanceUntilIdle();awaitState(model) { !it.saving }
            assertEquals(SetupStep.Printer,model.state.value.onboarding.step)
            model.onboardingNext();advanceUntilIdle();awaitState(model) { !it.saving };model.onboardingField(SetupField.MachineRate,"0");model.onboardingField(SetupField.Multiplier,"1")
            advanceUntilIdle();awaitState(model) { !it.saving };model.onboardingNext();advanceUntilIdle();awaitState(model) { !it.saving };model.completeOnboarding();advanceUntilIdle()
            awaitState(model) { !it.saving }
            assertTrue(model.state.value.onboarding.completed);assertEquals(Destination.Filaments,model.state.value.destination)
            assertEquals("Generic",model.state.value.selected!!.brand);assertNull(model.state.value.result)
            assertEquals(CalculatorInput("","","0"),model.state.value.input)
        }} finally {Dispatchers.resetMain()}
    }
    @Test fun migratedViewModelDoesNotReplaceStoredLanguageWithTheSystemLanguage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {disk {store,files ->
            val settings=AppSettings(language="es-ES",theme=ThemeMode.Dark)
            store.edit{it[stringPreferencesKey("settings_json")]=DataCodec.encodeSettings(settings)}
            files.data["filaments.json"]=DataCodec.encodeFilaments(Defaults.filaments())
            val model=MakerTallyViewModel(PreferencesRepository(store,files),SavedStateHandle(),"en-US")
            awaitState(model) { it.ready }
            assertEquals(settings,model.state.value.settings);assertEquals("9.35",model.state.value.result!!.suggestedSalePrice.toPlainString())
        }}finally {Dispatchers.resetMain()}
    }
    @Test fun printingPowerStillChangesElectricityAndPricePerKgRemainsExact() {
        val f=configured().genericFilament();val settings=configured().settings()
        assertEquals(decimal("14.99"),f.pricePerKg)
        val low=CalculatorInput("100","1","0").calculate(f,settings)!!
        val high=CalculatorInput("100","1","0").calculate(f.copy(printPower=decimal("180")),settings)!!
        assertTrue(high.electricityCost>low.electricityCost)
        assertEquals(f.pricePerKg,EditorDraft(f).copy(power="180").build()!!.pricePerKg)
        assertNull(EditorDraft(material="PLA",brand="Test",price="10").build())
    }
    @Test fun acknowledgedIntroAndDeletedGenericDoNotReturnAfterViewModelRestart() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {disk {store,files ->
            val repo=PreferencesRepository(store,files);val setup=configured()
            repo.saveOnboarding(setup);repo.completeOnboarding(setup)
            val first=MakerTallyViewModel(repo);awaitState(first) { it.ready }
            assertEquals(Destination.Filaments,first.state.value.destination)
            assertFalse(first.state.value.onboarding.filamentIntroSeen)
            first.acknowledgeFilamentIntro();awaitState(first) { !it.saving }
            first.askDelete(first.state.value.filaments.single());first.confirmDelete();awaitState(first) { !it.saving }
            val restarted=MakerTallyViewModel(repo);awaitState(restarted) { it.ready }
            assertTrue(restarted.state.value.onboarding.completed);assertTrue(restarted.state.value.onboarding.filamentIntroSeen)
            assertEquals(Destination.Calculator,restarted.state.value.destination)
            assertTrue(restarted.state.value.filaments.isEmpty());assertNull(restarted.state.value.result)
            assertEquals(CalculatorInput("","","0"),restarted.state.value.input)
        }}finally {Dispatchers.resetMain()}
    }

    private suspend fun disk(block:suspend(DataStore<Preferences>,MemoryFiles)->Unit) {
        val dir=kotlin.io.path.createTempDirectory("onboarding-test").toFile()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try { val store=PreferenceDataStoreFactory.create(scope=scope,produceFile={File(dir,"settings.preferences_pb")});block(store,MemoryFiles()) }
        finally {scope.cancel();scope.coroutineContext[Job]?.join();dir.deleteRecursively()}
    }
    private suspend fun awaitState(model:MakerTallyViewModel,condition:(UiState)->Boolean) = withContext(Dispatchers.Default) {
        withTimeout(10000) { model.state.first(condition) }
    }
}
