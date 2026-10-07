package com.joseamc91.makertally.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joseamc91.makertally.R
import com.joseamc91.makertally.BuildConfig
import com.joseamc91.makertally.data.StorageNotice
import com.joseamc91.makertally.domain.*
import java.math.BigDecimal
import kotlin.math.roundToInt

private val lightColors = lightColorScheme(primary=Color(0xFF205DBC), onPrimary=Color.White,
    primaryContainer=Color(0xFFDCE8FF),onPrimaryContainer=Color(0xFF15315C),
    secondaryContainer=Color(0xFFDCE8FF),onSecondaryContainer=Color(0xFF15315C),
    background=Color(0xFFF4F6FA),surface=Color(0xFFFDFDFE),surfaceContainer=Color(0xFFE9EDF5))
private val darkColors = darkColorScheme(primary=Color(0xFFA8C7FF),onPrimary=Color(0xFF123260),
    primaryContainer=Color(0xFF254777),onPrimaryContainer=Color(0xFFDCE8FF),
    secondaryContainer=Color(0xFF254777),onSecondaryContainer=Color(0xFFDCE8FF),
    background=Color(0xFF11151D),surface=Color(0xFF191D26),surfaceContainer=Color(0xFF232A36))

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MakerTallyApp(model: MakerTallyViewModel, applyLocale: (String) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled=helpOpen || privacyOpen) { helpOpen=false;privacyOpen=false }
    val dark = when(state.settings.theme) { ThemeMode.System -> isSystemInDarkTheme();ThemeMode.Light -> false;ThemeMode.Dark -> true }
    val density=LocalDensity.current
    val direction=LocalLayoutDirection.current
    val sideNavigation=WindowInsets.navigationBars.getLeft(density,direction)>0 || WindowInsets.navigationBars.getRight(density,direction)>0
    val view=LocalView.current
    SideEffect { (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it,view).apply {
        isAppearanceLightStatusBars=landscape && !dark;isAppearanceLightNavigationBars=landscape && !dark && !sideNavigation
    } } }
    LaunchedEffect(state.ready,state.settings.language) { if(state.ready)applyLocale(state.settings.language) }
    MaterialTheme(colorScheme=if(dark)darkColors else lightColors) {
        Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId=true }) {
            if (!state.ready) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            else {
                if(landscape) LandscapeShell(state,model,helpOpen,privacyOpen,onHelp={helpOpen=true},onPrivacy={privacyOpen=true},onBack={helpOpen=false;privacyOpen=false},
                    onNavigate={helpOpen=false;privacyOpen=false;model.navigate(it)})
                else Scaffold(
                containerColor=MaterialTheme.colorScheme.background,
                topBar={
                    TopAppBar(
                        title={
                            if(helpOpen || privacyOpen) Text(stringResource(if(privacyOpen)R.string.privacy_policy else R.string.calculator_help),maxLines=1,overflow=TextOverflow.Ellipsis,
                                style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                            else if(state.destination==Destination.Calculator) Row(
                                verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(10.dp)
                            ) {
                                Image(painterResource(R.drawable.ic_makertally_brand_mark),contentDescription=null,
                                    modifier=Modifier.size(32.dp))
                                Text(stringResource(R.string.app_name),maxLines=1,overflow=TextOverflow.Ellipsis,
                                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                            } else Text(stringResource(state.destination.title()),maxLines=1,overflow=TextOverflow.Ellipsis,
                                style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                        },
                        navigationIcon={
                            if(helpOpen || privacyOpen) IconButton(onClick={helpOpen=false;privacyOpen=false},modifier=Modifier.testTag(if(privacyOpen)"privacy_back" else "help_back")) {
                                Icon(painterResource(R.drawable.ic_arrow_back),contentDescription=stringResource(R.string.navigate_back),tint=MakerTallyWhite)
                            }
                        },
                        actions={
                            if(!helpOpen && !privacyOpen && state.destination==Destination.Settings) Text(
                                "v" + BuildConfig.VERSION_NAME,
                                modifier=Modifier.padding(horizontal=12.dp),
                                style=MaterialTheme.typography.labelSmall,
                                color=MakerTallyWhite.copy(alpha=.65f),
                                maxLines=1
                            )
                        },
                        colors=TopAppBarDefaults.topAppBarColors(containerColor=MakerTallyNavy,
                            scrolledContainerColor=MakerTallyNavy,titleContentColor=MakerTallyWhite,
                            actionIconContentColor=MakerTallyWhite)
                    )
                },
                bottomBar={ NavigationBar(containerColor=MakerTallyNavy) {
                    Destination.entries.forEach { destination ->
                        val title=stringResource(destination.title())
                        NavigationBarItem(selected=state.destination==destination,onClick={helpOpen=false;privacyOpen=false;model.navigate(destination)},
                            icon={ NavigationIcon(destination) },label={Text(title,maxLines=1)},
                            colors=NavigationBarItemDefaults.colors(
                                selectedIconColor=MakerTallyGreen,selectedTextColor=MakerTallyWhite,
                                unselectedIconColor=MakerTallyWhite.copy(alpha=.75f),
                                unselectedTextColor=MakerTallyWhite.copy(alpha=.75f),
                                indicatorColor=MakerTallyWhite.copy(alpha=.12f).compositeOver(MakerTallyNavy)),
                            modifier=Modifier.testTag("nav_${destination.name.lowercase()}"))
                    }
                } }
            ) { insets ->
                Box(Modifier.padding(insets).consumeWindowInsets(insets).fillMaxSize()) {
                    PageContent(state,model,helpOpen,privacyOpen,onHelp={helpOpen=true},onPrivacy={privacyOpen=true})
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
@Composable private fun PageContent(state:UiState,model:MakerTallyViewModel,helpOpen:Boolean,privacyOpen:Boolean,onHelp:()->Unit,onPrivacy:()->Unit) {
    if(privacyOpen) PrivacyPolicyPage() else if(helpOpen) CalculatorHelpPage() else when(state.destination) {
        Destination.Calculator -> CalculatorPage(state,model)
        Destination.Filaments -> FilamentsPage(state,model)
        Destination.Settings -> SettingsPage(state,model,onHelp=onHelp,onPrivacy=onPrivacy)
    }
}
@Composable private fun LandscapeShell(state:UiState,model:MakerTallyViewModel,helpOpen:Boolean,privacyOpen:Boolean,onHelp:()->Unit,onPrivacy:()->Unit,onBack:()->Unit,onNavigate:(Destination)->Unit) {
    val density=LocalDensity.current
    val direction=LocalLayoutDirection.current
    val navigationLeft=WindowInsets.navigationBars.getLeft(density,direction)
    val navigationRight=WindowInsets.navigationBars.getRight(density,direction)
    // Follow the actual system navigation side; gesture navigation has no side inset.
    val railRight=navigationRight>=navigationLeft
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top+WindowInsetsSides.Bottom)).imePadding()) {
        if(!railRight) LandscapeRail(state,onNavigate,WindowInsetsSides.Start)
        Column(Modifier.weight(1f).fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(if(railRight)WindowInsetsSides.Start else WindowInsetsSides.End))
            .testTag("landscape_content")) {
            if(helpOpen || privacyOpen) Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(end=16.dp).testTag(if(privacyOpen)"landscape_privacy_header" else "landscape_help_header"),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack,modifier=Modifier.size(48.dp).testTag(if(privacyOpen)"privacy_back" else "help_back")) {
                    Icon(painterResource(R.drawable.ic_arrow_back),contentDescription=stringResource(R.string.navigate_back),tint=MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(if(privacyOpen)R.string.privacy_policy else R.string.calculator_help),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { PageContent(state,model,helpOpen,privacyOpen,onHelp,onPrivacy) }
        }
        if(railRight) LandscapeRail(state,onNavigate,WindowInsetsSides.End)
    }
}
@Composable private fun LandscapeRail(state:UiState,onNavigate:(Destination)->Unit,systemSide:WindowInsetsSides) {
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    BoxWithConstraints(Modifier.fillMaxHeight().background(MakerTallyNavy).windowInsetsPadding(WindowInsets.safeDrawing.only(systemSide))) {
        val showBrand=maxHeight>=240.dp
        NavigationRail(modifier=Modifier.width(112.dp).fillMaxHeight().testTag("landscape_rail"),
            containerColor=MakerTallyNavy,contentColor=MakerTallyWhite,windowInsets=WindowInsets(0,0,0,0),
            header=if(showBrand){{Image(painterResource(R.drawable.ic_makertally_brand_mark),contentDescription=stringResource(R.string.app_name),
                modifier=Modifier.padding(vertical=12.dp).size(40.dp))}}else null) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.SpaceEvenly) {
                Destination.entries.forEach { destination ->
                    NavigationRailItem(selected=state.destination==destination,onClick={focus.clearFocus();keyboard?.hide();onNavigate(destination)},
                        icon={NavigationIcon(destination)},label={Text(stringResource(destination.title()),maxLines=1,overflow=TextOverflow.Ellipsis)},
                        colors=NavigationRailItemDefaults.colors(selectedIconColor=MakerTallyGreen,selectedTextColor=MakerTallyWhite,
                            unselectedIconColor=MakerTallyWhite.copy(alpha=.75f),unselectedTextColor=MakerTallyWhite.copy(alpha=.75f),
                            indicatorColor=MakerTallyWhite.copy(alpha=.12f).compositeOver(MakerTallyNavy)),
                        modifier=Modifier.fillMaxWidth().testTag("nav_${destination.name.lowercase()}"))
                }
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
@Composable private fun SoftCard(modifier:Modifier=Modifier,compact:Boolean=false,content:@Composable ColumnScope.()->Unit) {
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(if(compact)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(compact)8.dp else 10.dp),content=content)
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
@Composable private fun NumberInput(value:String,onChange:(String)->Unit,label:Int,unit:Int?,tag:String,error:Boolean=false,errorLabel:Int=R.string.valid_number,integer:Boolean=false,modifier:Modifier=Modifier,nextFocus:FocusRequester?=null) {
    val focusManager=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(TextFieldValue(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if(editing.text!=value) editing=TextFieldValue(value,TextRange(value.length))
    }
    LaunchedEffect(focused) {
        if(focused) {
            withFrameNanos { }
            editing=editing.copy(selection=TextRange(0,editing.text.length))
        }
    }
    OutlinedTextField(value=editing,onValueChange={
        val changed=editing.text!=it.text
        editing=it
        if(changed)onChange(it.text)
    },label={Text(stringResource(label))},singleLine=true,
        suffix=unit?.let{{Text(stringResource(it))}},isError=error,
        supportingText=if(error){{Text(stringResource(errorLabel))}}else null,
        keyboardOptions=KeyboardOptions(keyboardType=if(integer)KeyboardType.Number else KeyboardType.Decimal,
            imeAction=if(nextFocus!=null)ImeAction.Next else ImeAction.Done),
        keyboardActions=KeyboardActions(onNext={nextFocus?.requestFocus()},onDone={focusManager.clearFocus();keyboard?.hide()}),
        shape=RoundedCornerShape(12.dp),modifier=modifier.fillMaxWidth().testTag(tag).onFocusChanged{focused=it.isFocused})
}
@Composable private fun SuggestedPriceCard(price:String,modifier:Modifier=Modifier,roomy:Boolean=false) {
    val label=stringResource(R.string.suggested_price)
    val labelStyle=MaterialTheme.typography.titleSmall
    val priceStyle=MaterialTheme.typography.displayLarge.copy(fontWeight=FontWeight.Bold)
    val textMeasurer=rememberTextMeasurer()
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp)) {
            val available=constraints.maxWidth
            val labelWidth=textMeasurer.measure(label,style=labelStyle,softWrap=false).size.width
            val gap=with(LocalDensity.current) { 12.dp.roundToPx() }
            fun priceWidth(size:Int)=textMeasurer.measure(price,style=priceStyle.copy(fontSize=size.sp,lineHeight=(size+8).sp),softWrap=false).size.width
            val priceSize=if(labelWidth+gap+priceWidth(48)<=available)48 else 44
            val horizontal=labelWidth+gap+priceWidth(priceSize)<=available
            val ink=MaterialTheme.colorScheme.onPrimaryContainer
            // Keep the regular card horizontal; large accessibility text can use the full width.
            if(horizontal) Row(Modifier.fillMaxWidth().heightIn(min=if(roomy)92.dp else 60.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(label,Modifier.weight(1f),style=labelStyle,color=ink,maxLines=2)
                Text(price,style=priceStyle.copy(fontSize=priceSize.sp,lineHeight=(priceSize+8).sp),
                    color=ink,textAlign=TextAlign.End,maxLines=1,modifier=Modifier.testTag("suggested_price"))
            } else Column(Modifier.fillMaxWidth().heightIn(min=if(roomy)92.dp else 0.dp),verticalArrangement=Arrangement.spacedBy(4.dp,Alignment.CenterVertically)) {
                Text(label,style=labelStyle,color=ink,maxLines=2)
                Text(price,style=priceStyle.copy(fontSize=priceSize.sp,lineHeight=(priceSize+8).sp),
                    color=ink,textAlign=TextAlign.End,maxLines=1,modifier=Modifier.fillMaxWidth().testTag("suggested_price"))
            }
        }
    }
}
@Composable private fun CalculatorPage(state:UiState,model:MakerTallyViewModel) {
    val result=state.result;val lang=state.settings.language
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    val scroll=rememberScrollState()
    val hoursFocus=remember { FocusRequester() };val minutesFocus=remember { FocusRequester() }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var detailsTop by remember { mutableIntStateOf(0) }
    var detailsHeight by remember { mutableIntStateOf(0) }
    var toggleHeight by remember { mutableIntStateOf(0) }
    var revealDetails by remember { mutableStateOf(false) }
    LaunchedEffect(state.detailsOpen,detailsHeight,viewportHeight) {
        if(revealDetails && state.detailsOpen && detailsHeight>0 && viewportHeight>0) {
            // Wait for the expanded layout before using the new scroll range.
            withFrameNanos { }
            if(detailsTop+toggleHeight+detailsHeight>scroll.value+viewportHeight)
                scroll.animateScrollTo(detailsTop.coerceIn(0,scroll.maxValue))
            revealDetails=false
        }
    }
    fun money(value:BigDecimal?,places:Int=2)=Formatting.money(value,lang,places)
    Column(Modifier.fillMaxSize().onSizeChanged{viewportHeight=it.height}.verticalScroll(scroll).padding(if(landscape)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(landscape)8.dp else 14.dp)) {
        if(!landscape) SuggestedPriceCard(money(result?.suggestedSalePrice))
        SoftCard(compact=landscape) {
            Text(stringResource(R.string.new_print),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            val weightInput:@Composable (Modifier)->Unit={modifier ->
                NumberInput(state.input.weight,{model.input(weight=it)},R.string.piece_weight,R.string.unit_g,"piece_weight",NumericInput.nonNegative(state.input.weight)==null,modifier=modifier,nextFocus=hoursFocus)
            }
            if(landscape) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Bottom) {
                Column(Modifier.weight(1.2f)) { FilamentPicker(state,model,compact=true) }
                weightInput(Modifier.weight(1f))
            } else {
                FilamentPicker(state,model)
                weightInput(Modifier)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                NumberInput(state.input.hours,{model.input(hours=it)},R.string.hours,R.string.unit_h,"hours",NumericInput.nonNegative(state.input.hours,integer=true)==null,R.string.whole_hours,true,Modifier.weight(1f).focusRequester(hoursFocus),nextFocus=minutesFocus)
                NumberInput(state.input.minutes,{model.input(minutes=it)},R.string.minutes,R.string.unit_min,"minutes",NumericInput.nonNegative(state.input.minutes,integer=true,maximum=59)==null,R.string.minutes_range,true,Modifier.weight(1f).focusRequester(minutesFocus))
            }
            if(result==null) Text(stringResource(R.string.invalid_result),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(landscape) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            SuggestedPriceCard(money(result?.suggestedSalePrice),Modifier.weight(1.1f),roomy=true)
            PrintCosts(result,lang,Modifier.weight(1f),compact=true)
        } else PrintCosts(result,lang)
        val detailsState=stringResource(if(state.detailsOpen)R.string.expanded else R.string.collapsed)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).onGloballyPositioned{detailsTop=it.positionInParent().y.roundToInt();toggleHeight=it.size.height}
            .clickable { revealDetails=!state.detailsOpen;if(state.detailsOpen)detailsHeight=0;model.toggleDetails() }
            .semantics { stateDescription=detailsState }.testTag("details_toggle").padding(horizontal=4.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.calculation_details),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
            Text(if(state.detailsOpen)"⌄"else"›",fontSize=24.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(state.detailsOpen) SoftCard(Modifier.onSizeChanged{detailsHeight=it.height}.testTag("calculation_detail_values")) {
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
        if(!landscape) Spacer(Modifier.height(4.dp))
    }
}
@Composable private fun FilamentPicker(state:UiState,model:MakerTallyViewModel,compact:Boolean=false) {
    Column(verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 10.dp)) {
    var choosing by remember { mutableStateOf(false) }
    Text(stringResource(R.string.filament),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Box {
        OutlinedButton(onClick={choosing=true},Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("filament_picker"),shape=RoundedCornerShape(12.dp)) {
            Text(state.selected?.displayName ?: stringResource(R.string.no_active_filaments),Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
            Icon(painterResource(R.drawable.ic_arrow_drop_down),contentDescription=null,modifier=Modifier.size(24.dp))
        }
        DropdownMenu(expanded=choosing,onDismissRequest={choosing=false}) {
            state.filaments.filter{it.active}.forEach { f-> DropdownMenuItem(text={Text(f.displayName)},onClick={model.select(f.id);choosing=false}) }
        }
    }
    }
}
@Composable private fun PrintCosts(result:CalculationResult?,lang:String,modifier:Modifier=Modifier,compact:Boolean=false) {
    fun money(value:BigDecimal?,places:Int=2)=Formatting.money(value,lang,places)
    Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(if(compact)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 10.dp)) {
            CostRow(R.string.piece_cost,money(result?.pieceCost),important=true)
            CostRow(R.string.material_cost,money(result?.materialCost))
            CostRow(R.string.electricity_cost,money(result?.electricityCost))
            CostRow(R.string.machine_additional,money(result?.machineCost),secondary=true)
        }
    }
}
@Composable private fun FilamentSortAction(state:UiState,model:MakerTallyViewModel) {
    var sorting by remember { mutableStateOf(false) }
    Box {
        SmallFloatingActionButton(onClick={sorting=true},modifier=Modifier.size(48.dp).testTag("filament_sort"),
            containerColor=MaterialTheme.colorScheme.surfaceContainerHigh,contentColor=MaterialTheme.colorScheme.onSurfaceVariant) {
            Icon(painterResource(R.drawable.ic_sort),contentDescription=stringResource(R.string.sort_filaments))
        }
        DropdownMenu(expanded=sorting,onDismissRequest={sorting=false}) {
            FilamentSort.entries.forEach { mode ->
                val selected=state.settings.filamentSort==mode
                DropdownMenuItem(text={Text(stringResource(when(mode) {
                    FilamentSort.Name->R.string.sort_name
                    FilamentSort.PriceAscending->R.string.sort_price_ascending
                    FilamentSort.PriceDescending->R.string.sort_price_descending
                }))},leadingIcon={Text(if(selected)"✓"else"",Modifier.clearAndSetSemantics { })},
                    modifier=Modifier.testTag("sort_${mode.name.lowercase()}").semantics{this.selected=selected},
                    onClick={sorting=false;model.sort(mode)})
            }
        }
    }
}
@Composable private fun FilamentsPage(state:UiState,model:MakerTallyViewModel) {
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    val listState=rememberLazyListState()
    val gridState=rememberLazyGridState()
    LaunchedEffect(state.settings.filamentSort,landscape) { if(landscape)gridState.scrollToItem(0)else listState.scrollToItem(0) }
    val profiles=remember(state.filaments,state.settings.filamentSort,state.settings.language) {
        orderedFilaments(state.filaments,state.settings.filamentSort,state.settings.language)
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Box(Modifier.weight(1f)) {
            if(landscape) LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.fillMaxSize().testTag("filament_grid"),state=gridState,
                contentPadding=PaddingValues(bottom=96.dp),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(state.filaments.isEmpty()) item { Text(stringResource(R.string.no_filaments)) }
                gridItems(profiles,key={it.id}) { f->FilamentCard(f,state,model) }
            } else LazyColumn(Modifier.fillMaxSize(),state=listState,contentPadding=PaddingValues(bottom=96.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                if(state.filaments.isEmpty()) item { Text(stringResource(R.string.no_filaments)) }
                items(profiles,key={it.id}) { f->FilamentCard(f,state,model) }
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(end=4.dp,bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                FilamentSortAction(state,model)
                AddFilamentAction(model)
            }
        }
    }
}
@Composable private fun AddFilamentAction(model:MakerTallyViewModel,modifier:Modifier=Modifier) {
    ExtendedFloatingActionButton(onClick={model.edit()},icon={Text("+",fontSize=24.sp)},text={Text(stringResource(R.string.add))},
        modifier=modifier.testTag("add_filament"))
}
@Composable private fun FilamentCard(f:Filament,state:UiState,model:MakerTallyViewModel) {
    Card(Modifier.fillMaxWidth().testTag("filament_${f.id}"),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(
        containerColor=if(f.active)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        contentColor=if(f.active)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)) {
        Column(Modifier.padding(start=14.dp,end=10.dp,top=8.dp,bottom=12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(f.displayName,Modifier.weight(1f),fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleMedium)
                var menu by remember { mutableStateOf(false) }
                Box {
                    val actions=stringResource(R.string.filament_actions)+": "+f.displayName
                    IconButton(onClick={menu=true},modifier=Modifier.size(48.dp).testTag("filament_menu_${f.id}").semantics{contentDescription=actions}) { Text("⋮",fontSize=22.sp) }
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text(stringResource(R.string.edit))},onClick={menu=false;model.edit(f)})
                        DropdownMenuItem(text={Text(stringResource(if(f.active)R.string.deactivate else R.string.activate))},onClick={menu=false;model.toggleActive(f)})
                        DropdownMenuItem(text={Text(stringResource(R.string.delete))},onClick={menu=false;model.askDelete(f)})
                    }
                }
            }
            Text(if(f.active)Formatting.number(f.spoolWeight,state.settings.language)+" "+stringResource(R.string.unit_g)+" · "+Formatting.number(f.printPower,state.settings.language)+" "+stringResource(R.string.unit_w)
                else stringResource(R.string.inactive),
                color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall,
                fontWeight=if(f.active)FontWeight.Normal else FontWeight.Medium)
            Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(R.string.paid_price)+": "+Formatting.money(f.purchasePrice,state.settings.language),Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                Text(Formatting.money(f.pricePerKg,state.settings.language)+stringResource(R.string.unit_per_kg),fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
@Composable private fun SettingsPage(state:UiState,model:MakerTallyViewModel,onHelp:()->Unit,onPrivacy:()->Unit) {
    val settings=state.settings;val lang=settings.language
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    val taxFocus=remember { FocusRequester() }
    val languageControls:@Composable ()->Unit={
        Text(stringResource(R.string.language),style=MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("es-ES" to R.string.spanish,"en-US" to R.string.english).forEachIndexed { index,(code,title) ->
                SegmentedButton(selected=lang==code,onClick={model.language(code)},shape=SegmentedButtonDefaults.itemShape(index,2),icon={},
                    modifier=Modifier.testTag("language_$code")) { Text(stringResource(title)) }
            }
        }
    }
    val themeControls:@Composable ()->Unit={
        Text(stringResource(R.string.theme),style=MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(selected=settings.theme==mode,onClick={model.theme(mode)},shape=SegmentedButtonDefaults.itemShape(index,3),icon={},
                    modifier=Modifier.testTag("theme_${mode.name.lowercase()}")) {
                    Text(stringResource(when(mode){ThemeMode.System->R.string.theme_system;ThemeMode.Light->R.string.theme_light;ThemeMode.Dark->R.string.theme_dark}),maxLines=1)
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if(landscape)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(landscape)8.dp else 12.dp)) {
        if(landscape) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(.4f),verticalArrangement=Arrangement.spacedBy(4.dp)) { languageControls() }
            Column(Modifier.weight(.6f),verticalArrangement=Arrangement.spacedBy(4.dp)) { themeControls() }
        } else { languageControls();themeControls() }
        SectionLabel(R.string.calculation,compact=landscape) {
            OutlinedButton(onClick=onHelp,modifier=Modifier.heightIn(min=40.dp).testTag("calculator_help"),
                contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.primary)) {
                Text(stringResource(R.string.help),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Medium)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            NumberInput(state.electricityText,model::electricity,R.string.electricity_price,R.string.input_per_kwh,"electricity",!state.electricityValid,
                modifier=Modifier.weight(1.7f),nextFocus=taxFocus)
            NumberInput(state.electricityTaxText,model::electricityTax,R.string.electricity_taxes,R.string.unit_percent,"electricity_tax",!state.electricityTaxValid,
                modifier=Modifier.weight(1f).focusRequester(taxFocus))
        }
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                StepRow(R.string.heating_power,Formatting.number(settings.heatingPower,lang)+" "+stringResource(R.string.unit_w),"heating_power",settings.heatingPower>BigDecimal.ZERO,{model.step(SettingStep.HeatingPower,false)},{model.step(SettingStep.HeatingPower,true)},Modifier.weight(1f).fillMaxHeight(),compact=true)
                StepRow(R.string.heating_time,Formatting.number(settings.heatingMinutes,lang)+" "+stringResource(R.string.unit_min),"heating_time",settings.heatingMinutes>BigDecimal.ZERO,{model.step(SettingStep.HeatingMinutes,false)},{model.step(SettingStep.HeatingMinutes,true)},Modifier.weight(1f).fillMaxHeight(),compact=true)
            }
            if(landscape) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                StepRow(R.string.machine_rate,Formatting.money(settings.machineRate,lang)+stringResource(R.string.unit_per_h),"machine_rate",settings.machineRate>BigDecimal.ZERO,{model.step(SettingStep.MachineRate,false)},{model.step(SettingStep.MachineRate,true)},Modifier.weight(1f).fillMaxHeight(),compact=true)
                StepRow(R.string.sale_multiplier,"×"+Formatting.number(settings.saleMultiplier,lang,1),"multiplier",settings.saleMultiplier>BigDecimal.ONE,{model.step(SettingStep.Multiplier,false)},{model.step(SettingStep.Multiplier,true)},Modifier.weight(1f).fillMaxHeight(),compact=true)
            } else {
                StepRow(R.string.machine_rate,Formatting.money(settings.machineRate,lang)+stringResource(R.string.unit_per_h),"machine_rate",settings.machineRate>BigDecimal.ZERO,{model.step(SettingStep.MachineRate,false)},{model.step(SettingStep.MachineRate,true)})
                StepRow(R.string.sale_multiplier,"×"+Formatting.number(settings.saleMultiplier,lang,1),"multiplier",settings.saleMultiplier>BigDecimal.ONE,{model.step(SettingStep.Multiplier,false)},{model.step(SettingStep.Multiplier,true)})
            }
        }
        TextButton(onClick=onPrivacy,modifier=Modifier.heightIn(min=48.dp).testTag("privacy_policy")) {
            Text(stringResource(R.string.privacy_policy))
        }
    }
}
@Composable private fun SectionLabel(title:Int,compact:Boolean=false,action:(@Composable ()->Unit)?=null) {
    Column(Modifier.padding(top=if(compact)0.dp else 4.dp),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(title),Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
            action?.invoke()
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
}
@Composable private fun StepRow(label:Int,value:String,tag:String,canDecrease:Boolean,decrease:()->Unit,increase:()->Unit,modifier:Modifier=Modifier,compact:Boolean=false) {
    Column(modifier.fillMaxWidth(),verticalArrangement=if(compact)Arrangement.SpaceBetween else Arrangement.spacedBy(2.dp)) {
        Text(stringResource(label),modifier=if(compact)Modifier.padding(bottom=2.dp)else Modifier,
            style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            val down=stringResource(R.string.decrease)+" "+stringResource(label)
            val up=stringResource(R.string.increase)+" "+stringResource(label)
            StepButton("−",decrease,canDecrease,"${tag}_minus",down)
            Text(value,Modifier.weight(1f),textAlign=TextAlign.Center,
                style=if(compact)MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
            StepButton("+",increase,true,"${tag}_plus",up)
        }
    }
}
@Composable private fun StepButton(symbol:String,onClick:()->Unit,enabled:Boolean,tag:String,description:String) {
    val tones=IconButtonDefaults.filledTonalIconButtonColors()
    IconButton(onClick=onClick,enabled=enabled,modifier=Modifier.size(48.dp).testTag(tag).semantics{contentDescription=description},
        colors=IconButtonDefaults.iconButtonColors(containerColor=Color.Transparent,contentColor=tones.contentColor,
            disabledContainerColor=Color.Transparent,disabledContentColor=tones.disabledContentColor)) {
        Box(Modifier.size(40.dp).background(if(enabled)tones.containerColor else tones.disabledContainerColor,CircleShape),contentAlignment=Alignment.Center) {
            Text(symbol,fontSize=22.sp)
        }
    }
}
@Composable private fun EditorDialog(draft:EditorDraft,state:UiState,model:MakerTallyViewModel) {
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    val brandFocus=remember { FocusRequester() };val variantFocus=remember { FocusRequester() }
    val weightFocus=remember { FocusRequester() };val priceFocus=remember { FocusRequester() };val powerFocus=remember { FocusRequester() }
    val materialField:@Composable (Modifier)->Unit={modifier ->
        OutlinedTextField(draft.material,{model.editor(draft.copy(material=it))},label={Text(stringResource(R.string.material_type))},singleLine=true,
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next),keyboardActions=KeyboardActions(onNext={brandFocus.requestFocus()}),modifier=modifier.fillMaxWidth().testTag("filament_material"))
    }
    val brandField:@Composable (Modifier)->Unit={modifier ->
        OutlinedTextField(draft.brand,{model.editor(draft.copy(brand=it))},label={Text(stringResource(R.string.brand))},singleLine=true,
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next),keyboardActions=KeyboardActions(onNext={variantFocus.requestFocus()}),modifier=modifier.fillMaxWidth().focusRequester(brandFocus).testTag("filament_brand"))
    }
    val variantField:@Composable (Modifier)->Unit={modifier ->
        OutlinedTextField(draft.variant,{model.editor(draft.copy(variant=it))},label={Text(stringResource(R.string.variant))},singleLine=true,
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next),keyboardActions=KeyboardActions(onNext={weightFocus.requestFocus()}),modifier=modifier.fillMaxWidth().focusRequester(variantFocus).testTag("filament_variant"))
    }
    val weightField:@Composable (Modifier)->Unit={modifier ->
        NumberInput(draft.weight,{model.editor(draft.copy(weight=it))},R.string.spool_weight,R.string.unit_g,"filament_weight",NumericInput.nonNegative(draft.weight,positive=true)==null,errorLabel=R.string.positive_number,modifier=modifier.focusRequester(weightFocus),nextFocus=priceFocus)
    }
    val priceField:@Composable (Modifier)->Unit={modifier ->
        NumberInput(draft.price,{model.editor(draft.copy(price=it))},R.string.purchase_price,R.string.unit_euro,"filament_price",draft.price.isNotEmpty()&&NumericInput.nonNegative(draft.price)==null,modifier=modifier.focusRequester(priceFocus),nextFocus=powerFocus)
    }
    val powerField:@Composable (Modifier)->Unit={modifier ->
        NumberInput(draft.power,{model.editor(draft.copy(power=it))},R.string.printing_power,R.string.unit_w,"filament_power",NumericInput.nonNegative(draft.power)==null,modifier=modifier.focusRequester(powerFocus))
    }
    val activeField:@Composable (Modifier)->Unit={modifier ->
        Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(R.string.active),Modifier.weight(1f));Switch(draft.active,{model.editor(draft.copy(active=it))})
        }
    }
    val derivedPrice:@Composable (Modifier)->Unit={modifier ->
        Column(modifier) { CostRow(R.string.price_per_kg,Formatting.money(draft.build()?.pricePerKg,state.settings.language)+stringResource(R.string.unit_per_kg)) }
    }
    // Keep a full field visible when the landscape text keyboard leaves very little height.
    val scrollSave=landscape && WindowInsets.ime.getBottom(LocalDensity.current)>0
    val saveAction:@Composable ()->Unit={
        Button(onClick=model::saveEditor,enabled=draft.build()!=null&&!state.saving,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=if(landscape)8.dp else 16.dp).heightIn(min=48.dp).testTag("save_filament")){Text(stringResource(R.string.save))}
    }
    Dialog(onDismissRequest=model::closeEditor,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId=true },color=MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().imePadding().fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=if(landscape)0.dp else 8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(if(draft.original==null)R.string.add_filament else R.string.edit_filament),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                    TextButton(onClick=model::closeEditor,enabled=!state.saving){Text(stringResource(R.string.cancel))}
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    if(landscape) {
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { materialField(Modifier.weight(1f));brandField(Modifier.weight(1f)) }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { variantField(Modifier.weight(1f));weightField(Modifier.weight(1f)) }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { priceField(Modifier.weight(1f));powerField(Modifier.weight(1f)) }
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) { activeField(Modifier.weight(1f));derivedPrice(Modifier.weight(1f)) }
                    } else {
                        materialField(Modifier);brandField(Modifier);variantField(Modifier)
                        weightField(Modifier);priceField(Modifier);powerField(Modifier)
                        activeField(Modifier);derivedPrice(Modifier)
                    }
                    if(scrollSave) saveAction()
                    Spacer(Modifier.height(12.dp))
                }
                if(!scrollSave) saveAction()
            }
        }
    }
}


