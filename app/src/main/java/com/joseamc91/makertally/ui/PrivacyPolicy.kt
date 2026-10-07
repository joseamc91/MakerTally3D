package com.joseamc91.makertally.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joseamc91.makertally.R

// Replace this public document URL if a definitive privacy website is introduced.
internal const val PRIVACY_POLICY_URL = "https://github.com/joseamc91/MakerTally3D/blob/main/PRIVACY.md"

@Composable internal fun PrivacyPolicyPage() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val resources = LocalResources.current
    val paragraphs = remember(resources,configuration) {
        resources.openRawResource(R.raw.privacy_policy).bufferedReader(Charsets.UTF_8).use { it.readText() }
            .trim().split(Regex("\\r?\\n\\s*\\r?\\n")).filterNot { it.startsWith("# ") }
    }
    var browserUnavailable by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("privacy_content"),
        verticalArrangement=Arrangement.spacedBy(16.dp)) {
        // This small document uses only paragraph text and level-two Markdown headings.
        paragraphs.forEach { paragraph ->
            if(paragraph.startsWith("## ")) Text(paragraph.removePrefix("## "),
                modifier=Modifier.semantics { heading() },style=MaterialTheme.typography.titleSmall,
                fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
            else Text(paragraph.replace(Regex("\\r?\\n"), " "),style=MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick={
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW,PRIVACY_POLICY_URL.toUri()))
            } catch (_: ActivityNotFoundException) { browserUnavailable=true }
        },modifier=Modifier.heightIn(min=48.dp).testTag("privacy_external")) {
            Text(stringResource(R.string.privacy_open_online))
        }
        if(browserUnavailable) Text(stringResource(R.string.privacy_no_browser),style=MaterialTheme.typography.bodyMedium)
    }
}
