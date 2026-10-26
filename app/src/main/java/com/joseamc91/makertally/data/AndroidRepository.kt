package com.joseamc91.makertally.data

import android.content.Context
import android.system.ErrnoException
import android.system.OsConstants
import android.util.AtomicFile
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import com.joseamc91.makertally.domain.*
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AndroidPrivateFiles(private val directory: File) : PrivateFiles {
    override fun read(name: String): String? {
        val base = File(directory, name)
        return try {
            // openRead restores AtomicFile's last committed backup before reading.
            AtomicFile(base).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: FileNotFoundException) {
            // FileNotFoundException also covers EACCES/EISDIR: only ENOENT means absence.
            // A remaining backup means recovery failed, rather than a genuine first run.
            if ((error.cause as? ErrnoException)?.errno == OsConstants.ENOENT &&
                !base.exists() && !File(base.path + ".bak").exists()) null
            else throw error
        }
    }
    override fun writeAtomically(name: String, content: String) {
        if (!directory.exists() && !directory.mkdirs()) throw java.io.IOException("Cannot create private files directory")
        val atomic = AtomicFile(File(directory, name))
        val output = atomic.startWrite()
        try {
            output.write(content.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        }
    }
}

class PreferencesRepository(
    private val preferences: DataStore<Preferences>,
    private val files: PrivateFiles,
    private val recoveredCorruption: () -> Boolean = { false },
    private val priorSettingsFile: Boolean = false,
    private val initialLanguage: String = "en-US"
) : AppRepository {
    private val key = stringPreferencesKey("settings_json")
    private val setupKey = stringPreferencesKey("onboarding_json")
    private val library = FilamentStore(files) { emptyList() }
    override suspend fun load(): LoadedApp = withContext(Dispatchers.IO) {
        val notices = mutableListOf<StorageNotice>()
        val prefs = try { preferences.data.first() } catch (_: java.io.IOException) {
            // Unknown/unreadable existing storage must never be mistaken for a new install.
            return@withContext LoadedApp(AppSettings(),emptyList(),listOf(StorageNotice.ReadFailed))
        }
        val setupText = prefs[setupKey]
        var setup = setupText?.let { text ->
            try { DataCodec.decodeOnboarding(text) } catch (_: IllegalArgumentException) {
                notices += StorageNotice.InvalidData
                try { files.writeAtomically("onboarding.json.invalid",text) }
                catch (_: java.io.IOException) { notices += StorageNotice.SaveFailed }
                null
            }
        }
        var migrated = false
        if(setup == null) {
            val hasLibrary = try { files.read("filaments.json") != null } catch (_: java.io.IOException) {
                return@withContext LoadedApp(AppSettings(),emptyList(),notices + StorageNotice.ReadFailed)
            }
            migrated = priorSettingsFile || prefs[key] != null || hasLibrary
            setup = if(migrated) OnboardingState.legacy() else OnboardingState(language=initialLanguage)
            try { saveOnboarding(setup) } catch (_: java.io.IOException) { notices += StorageNotice.SaveFailed }
        }
        if(!setup.completed) {
            // Do not initialize settings_json or filaments.json while onboarding is unfinished.
            return@withContext LoadedApp(neutralSettings(setup.language),emptyList(),notices,setup)
        }
        val settings = try {
            val text = prefs[key]
            if (text == null) {
                (if(setup.newInstallation) neutralSettings(setup.language) else AppSettings()).also { saveSettings(it) }
            } else {
                try { DataCodec.decodeSettings(text) } catch (_: IllegalArgumentException) {
                    notices += StorageNotice.InvalidData
                    (if(setup.newInstallation) neutralSettings(setup.language) else AppSettings()).also {
                        try { files.writeAtomically("settings.json.invalid", text); saveSettings(it) }
                        catch (_: java.io.IOException) { notices += StorageNotice.SaveFailed }
                    }
                }
            }
        } catch (_: java.io.IOException) { notices += StorageNotice.ReadFailed; if(setup.newInstallation) neutralSettings(setup.language) else AppSettings() }
        if (recoveredCorruption()) notices += StorageNotice.InvalidData
        val loaded = library.load()
        LoadedApp(settings, loaded.filaments, notices + loaded.notices, setup, migrated)
    }
    override suspend fun saveSettings(settings: AppSettings) {
        val text = DataCodec.encodeSettings(settings)
        preferences.edit { it[key] = text }
    }
    override suspend fun saveFilaments(profiles: List<Filament>) = withContext(Dispatchers.IO) { library.save(profiles) }
    override suspend fun saveOnboarding(state: OnboardingState) {
        preferences.edit { it[setupKey] = DataCodec.encodeOnboarding(state) }
    }
    override suspend fun completeOnboarding(state: OnboardingState): LoadedApp = withContext(Dispatchers.IO) {
        require(state.newInstallation && !state.completed && state.step == SetupStep.Complete && state.canAdvance)
        val settings = state.settings()
        // The persistent ID makes retries safe if the process stops between file and DataStore writes.
        // Existing data, even an empty library, is never overwritten or appended to during a retry.
        val text = files.read("filaments.json")
        val profiles = if(text == null) listOf(state.genericFilament()).also { library.save(it) }
            else DataCodec.decodeFilaments(text)
        val complete = state.copy(completed=true)
        preferences.edit { it[key]=DataCodec.encodeSettings(settings);it[setupKey]=DataCodec.encodeOnboarding(complete) }
        LoadedApp(settings,profiles,onboarding=complete)
    }
}

fun androidRepository(context: Context): AppRepository {
    val path = context.preferencesDataStoreFile("settings")
    // Inspect before DataStore initialization/corruption repair can create or replace the file.
    val priorSettingsFile = path.exists() || File(path.path + ".bak").exists()
    val systemLanguage = android.content.res.Resources.getSystem().configuration.locales[0]?.language
    val language = if(systemLanguage == "es") "es-ES" else "en-US"
    val recovered = java.util.concurrent.atomic.AtomicBoolean(false)
    val store = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler {
            // Preserve invalid protobuf bytes before DataStore repairs the file.
            if (path.exists()) path.copyTo(File(path.path + ".invalid"), overwrite = true)
            recovered.set(true)
            emptyPreferences()
        },
        produceFile = { path }
    )
    return PreferencesRepository(store, AndroidPrivateFiles(context.filesDir), { recovered.getAndSet(false) },priorSettingsFile,language)
}

