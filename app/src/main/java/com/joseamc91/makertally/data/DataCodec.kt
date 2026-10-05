package com.joseamc91.makertally.data

import com.joseamc91.makertally.domain.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object DataCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    fun encodeSettings(settings: AppSettings): String {
        require(settings.isValid())
        return json.encodeToString(settings)
    }
    fun decodeSettings(text: String): AppSettings = json.decodeFromString<AppSettings>(text).also { require(it.isValid()) }
    fun encodeFilaments(profiles: List<Filament>): String {
        validate(profiles)
        return json.encodeToString(profiles)
    }
    fun decodeFilaments(text: String): List<Filament> = json.decodeFromString<List<Filament>>(text).also { validate(it) }
    private fun validate(profiles: List<Filament>) {
        require(profiles.all { it.isValid() } && profiles.map { it.id }.distinct().size == profiles.size)
    }
}

enum class StorageNotice { InvalidData, ReadFailed, SaveFailed }
data class LoadedLibrary(val filaments: List<Filament>, val notices: List<StorageNotice> = emptyList())
data class LoadedApp(val settings: AppSettings, val filaments: List<Filament>, val notices: List<StorageNotice> = emptyList())
interface PrivateFiles {
    fun read(name: String): String?
    fun writeAtomically(name: String, content: String)
}
class FilamentStore(private val files: PrivateFiles) {
    fun load(): LoadedLibrary {
        val text = try { files.read("filaments.json") } catch (_: java.io.IOException) {
            return LoadedLibrary(Defaults.filaments(), listOf(StorageNotice.ReadFailed))
        }
        if (text != null) {
            try { return LoadedLibrary(DataCodec.decodeFilaments(text)) } catch (_: IllegalArgumentException) {
                return recover(text)
            }
        }
        val initial = Defaults.filaments()
        return try { save(initial); LoadedLibrary(initial) } catch (_: java.io.IOException) {
            LoadedLibrary(initial, listOf(StorageNotice.SaveFailed))
        }
    }
    private fun recover(text: String): LoadedLibrary {
        val notices = mutableListOf(StorageNotice.InvalidData)
        val initial = Defaults.filaments()
        try {
            files.writeAtomically("filaments.json.invalid", text)
            save(initial)
        } catch (_: java.io.IOException) { notices += StorageNotice.SaveFailed }
        return LoadedLibrary(initial, notices)
    }
    fun save(profiles: List<Filament>) = files.writeAtomically("filaments.json", DataCodec.encodeFilaments(profiles))
}
interface AppRepository {
    suspend fun load(): LoadedApp
    suspend fun saveSettings(settings: AppSettings)
    suspend fun saveFilaments(profiles: List<Filament>)
}
