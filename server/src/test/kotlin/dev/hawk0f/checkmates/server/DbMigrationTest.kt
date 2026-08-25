package dev.hawk0f.checkmates.server

import java.io.File
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DbMigrationTest {

    private fun newPath(): String {
        val file = File.createTempFile("migration-${UUID.randomUUID()}", ".db")
        file.delete()
        file.deleteOnExit()
        return file.absolutePath
    }

    private fun <T> onRawDatabase(path: String, block: (java.sql.Connection) -> T): T =
        DriverManager.getConnection("jdbc:sqlite:$path").use(block)

    private fun columnsOf(path: String, table: String): List<String> = onRawDatabase(path) { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info($table)").use { rs ->
                buildList {
                    while (rs.next()) {
                        add(rs.getString("name"))
                    }
                }
            }
        }
    }

    private fun schemaVersionOf(path: String): Int = onRawDatabase(path) { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT version FROM schema_version WHERE id = 1").use { rs ->
                if (rs.next()) rs.getInt("version") else 0
            }
        }
    }

    @Test
    fun aFreshDatabaseLandsOnTheLatestVersion() {
        val path = newPath()
        Db.init(path)

        assertEquals(Db.LATEST_VERSION, schemaVersionOf(path))
        assertTrue("push_token" in columnsOf(path, "users"))
    }

    @Test
    fun aColumnLostByAnEarlierMigrationIsAddedBack() {
        val path = newPath()
        Db.init(path)
        onRawDatabase(path) { connection ->
            connection.createStatement().use { statement ->
                statement.execute("ALTER TABLE users DROP COLUMN push_token")
                statement.execute("ALTER TABLE game_rooms DROP COLUMN clock_mode")
                statement.execute("UPDATE schema_version SET version = 6 WHERE id = 1")
            }
        }
        assertTrue("push_token" !in columnsOf(path, "users"))

        Db.init(path)

        assertTrue("push_token" in columnsOf(path, "users"))
        assertTrue("clock_mode" in columnsOf(path, "game_rooms"))
        assertEquals(Db.LATEST_VERSION, schemaVersionOf(path))
    }
}
