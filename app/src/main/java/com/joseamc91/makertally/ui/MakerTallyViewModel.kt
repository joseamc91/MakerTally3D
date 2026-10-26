package com.joseamc91.makertally.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joseamc91.makertally.data.*
import com.joseamc91.makertally.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.io.IOException

enum class Destination { Calculator, Filaments, Settings }
enum class SettingStep { HeatingPower, HeatingMinutes, MachineRate, Multiplier }
data class UiState(
    val ready: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val filaments: List<Filament> = emptyList(),
    val selectedId: String? = null,
    val input: CalculatorInput = CalculatorInput(),
    val electricityText: String = "0,1349",
    val electricityTaxText: String = "0",
    val destination: Destination = Destination.Calculator,
    val detailsOpen: Boolean = false,
    val editor: EditorDraft? = null,
    val deleting: Filament? = null,
    val notice: StorageNotice? = null,
    val saving: Boolean = false,
    val onboarding: OnboardingState = OnboardingState.legacy()
) {
    val selected get() = preferredFilament(filaments, selectedId)
    val electricityValid get() = NumericInput.nonNegative(electricityText) != null
    val electricityTaxValid get() = NumericInput.percentage(electricityTaxText) != null
    val result get() = input.calculate(selected, settings, electricityValid && electricityTaxValid)
}

class MakerTallyViewModel(private val repository: AppRepository, private val saved: SavedStateHandle = SavedStateHandle(), private val platformLanguage: String? = null) : ViewModel() {
    private val _state = MutableStateFlow(UiState(
        input = CalculatorInput(saved["weight"] ?: "141", saved["hours"] ?: "6", saved["minutes"] ?: "0"),
        destination = Destination.entries.getOrElse(saved["tab"] ?: 0) { Destination.Calculator },
        detailsOpen = saved["details"] ?: false,
        selectedId = saved["selected"]
    ))
    val state = _state.asStateFlow()
    private val writes = Mutex()
    private var setupWrites = 0
    init {
        viewModelScope.launch {
            var loaded = repository.load()
            if (loaded.onboarding.completed && !loaded.migratedLegacy && platformLanguage in listOf("es-ES", "en-US") && platformLanguage != loaded.settings.language) {
                val settings = loaded.settings.copy(language = platformLanguage!!)
                try { repository.saveSettings(settings); loaded = loaded.copy(settings = settings) }
                catch (_: IOException) { loaded = loaded.copy(notices = loaded.notices + StorageNotice.SaveFailed) }
            }
            val selected = preferredFilament(loaded.filaments, _state.value.selectedId
                ?: if(loaded.onboarding.newInstallation) loaded.onboarding.genericId
                else loaded.filaments.firstOrNull { it.material == "ASA" && it.brand == "eSun" && it.active }?.id)
            val fresh = loaded.onboarding.newInstallation
            val input = CalculatorInput(saved["weight"] ?: if(fresh) "" else "141",
                saved["hours"] ?: if(fresh) "" else "6",saved["minutes"] ?: "0")
            _state.value = _state.value.copy(ready = true, settings = loaded.settings, filaments = loaded.filaments,
                selectedId = selected?.id, input = input.localized(loaded.settings.language),onboarding=loaded.onboarding,
                destination=if(fresh && loaded.onboarding.completed && !loaded.onboarding.filamentIntroSeen) Destination.Filaments else _state.value.destination,
                electricityText = saved["electricity"] ?: Formatting.input(loaded.settings.electricityPrice, loaded.settings.language),
                electricityTaxText = saved["electricityTax"] ?: Formatting.input(loaded.settings.electricityTaxPercent, loaded.settings.language),
                notice = loaded.notices.firstOrNull())
        }
    }
    fun navigate(destination: Destination) { saved["tab"] = destination.ordinal; update { it.copy(destination = destination) } }
    fun toggleDetails() { update { it.copy(detailsOpen = !it.detailsOpen) }; saved["details"] = _state.value.detailsOpen }
    fun select(id: String) { saved["selected"] = id; update { it.copy(selectedId = id) } }
    fun input(weight: String? = null, hours: String? = null, minutes: String? = null) {
        update { it.copy(input = it.input.copy(weight = weight ?: it.input.weight, hours = hours ?: it.input.hours, minutes = minutes ?: it.input.minutes)) }
        saved["weight"] = _state.value.input.weight; saved["hours"] = _state.value.input.hours; saved["minutes"] = _state.value.input.minutes
    }
    fun electricity(text: String) {
        saved["electricity"] = text
        update { it.copy(electricityText = text) }
        NumericInput.nonNegative(text)?.let { value -> changeSettings { it.copy(electricityPrice = value) } }
    }
    fun electricityTax(text: String) {
        saved["electricityTax"] = text
        update { it.copy(electricityTaxText = text) }
        NumericInput.percentage(text)?.let { value -> changeSettings { it.copy(electricityTaxPercent = value) } }
    }
    fun language(language: String) = changeSettings { it.copy(language = language) }
    fun theme(theme: ThemeMode) = changeSettings { it.copy(theme = theme) }
    fun sort(mode: FilamentSort) = changeSettings { it.copy(filamentSort = mode) }
    fun step(setting: SettingStep, up: Boolean) {
        changeSettings { settings ->
            fun next(value: BigDecimal, delta: String, minimum: BigDecimal = BigDecimal.ZERO) =
                SettingsSteps.change(value, decimal(delta) * (if (up) BigDecimal.ONE else -BigDecimal.ONE), minimum)
            when (setting) {
                SettingStep.HeatingPower -> settings.copy(heatingPower = next(settings.heatingPower, "100"))
                SettingStep.HeatingMinutes -> settings.copy(heatingMinutes = next(settings.heatingMinutes, "1"))
                SettingStep.MachineRate -> settings.copy(machineRate = next(settings.machineRate, "0.05"))
                SettingStep.Multiplier -> settings.copy(saleMultiplier = next(settings.saleMultiplier, "0.5", BigDecimal.ONE))
            }
        }
    }
    private fun changeSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            writes.withLock {
                val before = _state.value
                val settings = runCatching { transform(before.settings) }.getOrNull()?.takeIf { it.isValid() } ?: return@withLock
                try {
                    repository.saveSettings(settings)
                    val languageChanged = settings.language != before.settings.language
                    update { current -> current.copy(settings = settings,
                        input = if (languageChanged) current.input.localized(settings.language) else current.input,
                        electricityText = if (languageChanged) NumericInput.parse(current.electricityText)?.let { Formatting.input(it, settings.language) } ?: current.electricityText else current.electricityText,
                        electricityTaxText = if (languageChanged) NumericInput.percentage(current.electricityTaxText)?.let { Formatting.input(it, settings.language) } ?: current.electricityTaxText else current.electricityTaxText) }
                    saved["electricity"] = _state.value.electricityText
                    saved["electricityTax"] = _state.value.electricityTaxText
                } catch (_: IOException) { update { it.copy(notice = StorageNotice.SaveFailed) } }
            }
        }
    }
    fun edit(filament: Filament? = null) { update { it.copy(editor = EditorDraft(filament)) } }
    fun editor(draft: EditorDraft) { update { it.copy(editor = draft) } }
    fun closeEditor() { if (!_state.value.saving) update { it.copy(editor = null) } }
    fun saveEditor() {
        val filament = _state.value.editor?.build() ?: return
        mutateLibrary { profiles -> if (profiles.any { it.id == filament.id }) profiles.map { if (it.id == filament.id) filament else it } else profiles + filament }
    }
    fun toggleActive(filament: Filament) = mutateLibrary { profiles -> profiles.map { if (it.id == filament.id) it.copy(active = !it.active) else it } }
    fun askDelete(filament: Filament) { update { it.copy(deleting = filament) } }
    fun cancelDelete() { update { it.copy(deleting = null) } }
    fun confirmDelete() { val id = _state.value.deleting?.id ?: return; mutateLibrary { it.filterNot { profile -> profile.id == id } } }
    private fun mutateLibrary(transform: (List<Filament>) -> List<Filament>) {
        if (_state.value.saving) return
        update { it.copy(saving = true) }
        viewModelScope.launch {
            writes.withLock {
                val profiles = transform(_state.value.filaments)
                try {
                    repository.saveFilaments(profiles)
                    update { it.copy(filaments = profiles, selectedId = preferredFilament(profiles, it.selectedId)?.id, editor = null, deleting = null) }
                } catch (_: IOException) { update { it.copy(notice = StorageNotice.SaveFailed) } }
                finally { update { it.copy(saving = false) } }
            }
        }
    }
    fun dismissNotice() { update { it.copy(notice = null) } }
    fun onboardingField(field: SetupField, text: String) = persistSetup(_state.value.onboarding.field(field,text))
    fun onboardingBack() {
        if(!_state.value.saving) persistSetup(_state.value.onboarding.back())
    }
    fun onboardingNext() {
        val current = _state.value
        if(!current.saving && current.onboarding.canAdvance) persistSetup(current.onboarding.next())
    }
    private fun persistSetup(next: OnboardingState) {
        setupWrites++
        update { it.copy(onboarding=next,saving=true) }
        viewModelScope.launch {
            writes.withLock {
                try { repository.saveOnboarding(next) }
                catch (_: IOException) { update { it.copy(notice=StorageNotice.SaveFailed) } }
                finally { setupWrites--;update { it.copy(saving=setupWrites>0) } }
            }
        }
    }
    fun completeOnboarding() {
        val current = _state.value
        if(current.saving || current.onboarding.completed || current.onboarding.step != SetupStep.Complete || !current.onboarding.canAdvance) return
        update { it.copy(saving=true) }
        viewModelScope.launch {
            writes.withLock {
                try {
                    val loaded = repository.completeOnboarding(current.onboarding)
                    saved["weight"]="";saved["hours"]="";saved["minutes"]="0";saved["tab"]=Destination.Filaments.ordinal
                    update { it.copy(settings=loaded.settings,filaments=loaded.filaments,onboarding=loaded.onboarding,
                        input=CalculatorInput("","","0"),selectedId=preferredFilament(loaded.filaments,loaded.onboarding.genericId)?.id,
                        destination=Destination.Filaments,electricityText=Formatting.input(loaded.settings.electricityPrice,loaded.settings.language),
                        electricityTaxText=Formatting.input(loaded.settings.electricityTaxPercent,loaded.settings.language)) }
                } catch (_: IOException) { update { it.copy(notice=StorageNotice.SaveFailed) } }
                finally { update { it.copy(saving=false) } }
            }
        }
    }
    fun acknowledgeFilamentIntro() = persistSetup(_state.value.onboarding.copy(filamentIntroSeen=true))
    private inline fun update(transform: (UiState) -> UiState) { _state.value = transform(_state.value) }
}

