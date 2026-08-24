package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.CrashReportRequest
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SqliteBackupTest {

    private fun newDatabaseFile(): File {
        val file = File.createTempFile("backup-${UUID.randomUUID()}", ".db")
        file.delete()
        file.deleteOnExit()
        return file
    }

    @Test
    fun aBackupContainsTheRecordedData() = runTest {
        val file = newDatabaseFile()
        val crashes = CrashRepository(Db.init(file.absolutePath))
        crashes.record(CrashReportRequest("android", "1.0", "16", "boom", 1))

        val directory = File(file.parentFile, "backups-${UUID.randomUUID()}")
        val backup = SqliteBackup(file.absolutePath, directory.absolutePath).run(stampMillis = 1)
        assertTrue(backup.exists())
        assertTrue(backup.length() > 0)

        val restored = CrashRepository(Db.init(backup.absolutePath))
        assertEquals(1, restored.recent().size)
        assertEquals("boom", restored.recent().first().stackTrace)
        directory.deleteRecursively()
    }

    @Test
    fun onlyTheNewestBackupsAreKept() = runTest {
        val file = newDatabaseFile()
        Db.init(file.absolutePath)
        val directory = File(file.parentFile, "backups-${UUID.randomUUID()}")
        val backup = SqliteBackup(file.absolutePath, directory.absolutePath, keep = 3)

        for (stamp in 1L..5L) {
            backup.run(stampMillis = stamp * 1_000_000)
        }

        val kept = directory.listFiles()?.map { it.name }.orEmpty().sorted()
        assertEquals(3, kept.size)
        assertTrue(kept.none { it.endsWith("-1000000.db") })
        directory.deleteRecursively()
    }
}
