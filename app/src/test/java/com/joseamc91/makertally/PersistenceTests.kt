package com.joseamc91.makertally

import com.joseamc91.makertally.data.*
import com.joseamc91.makertally.domain.*
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class MemoryFiles : PrivateFiles {
    val data=mutableMapOf<String,String>()
    var failRead=false; var failWrite=false
    override fun read(name:String):String? { if(failRead)throw IOException("read");return data[name] }
    override fun writeAtomically(name:String,content:String) { if(failWrite)throw IOException("write");data[name]=content }
}
class PersistenceTests {
    @Test fun exactJsonRoundTripRetainsDecimalsIdsVariantsAndInactiveProfiles() {
        val settings=AppSettings(language="en-US",theme=ThemeMode.Dark,electricityPrice=decimal("0.134912345678"),saleMultiplier=decimal("0"))
        assertEquals(settings,DataCodec.decodeSettings(DataCodec.encodeSettings(settings)))
        val fs=Defaults.filaments().map{it.copy(active=false,variant="Matte")}
        assertEquals(fs,DataCodec.decodeFilaments(DataCodec.encodeFilaments(fs)))
        assertFalse(DataCodec.encodeFilaments(fs).contains("pricePerKg"))
    }
    @Test fun firstRunSeedsOnceAndExistingEmptyLibraryStaysEmpty() {
        val files=MemoryFiles();val store=FilamentStore(files);val first=store.load()
        assertEquals(7,first.filaments.size);assertEquals(first.filaments,store.load().filaments)
        store.save(emptyList());assertTrue(store.load().filaments.isEmpty())
    }
    @Test fun corruptLibraryIsBackedUpBeforeDefaultsAndThenStable() {
        val files=MemoryFiles();files.data["filaments.json"]="not json"
        val store=FilamentStore(files);val loaded=store.load()
        assertEquals(listOf(StorageNotice.InvalidData),loaded.notices)
        assertEquals("not json",files.data["filaments.json.invalid"])
        assertEquals(7,loaded.filaments.size);assertEquals(loaded.filaments,store.load().filaments)
    }
    @Test fun semanticErrorsAndDuplicateIdsAreRejected() {
        val f=Defaults.filaments().first();val encoded=DataCodec.encodeFilaments(listOf(f))
        assertThrows(IllegalArgumentException::class.java){DataCodec.decodeFilaments("["+encoded.drop(1).dropLast(1)+","+encoded.drop(1).dropLast(1)+"]")}
        assertThrows(IllegalArgumentException::class.java){DataCodec.encodeFilaments(listOf(f.copy(spoolWeight=decimal("0"))))}
        assertThrows(IllegalArgumentException::class.java){DataCodec.decodeSettings("{\"electricityPrice\":\"-1\"}")}
        assertThrows(IllegalArgumentException::class.java){DataCodec.decodeSettings("{\"language\":\"fr-FR\"}")}
    }
    @Test fun semanticallyInvalidLibraryUsesSafeDefaultsAndRetainsOriginal() {
        val files=MemoryFiles(); val valid=DataCodec.encodeFilaments(listOf(Defaults.filaments().first()))
        val bad=valid.replace("\"spoolWeight\":\"1000\"","\"spoolWeight\":\"0\"")
        assertNotEquals(valid,bad);files.data["filaments.json"]=bad
        val loaded=FilamentStore(files).load();assertEquals(7,loaded.filaments.size)
        assertEquals(listOf(StorageNotice.InvalidData),loaded.notices);assertEquals(bad,files.data["filaments.json.invalid"])
    }
    @Test fun storageFailuresReturnDefaultsWithoutOverwritingOriginal() {
        val files=MemoryFiles();files.failWrite=true
        assertEquals(listOf(StorageNotice.SaveFailed),FilamentStore(files).load().notices)
        files.data["filaments.json"]="bad";val loaded=FilamentStore(files).load()
        assertTrue(loaded.notices.contains(StorageNotice.SaveFailed));assertEquals("bad",files.data["filaments.json"])
        files.failRead=true;assertEquals(listOf(StorageNotice.ReadFailed),FilamentStore(files).load().notices)
    }
    @Test fun preferencesPersistSettingsAndLibraryIndependently() = runTest {
        withRepository { repo, _, files ->
            val initial=repo.load();assertEquals(AppSettings(),initial.settings)
            val updated=initial.settings.copy(language="en-US",theme=ThemeMode.Light,machineRate=decimal("0.30"))
            repo.saveSettings(updated);repo.saveFilaments(listOf(initial.filaments[4].copy(active=false)))
            assertEquals(updated,repo.load().settings);assertFalse(repo.load().filaments.single().active)
            files.data.remove("filaments.json");assertEquals(updated,repo.load().settings);assertEquals(7,repo.load().filaments.size)
        }
    }
    @Test fun missingSettingsPreservesExistingLibrary() = runTest {
        withRepository { repo, store, _ ->
            repo.saveFilaments(emptyList());store.edit{it.clear()}
            assertEquals(AppSettings(),repo.load().settings);assertTrue(repo.load().filaments.isEmpty())
        }
    }
    @Test fun corruptPreferencesJsonIsPreservedAndRecovered() = runTest {
        withRepository { repo, store, files ->
            store.edit{it[stringPreferencesKey("settings_json")]="{bad"}
            val loaded=repo.load();assertEquals(AppSettings(),loaded.settings)
            assertTrue(loaded.notices.contains(StorageNotice.InvalidData));assertEquals("{bad",files.data["settings.json.invalid"])
            assertTrue(repo.load().notices.isEmpty())
        }
    }
    private suspend fun withRepository(block:suspend(PreferencesRepository,androidx.datastore.core.DataStore<Preferences>,MemoryFiles)->Unit) {
        val dir=kotlin.io.path.createTempDirectory("makertally-test").toFile()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try {
            val store=PreferenceDataStoreFactory.create(scope=scope,produceFile={File(dir,"settings.preferences_pb")})
            val files=MemoryFiles();block(PreferencesRepository(store,files),store,files)
        } finally { scope.cancel(); scope.coroutineContext[Job]?.join();dir.deleteRecursively() }
    }
}

