package dev.hawk0f.checkmates.server

import java.io.File
import java.sql.DriverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteBackup(
    private val databasePath: String,
    private val directory: String,
    private val keep: Int = DEFAULT_KEEP
) {

    suspend fun run(stampMillis: Long = System.currentTimeMillis()): File = withContext(Dispatchers.IO) {
        val target = File(directory)
        target.mkdirs()
        val backup = File(target, "chess-$stampMillis.db")
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("VACUUM INTO '${backup.absolutePath.replace("'", "''")}'")
            }
        }
        prune(target)
        backup
    }

    fun prune(directory: File = File(this.directory)): List<File> {
        val backups = directory.listFiles { file -> file.name.startsWith("chess-") && file.name.endsWith(".db") }
            ?.sortedByDescending { it.name }
            .orEmpty()
        backups.drop(keep).forEach { it.delete() }
        return backups.take(keep)
    }

    companion object {
        const val DEFAULT_KEEP = 7
    }
}
