package com.joseamc91.makertally.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joseamc91.makertally.R
import com.joseamc91.makertally.data.*
import com.joseamc91.makertally.domain.NumericInput
import java.math.BigDecimal

@Composable internal fun OnboardingPage(state: UiState, model: MakerTallyViewModel) {
    val setup=state.onboarding
    val scroll=rememberScrollState()
    LaunchedEffect(setup.step) { scroll.scrollTo(0) }
    val title=when(setup.step) {
        SetupStep.Welcome -> R.string.app_name
        SetupStep.Electricity -> R.string.setup_electricity_title
        SetupStep.Printer -> R.string.setup_printer_title
        SetupStep.Costs -> R.string.setup_costs_title
        SetupStep.Complete -> R.string.setup_complete_title
    }
    BackHandler(enabled=setup.step!=SetupStep.Welcome) { if(!state.saving)model.onboardingBack() }
    Column(Modifier.fillMaxSize().background(MakerTallyNavy).safeDrawingPadding().imePadding()
        .testTag("onboarding_step_${setup.step.name.lowercase()}")) {
        Text(stringResource(title),Modifier.fillMaxWidth().padding(16.dp),color=MakerTallyWhite,
            style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        Surface(Modifier.weight(1f).fillMaxWidth(),color=MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // These explanation-heavy steps need more scroll space at short heights.
                // Keep the approved footer placement on the other steps.
                val compact=maxHeight<300.dp || (maxHeight<480.dp &&
                    setup.step in listOf(SetupStep.Electricity,SetupStep.Printer,SetupStep.Costs))
                val bookend=setup.step==SetupStep.Welcome || setup.step==SetupStep.Complete
                Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().testTag("onboarding_content").verticalScroll(scroll).padding(vertical=12.dp),
                        verticalArrangement=Arrangement.spacedBy(12.dp,if(bookend && !compact)Alignment.CenterVertically else Alignment.Top)) {
                        OnboardingContent(setup,model)
                        if(compact) Reassurance()
                    }
                    if(!compact) Reassurance()
                    Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(setup.step!=SetupStep.Welcome) OutlinedButton(onClick=model::onboardingBack,enabled=!state.saving,
                            modifier=Modifier.heightIn(min=48.dp).testTag("onboarding_back")) { Text(stringResource(R.string.setup_back)) }
                        Spacer(Modifier.weight(1f))
                        Button(onClick=if(setup.step==SetupStep.Complete)model::completeOnboarding else model::onboardingNext,
                            enabled=!state.saving && setup.canAdvance,modifier=Modifier.heightIn(min=48.dp).testTag("onboarding_next")) {
                            Text(stringResource(when(setup.step) {
                                SetupStep.Welcome -> R.string.setup_start
                                SetupStep.Complete -> R.string.setup_open_filaments
                                else -> R.string.setup_next
                            }))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Reassurance() {
    SetupHelp(Modifier.testTag("onboarding_reassurance")) {
        Text(stringResource(R.string.setup_reassurance),
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SetupHelp(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Surface(modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,
        color=MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
    }
}

@Composable private fun OnboardingContent(setup: OnboardingState, model: MakerTallyViewModel) {
    val second=remember { FocusRequester() }
    val pair:@Composable (@Composable (Modifier)->Unit,@Composable (Modifier)->Unit)->Unit={first,last ->
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if(maxWidth>=560.dp) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) { first(Modifier.weight(1f));last(Modifier.weight(1f)) }
            else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { first(Modifier);last(Modifier) }
        }
    }
    when(setup.step) {
        SetupStep.Welcome -> {
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(20.dp)) {
                    val wide=maxWidth>=560.dp
                    val mark:@Composable ()->Unit={Surface(shape=MaterialTheme.shapes.medium,color=MakerTallyNavy) {
                        Image(painterResource(R.drawable.ic_makertally_brand_mark),contentDescription=null,modifier=Modifier.padding(12.dp).size(56.dp))
                    }}
                    val introduction:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=if(wide)Alignment.Start else Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.setup_welcome),style=MaterialTheme.typography.bodyLarge,textAlign=if(wide)TextAlign.Start else TextAlign.Center)
                        Text(stringResource(R.string.setup_material_energy_machine),style=MaterialTheme.typography.titleSmall,
                            color=MaterialTheme.colorScheme.primary,textAlign=if(wide)TextAlign.Start else TextAlign.Center)
                    }}
                    if(wide) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        mark();Box(Modifier.weight(1f)){introduction()}
                    } else Column(verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {mark();introduction()}
                }
            }
        }
        SetupStep.Electricity -> {
            Text(stringResource(R.string.setup_electricity_intro))
            pair({ m -> SetupInput(setup.electricity,{model.onboardingField(SetupField.Electricity,it)},R.string.setup_electricity_price,R.string.input_per_kwh,
                "onboarding_electricity",NumericInput.nonNegative(setup.electricity)!=null,m,next=second) },
                { m -> SetupInput(setup.tax,{model.onboardingField(SetupField.Tax,it)},R.string.setup_effective_taxes,R.string.unit_percent,
                    "onboarding_tax",NumericInput.percentage(setup.tax)!=null,m.focusRequester(second)) })
            SetupHelp(Modifier.testTag("onboarding_tax_help")) {
                Text(stringResource(R.string.setup_tax_help),style=MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.setup_tax_explanation),style=MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.setup_tax_formula),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,
                    modifier=Modifier.testTag("onboarding_tax_formula"))
                Text(stringResource(R.string.setup_tax_included),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SetupStep.Printer -> {
            Text(stringResource(R.string.setup_printer_intro))
            pair({ m -> SetupInput(setup.heatingPower,{model.onboardingField(SetupField.HeatingPower,it)},R.string.setup_heating_power,R.string.unit_w,
                "onboarding_heating_power",NumericInput.nonNegative(setup.heatingPower)!=null,m,next=second) },
                { m -> SetupInput(setup.heatingMinutes,{model.onboardingField(SetupField.HeatingMinutes,it)},R.string.setup_heating_duration,R.string.unit_min,
                    "onboarding_heating_minutes",NumericInput.nonNegative(setup.heatingMinutes)!=null,m.focusRequester(second)) })
            SetupHelp {
                Text(stringResource(R.string.setup_heating_reference),style=MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.setup_heating_example),style=MaterialTheme.typography.bodyMedium,
                    fontWeight=FontWeight.SemiBold,modifier=Modifier.testTag("onboarding_heating_example"))
                Text(stringResource(R.string.setup_heating_caution),style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.testTag("onboarding_heating_caution"))
            }
        }
        SetupStep.Costs -> {
            // Each explanation stays associated with its input, including when the pair is wide.
            pair({ m -> Column(m,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.machine_rate),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_machine_intro))
                SetupInput(setup.machineRate,{model.onboardingField(SetupField.MachineRate,it)},R.string.machine_rate,R.string.input_per_hour,
                    "onboarding_machine",NumericInput.nonNegative(setup.machineRate)!=null,next=second)
                SetupHelp {Text(stringResource(R.string.setup_machine_reference),style=MaterialTheme.typography.bodyMedium,
                    modifier=Modifier.testTag("onboarding_machine_reference"))}
            } }, { m -> Column(m,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.sale_multiplier),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_multiplier_intro))
                SetupInput(setup.multiplier,{model.onboardingField(SetupField.Multiplier,it)},R.string.sale_multiplier,null,
                    "onboarding_multiplier",NumericInput.nonNegative(setup.multiplier)?.let{it>=BigDecimal.ONE}==true,
                    Modifier.focusRequester(second),error=R.string.setup_multiplier_error)
                SetupHelp {Text(stringResource(R.string.setup_multiplier_reference),style=MaterialTheme.typography.bodyMedium,
                    modifier=Modifier.testTag("onboarding_multiplier_reference"))}
            } })
        }
        SetupStep.Complete -> {
            Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.setup_complete_intro),style=MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.setup_generic_explanation),style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable private fun SetupInput(value:String,onChange:(String)->Unit,label:Int,unit:Int?,tag:String,valid:Boolean,
    modifier:Modifier=Modifier,next:FocusRequester?=null,error:Int=R.string.valid_number) {
    val focus=LocalFocusManager.current
    OutlinedTextField(value,onChange,label={Text(stringResource(label))},suffix=unit?.let{{Text(stringResource(it))}},singleLine=true,
        isError=value.isNotEmpty()&&!valid,supportingText=if(value.isNotEmpty()&&!valid){{Text(stringResource(error))}}else null,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal,imeAction=if(next==null)ImeAction.Done else ImeAction.Next),
        keyboardActions=KeyboardActions(onNext={next?.requestFocus()},onDone={focus.clearFocus()}),
        modifier=modifier.fillMaxWidth().testTag(tag))
}

@Composable internal fun FilamentIntroDialog(state: UiState,model: MakerTallyViewModel) {
    AlertDialog(onDismissRequest={},title={Text(stringResource(R.string.setup_filaments))},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.filament_intro_message))
            Text(stringResource(R.string.filament_intro_profile),fontWeight=FontWeight.Medium)
            Text(stringResource(R.string.filament_intro_actions))
        }},confirmButton={TextButton(onClick=model::acknowledgeFilamentIntro,enabled=!state.saving,
            modifier=Modifier.heightIn(min=48.dp).testTag("filament_intro_understood")){Text(stringResource(R.string.understood))}},
        modifier=Modifier.testTag("filament_intro").semantics { testTagsAsResourceId=true })
}
