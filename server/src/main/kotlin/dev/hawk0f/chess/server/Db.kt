package dev.hawk0f.chess.server

import java.io.File
import java.sql.Connection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val login = varchar("login", 32).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val displayName = varchar("display_name", 40)
    val avatarKind = varchar("avatar_kind", 10)
    val avatarValue = varchar("avatar_value", 20)
    val createdAtMillis = long("created_at_millis")
    override val primaryKey = PrimaryKey(id)
}

object AuthSessions : Table("auth_sessions") {
    val token = varchar("token", 64)
    val userId = long("user_id").references(Users.id)
    val createdAtMillis = long("created_at_millis")
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

object Db {

    fun init(path: String): Database {
        File(path).parentFile?.mkdirs()
        val database = Database.connect("jdbc:sqlite:$path", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(database) {
            SchemaUtils.create(Users, AuthSessions, GameRecords)
        }
        return database
    }
}
