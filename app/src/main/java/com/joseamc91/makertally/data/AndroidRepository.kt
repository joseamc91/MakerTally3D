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
    private val recoveredCorruption: () -> Boolean = { false }
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
        if (recoveredCorruption()) notices += StorageNotice.InvalidData
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
    return PreferencesRepository(store, AndroidPrivateFiles(context.filesDir)) { recovered.getAndSet(false) }
}

