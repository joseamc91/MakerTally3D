package com.joseamc91.makertally.data

import android.content.Context
import android.util.AtomicFile
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import com.joseamc91.makertally.domain.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AndroidPrivateFiles(private val directory: File) : PrivateFiles {
    override fun read(name: String): String? = File(directory, name).let { if (it.exists()) it.readText() else null }
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
    private val files: PrivateFiles
) : AppRepository {
    private val key = stringPreferencesKey("settings_json")
    private val library = FilamentStore(files)
    override suspend fun load(): LoadedApp = withContext(Dispatchers.IO) {
        val notices = mutableListOf<StorageNotice>()
        val settings = try {
            val text = preferences.data.first()[key]
            if (text == null) {
                AppSettings().also { saveSettings(it) }
            } else {
                try { DataCodec.decodeSettings(text) } catch (_: IllegalArgumentException) {
                    notices += StorageNotice.InvalidData
                    AppSettings().also {
                        try { files.writeAtomically("settings.json.invalid", text); saveSettings(it) }
                        catch (_: java.io.IOException) { notices += StorageNotice.SaveFailed }
                    }
                }
            }
        } catch (_: java.io.IOException) { notices += StorageNotice.ReadFailed; AppSettings() }
        val loaded = library.load()
        LoadedApp(settings, loaded.filaments, notices + loaded.notices)
    }
    override suspend fun saveSettings(settings: AppSettings) {
        val text = DataCodec.encodeSettings(settings)
        preferences.edit { it[key] = text }
    }
    override suspend fun saveFilaments(profiles: List<Filament>) = withContext(Dispatchers.IO) { library.save(profiles) }
}

fun androidRepository(context: Context): AppRepository {
    val path = context.preferencesDataStoreFile("settings")
    val store = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler {
            // Preserve invalid protobuf bytes before DataStore repairs the file.
            if (path.exists()) path.copyTo(File(path.path + ".invalid"), overwrite = true)
            emptyPreferences()
        },
        produceFile = { path }
    )
    return PreferencesRepository(store, AndroidPrivateFiles(context.filesDir))
}
