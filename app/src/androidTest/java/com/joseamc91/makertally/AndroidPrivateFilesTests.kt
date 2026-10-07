package com.joseamc91.makertally

import android.system.Os
import androidx.test.platform.app.InstrumentationRegistry
import com.joseamc91.makertally.data.*
import com.joseamc91.makertally.domain.Defaults
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

/** Exercises the real platform AtomicFile, without touching the user's library. */
class AndroidPrivateFilesTests {
    private lateinit var directory: File
    private lateinit var files: AndroidPrivateFiles
    private val profiles = listOf(Defaults.filaments().first().copy(brand = "Recovery test", active = false))

    @Before fun setUp() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        directory = File.createTempFile("atomic-regression-", "", cache)
        check(directory.delete() && directory.mkdir())
        files = AndroidPrivateFiles(directory)
    }

    @After fun tearDown() { directory.deleteRecursively() }

    @Test fun validBaseLoadsWithoutChangingItsContents() {
        val text = DataCodec.encodeFilaments(profiles)
        files.writeAtomically("filaments.json", text)
        assertEquals(text, files.read("filaments.json"))
        val loaded = FilamentStore(files).load()
        assertEquals(profiles, loaded.filaments)
        assertTrue(loaded.notices.isEmpty())
        assertEquals(text, File(directory, "filaments.json").readText())
    }

    @Test fun missingBaseRecoversLastCommittedBackupInsteadOfSeedingDefaults() {
        val text = DataCodec.encodeFilaments(profiles)
        File(directory, "filaments.json.bak").writeText(text)
        assertFalse(File(directory, "filaments.json").exists())
        val loaded = FilamentStore(files).load()
        assertEquals(profiles, loaded.filaments)
        assertTrue(loaded.notices.isEmpty())
        assertEquals(text, File(directory, "filaments.json").readText())
        assertEquals(profiles, FilamentStore(files).load().filaments)
    }

    @Test fun committedBackupTakesPrecedenceOverAnInterruptedBase() {
        File(directory, "filaments.json.bak").writeText(DataCodec.encodeFilaments(profiles))
        File(directory, "filaments.json").writeText("{interrupted")
        val loaded = FilamentStore(files).load()
        assertEquals(profiles, loaded.filaments)
        assertTrue(loaded.notices.isEmpty())
        assertFalse(File(directory, "filaments.json.invalid").exists())
    }

    @Test fun trulyMissingFileReturnsNullAndFirstRunSeedsOnce() {
        assertNull(files.read("filaments.json"))
        val first = FilamentStore(files).load()
        assertEquals(7, first.filaments.size)
        assertTrue(first.notices.isEmpty())
        assertEquals(first.filaments, FilamentStore(files).load().filaments)
    }

    @Test fun invalidContentsArePreservedBeforeRecovery() {
        files.writeAtomically("filaments.json", "{bad")
        val loaded = FilamentStore(files).load()
        assertEquals(listOf(StorageNotice.InvalidData), loaded.notices)
        assertEquals("{bad", files.read("filaments.json.invalid"))
        assertEquals(7, loaded.filaments.size)
        assertEquals(loaded.filaments, FilamentStore(files).load().filaments)
    }

    @Test fun directoryAtFilePathPropagatesIoErrorAndReportsReadFailure() {
        val base = File(directory, "filaments.json")
        assertTrue(base.mkdir())
        assertThrows(IOException::class.java) { files.read("filaments.json") }
        assertEquals(listOf(StorageNotice.ReadFailed), FilamentStore(files).load().notices)
        assertTrue(base.isDirectory)
    }

    @Test fun deniedAccessIsNotTreatedAsMissingData() {
        files.writeAtomically("filaments.json", DataCodec.encodeFilaments(profiles))
        Os.chmod(directory.path, 0)
        try {
            assertThrows(IOException::class.java) { files.read("filaments.json") }
            assertEquals(listOf(StorageNotice.ReadFailed), FilamentStore(files).load().notices)
        } finally {
            Os.chmod(directory.path, 448) // 0700, so cleanup can remove only this test directory.
        }
        assertEquals(profiles, FilamentStore(files).load().filaments)
    }
}
