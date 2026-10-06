package com.joseamc91.makertally.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joseamc91.makertally.R

@Composable internal fun CalculatorHelpPage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("calculator_help_content"),
        verticalArrangement=Arrangement.spacedBy(20.dp)) {
        HelpSection(R.string.electricity_price,R.string.help_electricity_price)
        HelpSection(R.string.electricity_taxes,R.string.help_electricity_taxes)
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.help_tax_example_title),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
                Text(stringResource(R.string.help_tax_example),style=MaterialTheme.typography.bodyMedium)
            }
        }
        HelpSection(R.string.heating_power,R.string.help_heating_power)
        HelpSection(R.string.heating_time,R.string.help_heating_time)
        HelpSection(R.string.machine_rate,R.string.help_machine_cost)
        HelpSection(R.string.sale_multiplier,R.string.help_sale_multiplier)
    }
}

@Composable private fun HelpSection(title:Int,body:Int) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
        Text(stringResource(body),style=MaterialTheme.typography.bodyMedium)
    }
}
