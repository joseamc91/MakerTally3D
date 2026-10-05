package com.joseamc91.makertally

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joseamc91.makertally.ui.*

class MainActivity : AppCompatActivity() {
    private val model: MakerTallyViewModel by viewModels {
        viewModelFactory { initializer { MakerTallyViewModel((application as MakerTallyApplication).repository, createSavedStateHandle(), AppCompatDelegate.getApplicationLocales().get(0)?.language?.let { when (it) { "es" -> "es-ES"; "en" -> "en-US"; else -> null } }) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw the Compose chrome behind transparent system bars on every supported API.
        // Keep the platform's contrast protection for three-button navigation.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ) { true }
        )
        setContent { MakerTallyApp(model) { language ->
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        } }
    }
}

