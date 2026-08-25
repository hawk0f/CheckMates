package dev.hawk0f.checkmates.server

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val login = varchar("login", 32).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val displayName = varchar("display_name", 40)
    val avatarKind = varchar("avatar_kind", 10)
    val avatarValue = varchar("avatar_value", 20)
    val createdAtMillis = long("created_at_millis")
    val pushToken = varchar("push_token", 200).nullable()
    override val primaryKey = PrimaryKey(id)
}

object AuthSessions : Table("auth_sessions") {
    val token = varchar("token", 64)
    val userId = long("user_id").references(Users.id)
    val createdAtMillis = long("created_at_millis")
    val expiresAtMillis = long("expires_at_millis").default(0)
    override val primaryKey = PrimaryKey(token)
}

object GameRecords : Table("game_records") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id).index()
    val mode = varchar("mode", 10)
    val myColor = varchar("my_color", 5).nullable()
    val whiteName = varchar("white_name", 40)
    val blackName = varchar("black_name", 40)
    val winner = varchar("winner", 5).nullable()
    val reason = varchar("reason", 25)
    val uciHistory = text("uci_history")
    val finishedAtMillis = long("finished_at_millis")
    override val primaryKey = PrimaryKey(id)
}

object GameRooms : Table("game_rooms") {
    val gameId = varchar("game_id", 64)
    val shortCode = varchar("short_code", 16)
    val status = varchar("status", 24)
    val initialSeconds = integer("initial_seconds").default(-1)
    val incrementSeconds = integer("increment_seconds").default(-1)
    val clockMode = varchar("clock_mode", 12).default("fischer")
    val blackInitialSeconds = integer("black_initial_seconds").default(-1)
    val blackIncrementSeconds = integer("black_increment_seconds").default(-1)
    val uciHistory = text("uci_history").default("")
    val turnStartedAtMillis = long("turn_started_at_millis").default(0)
    val lastActivityMillis = long("last_activity_millis").default(0)
    val whiteToken = varchar("white_token", 64).nullable()
    val whiteName = varchar("white_name", 40).nullable()
    val whiteUserId = long("white_user_id").nullable()
    val whiteMillis = long("white_millis").default(-1)
    val blackToken = varchar("black_token", 64).nullable()
    val blackName = varchar("black_name", 40).nullable()
    val blackUserId = long("black_user_id").nullable()
    val blackMillis = long("black_millis").default(-1)
    override val primaryKey = PrimaryKey(gameId)
}

object UserRatings : Table("user_ratings") {
    val userId = long("user_id").references(Users.id)
    val speed = varchar("speed", 12)
    val rating = double("rating")
    val deviation = double("deviation")
    val volatility = double("volatility")
    val games = integer("games").default(0)
    val lastPlayedMillis = long("last_played_millis").default(0)
    override val primaryKey = PrimaryKey(userId, speed)
}

object Friends : Table("friends") {
    val userId = long("user_id").references(Users.id)
    val friendUserId = long("friend_user_id").references(Users.id)
    val createdAtMillis = long("created_at_millis")
    override val primaryKey = PrimaryKey(userId, friendUserId)
}

object CrashReports : Table("crash_reports") {
    val id = long("id").autoIncrement()
    val platform = varchar("platform", 16)
    val appVersion = varchar("app_version", 32)
    val osVersion = varchar("os_version", 64)
    val stackTrace = text("stack_trace")
    val occurredAtMillis = long("occurred_at_millis")
    val receivedAtMillis = long("received_at_millis").index()
    override val primaryKey = PrimaryKey(id)
}

object SchemaVersion : Table("schema_version") {
    val id = integer("id")
    val version = integer("version")
    override val primaryKey = PrimaryKey(id)
}

object Db {

    const val LATEST_VERSION = 7

    private val ADDED_COLUMNS = listOf(
        Triple("auth_sessions", "expires_at_millis", "INTEGER NOT NULL DEFAULT 0"),
        Triple("game_rooms", "clock_mode", "TEXT NOT NULL DEFAULT 'fischer'"),
        Triple("game_rooms", "black_initial_seconds", "INTEGER NOT NULL DEFAULT -1"),
        Triple("game_rooms", "black_increment_seconds", "INTEGER NOT NULL DEFAULT -1"),
        Triple("user_ratings", "games", "INTEGER NOT NULL DEFAULT 0"),
        Triple("user_ratings", "last_played_millis", "INTEGER NOT NULL DEFAULT 0"),
        Triple("users", "push_token", "TEXT")
    )

    fun init(path: String): Database {
        File(path).parentFile?.mkdirs()
        enableWriteAheadLog(path)
        val database = Database.connect("jdbc:sqlite:$path?busy_timeout=5000", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(database) {
            SchemaUtils.create(
                Users,
                AuthSessions,
                GameRecords,
                SchemaVersion,
                GameRooms,
                UserRatings,
                CrashReports,
                Friends
            )
            migrate()
        }
        return database
    }

    private fun enableWriteAheadLog(path: String) {
        DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL")
            }
        }
    }

    private fun JdbcTransaction.migrate() {
        val current = readVersion()
        if (current >= LATEST_VERSION) {
            return
        }
        if (current < 2) {
            ensureColumn("auth_sessions", "expires_at_millis", "INTEGER NOT NULL DEFAULT 0")
            exec("CREATE INDEX IF NOT EXISTS auth_sessions_user_id ON auth_sessions(user_id)")
            exec("CREATE INDEX IF NOT EXISTS game_records_user_finished ON game_records(user_id, finished_at_millis)")
        }
        if (current < 3) {
            exec("CREATE INDEX IF NOT EXISTS game_rooms_activity ON game_rooms(last_activity_millis)")
            exec("CREATE INDEX IF NOT EXISTS game_rooms_short_code ON game_rooms(short_code)")
        }
        if (current < 4) {
            exec("CREATE INDEX IF NOT EXISTS user_ratings_board ON user_ratings(speed, rating)")
        }
        if (current < 7) {
            for ((table, column, definition) in ADDED_COLUMNS) {
                ensureColumn(table, column, definition)
            }
        }
        writeVersion(LATEST_VERSION)
    }

    private fun JdbcTransaction.readVersion(): Int =
        exec("SELECT version FROM schema_version WHERE id = 1") { rs ->
            if (rs.next()) rs.getInt("version") else 0
        } ?: 0

    private fun JdbcTransaction.ensureColumn(table: String, column: String, definition: String) {
        if (!columnExists(table, column)) {
            exec("ALTER TABLE $table ADD COLUMN $column $definition")
        }
    }

    private fun JdbcTransaction.columnExists(table: String, column: String): Boolean =
        exec("PRAGMA table_info($table)") { rs ->
            var found = false
            while (rs.next()) {
                if (rs.getString("name") == column) {
                    found = true
                }
            }
            found
        } == true

    private fun JdbcTransaction.writeVersion(version: Int) {
        exec("INSERT INTO schema_version (id, version) VALUES (1, $version) ON CONFLICT(id) DO UPDATE SET version = $version")
    }
}
