package com.joseamc91.makertally.ui

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joseamc91.makertally.R
import com.joseamc91.makertally.data.StorageNotice
import com.joseamc91.makertally.domain.*
import java.math.BigDecimal

private val lightColors = lightColorScheme(primary=Color(0xFF205DBC), onPrimary=Color.White,
    primaryContainer=Color(0xFFDCE8FF),onPrimaryContainer=Color(0xFF15315C),
    secondaryContainer=Color(0xFFDCE8FF),onSecondaryContainer=Color(0xFF15315C),
    background=Color(0xFFF4F6FA),surface=Color(0xFFFDFDFE),surfaceContainer=Color(0xFFE9EDF5))
private val darkColors = darkColorScheme(primary=Color(0xFFA8C7FF),onPrimary=Color(0xFF123260),
    primaryContainer=Color(0xFF254777),onPrimaryContainer=Color(0xFFDCE8FF),
    secondaryContainer=Color(0xFF254777),onSecondaryContainer=Color(0xFFDCE8FF),
    background=Color(0xFF11151D),surface=Color(0xFF191D26),surfaceContainer=Color(0xFF232A36))

@Composable fun MakerTallyApp(model: MakerTallyViewModel, applyLocale: (String) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val dark = when(state.settings.theme) { ThemeMode.System -> isSystemInDarkTheme();ThemeMode.Light -> false;ThemeMode.Dark -> true }
    val view=LocalView.current
    SideEffect { (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it,view).apply {
        isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark
    } } }
    LaunchedEffect(state.ready,state.settings.language) { if(state.ready)applyLocale(state.settings.language) }
    MaterialTheme(colorScheme=if(dark)darkColors else lightColors) {
        Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId=true }) {
            if (!state.ready) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            else Scaffold(
                containerColor=MaterialTheme.colorScheme.background,
                bottomBar={ NavigationBar {
                    Destination.entries.forEach { destination ->
                        val title=stringResource(destination.title())
                        NavigationBarItem(selected=state.destination==destination,onClick={model.navigate(destination)},
                            icon={ NavigationIcon(destination) },label={Text(title,maxLines=1)},
                            modifier=Modifier.testTag("nav_${destination.name.lowercase()}"))
                    }
                } }
            ) { insets ->
                Box(Modifier.padding(insets).fillMaxSize()) {
                    when(state.destination) {
                        Destination.Calculator -> CalculatorPage(state,model)
                        Destination.Filaments -> FilamentsPage(state,model)
                        Destination.Settings -> SettingsPage(state,model)
                    }
                }
                state.notice?.let { notice -> AlertDialog(onDismissRequest=model::dismissNotice,
                    text={Text(stringResource(when(notice){StorageNotice.InvalidData->R.string.invalid_data;StorageNotice.ReadFailed->R.string.read_failed;StorageNotice.SaveFailed->R.string.save_failed}))},
                    confirmButton={TextButton(onClick=model::dismissNotice){Text(stringResource(R.string.dismiss))}}) }
                state.editor?.let { EditorDialog(it,state,model) }
                state.deleting?.let { filament -> AlertDialog(onDismissRequest=model::cancelDelete,
                    title={Text(stringResource(R.string.delete_title))},text={Text(filament.displayName+"\n\n"+stringResource(R.string.delete_message))},
                    dismissButton={TextButton(onClick=model::cancelDelete){Text(stringResource(R.string.cancel))}},
                    confirmButton={TextButton(onClick=model::confirmDelete,enabled=!state.saving){Text(stringResource(R.string.delete))}}) }
            }
        }
    }
}
private fun Destination.title() = when(this){Destination.Calculator->R.string.calculator;Destination.Filaments->R.string.filaments;Destination.Settings->R.string.settings}
@Composable private fun NavigationIcon(destination:Destination) {
    // Small geometric native vector icons; no icon SDK or external assets required.
    val ink=LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=2.dp.toPx())

        when(destination) {
            Destination.Calculator -> {
                drawRoundRect(ink,topLeft=androidx.compose.ui.geometry.Offset(size.width*.2f,0f),size=androidx.compose.ui.geometry.Size(size.width*.6f,size.height),cornerRadius=androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),style=stroke)
                drawLine(ink,androidx.compose.ui.geometry.Offset(size.width*.3f,size.height*.3f),androidx.compose.ui.geometry.Offset(size.width*.7f,size.height*.3f),2.dp.toPx())
                listOf(.5f,.75f).forEach { y->listOf(.35f,.65f).forEach { x->drawCircle(ink,1.5.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width*x,size.height*y)) } }
            }
            Destination.Filaments -> { drawCircle(ink,size.width*.42f,style=stroke);drawCircle(ink,size.width*.16f,style=stroke) }
            Destination.Settings -> {
                drawCircle(ink,size.width*.32f,style=stroke);drawCircle(ink,size.width*.10f,style=stroke)
                drawLine(ink,androidx.compose.ui.geometry.Offset(size.width/2,0f),androidx.compose.ui.geometry.Offset(size.width/2,size.height*.2f),2.dp.toPx())
                drawLine(ink,androidx.compose.ui.geometry.Offset(size.width/2,size.height*.8f),androidx.compose.ui.geometry.Offset(size.width/2,size.height),2.dp.toPx())
                drawLine(ink,androidx.compose.ui.geometry.Offset(0f,size.height/2),androidx.compose.ui.geometry.Offset(size.width*.2f,size.height/2),2.dp.toPx())
                drawLine(ink,androidx.compose.ui.geometry.Offset(size.width*.8f,size.height/2),androidx.compose.ui.geometry.Offset(size.width,size.height/2),2.dp.toPx())
            }
        }
    }
}
@Composable private fun PageTitle(resource:Int) { Text(stringResource(resource),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold) }
@Composable private fun SoftCard(content:@Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}
@Composable private fun CostRow(label:Int,value:String,important:Boolean=false,secondary:Boolean=false) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(stringResource(label),Modifier.weight(1f),style=if(secondary)MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight=if(important)FontWeight.SemiBold else FontWeight.Normal,color=if(secondary)MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        Text(value,style=if(important)MaterialTheme.typography.titleMedium else if(secondary)MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight=if(important)FontWeight.Bold else FontWeight.Medium,color=if(secondary)MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}
@Composable private fun NumberInput(value:String,onChange:(String)->Unit,label:Int,unit:Int?,tag:String,error:Boolean=false,errorLabel:Int=R.string.valid_number,integer:Boolean=false,modifier:Modifier=Modifier) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(stringResource(label))},singleLine=true,
        suffix=unit?.let{{Text(stringResource(it))}},isError=error,
        supportingText=if(error){{Text(stringResource(errorLabel))}}else null,
        keyboardOptions=KeyboardOptions(keyboardType=if(integer)KeyboardType.Number else KeyboardType.Decimal),
        shape=RoundedCornerShape(12.dp),modifier=modifier.fillMaxWidth().testTag(tag))
}
@Composable private fun CalculatorPage(state:UiState,model:MakerTallyViewModel) {
    val result=state.result;val lang=state.settings.language
    fun money(value:BigDecimal?,places:Int=2)=Formatting.money(value,lang,places)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        PageTitle(R.string.calculator)
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(horizontal=20.dp,vertical=18.dp)) {
                Text(stringResource(R.string.suggested_price),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)
                Text(money(result?.suggestedSalePrice),fontSize=48.sp,lineHeight=56.sp,fontWeight=FontWeight.Bold,
                    color=MaterialTheme.colorScheme.onPrimaryContainer,modifier=Modifier.testTag("suggested_price"),maxLines=1)
            }
        }
        SoftCard {
            Text(stringResource(R.string.new_print),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            var choosing by remember { mutableStateOf(false) }
            Text(stringResource(R.string.filament),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                OutlinedButton(onClick={choosing=true},Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("filament_picker"),shape=RoundedCornerShape(12.dp)) {
                    Text(state.selected?.displayName ?: stringResource(R.string.no_active_filaments),Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("⌄")
                }
                DropdownMenu(expanded=choosing,onDismissRequest={choosing=false}) {
                    state.filaments.filter{it.active}.forEach { f-> DropdownMenuItem(text={Text(f.displayName)},onClick={model.select(f.id);choosing=false}) }
                }
            }
            NumberInput(state.input.weight,{model.input(weight=it)},R.string.piece_weight,R.string.unit_g,"piece_weight",NumericInput.nonNegative(state.input.weight)==null)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                NumberInput(state.input.hours,{model.input(hours=it)},R.string.hours,R.string.unit_h,"hours",NumericInput.nonNegative(state.input.hours,integer=true)==null,R.string.whole_hours,true,Modifier.weight(1f))
                NumberInput(state.input.minutes,{model.input(minutes=it)},R.string.minutes,R.string.unit_min,"minutes",NumericInput.nonNegative(state.input.minutes,integer=true,maximum=59)==null,R.string.minutes_range,true,Modifier.weight(1f))
            }
            if(result==null) Text(stringResource(R.string.invalid_result),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SoftCard {
            CostRow(R.string.piece_cost,money(result?.pieceCost),important=true)
            CostRow(R.string.material_cost,money(result?.materialCost))
            CostRow(R.string.electricity_cost,money(result?.electricityCost))
            CostRow(R.string.machine_additional,money(result?.machineCost),secondary=true)
        }
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(onClick=model::toggleDetails).testTag("details_toggle").padding(horizontal=4.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.calculation_details),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
            Text(if(state.detailsOpen)"⌄"else"›",fontSize=24.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(state.detailsOpen) SoftCard {
            CostRow(R.string.price_per_gram,money(result?.pricePerGram,4)+stringResource(R.string.unit_per_g))
            listOf(R.string.heating_energy to result?.heatingEnergy,R.string.printing_energy to result?.printingEnergy,R.string.total_energy to result?.totalEnergy).forEach { (label,value)->
                CostRow(label,if(value==null)"—"else Formatting.number(value,lang)+" "+stringResource(R.string.unit_kwh))
            }
            CostRow(R.string.electricity_detail,money(result?.electricityCost,4))
            CostRow(R.string.piece_detail,money(result?.pieceCost,5))
            CostRow(R.string.machine_rate,money(state.settings.machineRate)+stringResource(R.string.unit_per_h))
            CostRow(R.string.multiplier_used,"×"+Formatting.number(state.settings.saleMultiplier,lang,1))
            CostRow(R.string.gross_margin,money(result?.grossMargin),important=true)
        }
        Spacer(Modifier.height(4.dp))
    }
}
@Composable private fun FilamentsPage(state:UiState,model:MakerTallyViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Box(Modifier.padding(vertical=16.dp)){PageTitle(R.string.filaments)}
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=96.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(state.filaments.isEmpty()) item { Text(stringResource(R.string.no_filaments)) }
                items(state.filaments,key={it.id}) { f->
                    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(start=14.dp,end=10.dp,top=8.dp,bottom=12.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text(f.displayName,Modifier.weight(1f),fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleMedium)
                                var menu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick={menu=true},modifier=Modifier.size(48.dp).semantics{contentDescription=f.displayName}) { Text("⋮",fontSize=22.sp) }
                                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                                        DropdownMenuItem(text={Text(stringResource(R.string.edit))},onClick={menu=false;model.edit(f)})
                                        DropdownMenuItem(text={Text(stringResource(if(f.active)R.string.deactivate else R.string.activate))},onClick={menu=false;model.toggleActive(f)})
                                        DropdownMenuItem(text={Text(stringResource(R.string.delete))},onClick={menu=false;model.askDelete(f)})
                                    }
                                }
                            }
                            Text(Formatting.number(f.spoolWeight,state.settings.language)+" "+stringResource(R.string.unit_g)+" · "+Formatting.number(f.printPower,state.settings.language)+" "+stringResource(R.string.unit_w),
                                color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text(stringResource(R.string.paid_price)+": "+Formatting.money(f.purchasePrice,state.settings.language),Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                                Text(Formatting.money(f.pricePerKg,state.settings.language)+stringResource(R.string.unit_per_kg),fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodyMedium)
                            }
                            if(!f.active) Text(stringResource(R.string.inactive),Modifier.padding(top=6.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(onClick={model.edit()},icon={Text("+",fontSize=24.sp)},text={Text(stringResource(R.string.add))},
                modifier=Modifier.align(Alignment.BottomEnd).padding(end=4.dp,bottom=16.dp).testTag("add_filament"))
        }
    }
}
@Composable private fun SettingsPage(state:UiState,model:MakerTallyViewModel) {
    val settings=state.settings;val lang=settings.language
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        PageTitle(R.string.settings)
        SectionLabel(R.string.application)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.language),Modifier.weight(1f))
            var languages by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick={languages=true},modifier=Modifier.testTag("language_picker")) { Text(stringResource(if(lang=="es-ES")R.string.spanish else R.string.english)+"  ⌄") }
                DropdownMenu(expanded=languages,onDismissRequest={languages=false}) {
                    listOf("es-ES" to R.string.spanish,"en-US" to R.string.english).forEach{(code,title)->DropdownMenuItem(text={Text(stringResource(title))},onClick={languages=false;model.language(code)})}
                }
            }
        }
        Text(stringResource(R.string.theme),style=MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(selected=settings.theme==mode,onClick={model.theme(mode)},shape=SegmentedButtonDefaults.itemShape(index,3),icon={},
                    modifier=Modifier.testTag("theme_${mode.name.lowercase()}")) {
                    Text(stringResource(when(mode){ThemeMode.System->R.string.theme_system;ThemeMode.Light->R.string.theme_light;ThemeMode.Dark->R.string.theme_dark}),maxLines=1)
                }
            }
        }
        SectionLabel(R.string.calculation)
        NumberInput(state.electricityText,model::electricity,R.string.electricity_price,R.string.input_per_kwh,"electricity",!state.electricityValid)
        StepRow(R.string.heating_power,Formatting.number(settings.heatingPower,lang)+" "+stringResource(R.string.unit_w),"heating_power",settings.heatingPower>BigDecimal.ZERO,{model.step(SettingStep.HeatingPower,false)},{model.step(SettingStep.HeatingPower,true)})
        StepRow(R.string.heating_time,Formatting.number(settings.heatingMinutes,lang)+" "+stringResource(R.string.unit_min),"heating_time",settings.heatingMinutes>BigDecimal.ZERO,{model.step(SettingStep.HeatingMinutes,false)},{model.step(SettingStep.HeatingMinutes,true)})
        StepRow(R.string.machine_rate,Formatting.money(settings.machineRate,lang)+stringResource(R.string.unit_per_h),"machine_rate",settings.machineRate>BigDecimal.ZERO,{model.step(SettingStep.MachineRate,false)},{model.step(SettingStep.MachineRate,true)})
        StepRow(R.string.sale_multiplier,"×"+Formatting.number(settings.saleMultiplier,lang,1),"multiplier",settings.saleMultiplier>BigDecimal.ONE,{model.step(SettingStep.Multiplier,false)},{model.step(SettingStep.Multiplier,true)})
        Text(stringResource(R.string.alpha_version),Modifier.padding(vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun SectionLabel(title:Int) {
    Column(Modifier.padding(top=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
}
@Composable private fun StepRow(label:Int,value:String,tag:String,canDecrease:Boolean,decrease:()->Unit,increase:()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text(stringResource(label),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            val down=stringResource(R.string.decrease)+" "+stringResource(label)
            val up=stringResource(R.string.increase)+" "+stringResource(label)
            FilledTonalIconButton(onClick=decrease,enabled=canDecrease,modifier=Modifier.size(48.dp).testTag("${tag}_minus").semantics{contentDescription=down}){Text("−",fontSize=22.sp)}
            Text(value,Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.titleMedium)
            FilledTonalIconButton(onClick=increase,modifier=Modifier.size(48.dp).testTag("${tag}_plus").semantics{contentDescription=up}){Text("+",fontSize=22.sp)}
        }
    }
}
@Composable private fun EditorDialog(draft:EditorDraft,state:UiState,model:MakerTallyViewModel) {
    Dialog(onDismissRequest=model::closeEditor,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId=true },color=MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding().fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(if(draft.original==null)R.string.add_filament else R.string.edit_filament),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                    TextButton(onClick=model::closeEditor,enabled=!state.saving){Text(stringResource(R.string.cancel))}
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(draft.material,{model.editor(draft.copy(material=it))},label={Text(stringResource(R.string.material_type))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("filament_material"))
                    OutlinedTextField(draft.brand,{model.editor(draft.copy(brand=it))},label={Text(stringResource(R.string.brand))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("filament_brand"))
                    OutlinedTextField(draft.variant,{model.editor(draft.copy(variant=it))},label={Text(stringResource(R.string.variant))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("filament_variant"))
                    NumberInput(draft.weight,{model.editor(draft.copy(weight=it))},R.string.spool_weight,R.string.unit_g,"filament_weight",NumericInput.nonNegative(draft.weight,positive=true)==null)
                    NumberInput(draft.price,{model.editor(draft.copy(price=it))},R.string.purchase_price,R.string.unit_euro,"filament_price",draft.price.isNotEmpty()&&NumericInput.nonNegative(draft.price)==null)
                    NumberInput(draft.power,{model.editor(draft.copy(power=it))},R.string.printing_power,R.string.unit_w,"filament_power",NumericInput.nonNegative(draft.power)==null)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(stringResource(R.string.active),Modifier.weight(1f));Switch(draft.active,{model.editor(draft.copy(active=it))})
                    }
                    CostRow(R.string.price_per_kg,Formatting.money(draft.build()?.pricePerKg,state.settings.language)+stringResource(R.string.unit_per_kg))
                    Spacer(Modifier.height(12.dp))
                }
                Button(onClick=model::saveEditor,enabled=draft.build()!=null&&!state.saving,modifier=Modifier.fillMaxWidth().padding(16.dp).heightIn(min=48.dp).testTag("save_filament")){Text(stringResource(R.string.save))}
            }
        }
    }
}


