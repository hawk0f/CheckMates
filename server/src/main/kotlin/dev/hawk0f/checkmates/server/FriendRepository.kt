package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class FriendRepository(private val database: Database) {

    suspend fun add(userId: Long, displayName: String, nowMillis: Long = System.currentTimeMillis()): FriendSummary? =
        dbQuery {
            val friend = Users
                .selectAll()
                .where { (Users.displayName eq displayName.trim()) and (Users.id neq userId) }
                .firstOrNull() ?: return@dbQuery null
            val friendId = friend[Users.id]
            Friends.insertIgnore {
                it[Friends.userId] = userId
                it[friendUserId] = friendId
                it[createdAtMillis] = nowMillis
            }
            FriendSummary(userId = friendId, displayName = friend[Users.displayName])
        }

    suspend fun remove(userId: Long, friendId: Long): Boolean = dbQuery {
        Friends.deleteWhere { (Friends.userId eq userId) and (friendUserId eq friendId) } > 0
    }

    suspend fun list(userId: Long): List<FriendSummary> = dbQuery {
        val ids = Friends
            .select(Friends.friendUserId)
            .where { Friends.userId eq userId }
            .map { it[Friends.friendUserId] }
        if (ids.isEmpty()) {
            return@dbQuery emptyList()
        }
        Users.selectAll()
            .where { Users.id inList ids }
            .map { FriendSummary(userId = it[Users.id], displayName = it[Users.displayName]) }
    }

    suspend fun recentOpponents(userId: Long, limit: Int = 10): List<FriendSummary> = dbQuery {
        val myName = Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.displayName)
            ?: return@dbQuery emptyList()
        val names = GameRecords.selectAll()
            .where { GameRecords.userId eq userId }
            .orderBy(GameRecords.finishedAtMillis, SortOrder.DESC)
            .limit(60)
            .mapNotNull { row ->
                val white = row[GameRecords.whiteName]
                val black = row[GameRecords.blackName]
                val opponent = if (white == myName) black else white
                opponent.takeIf { it != myName && it.isNotBlank() }?.let { it to row[GameRecords.finishedAtMillis] }
            }
        val newestByName = linkedMapOf<String, Long>()
        for ((name, millis) in names) {
            if (!newestByName.containsKey(name)) {
                newestByName[name] = millis
            }
        }
        val known = Users.selectAll()
            .where { Users.displayName inList newestByName.keys.toList() }
            .associate { it[Users.displayName] to it[Users.id] }
        newestByName.entries.take(limit).map { (name, millis) ->
            FriendSummary(
                userId = known[name] ?: -1,
                displayName = name,
                lastPlayedMillis = millis
            )
        }
    }

    suspend fun savePushToken(userId: Long, token: String) = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[pushToken] = token.take(200)
        }
        Unit
    }

    suspend fun pushTokenOf(userId: Long): String? = dbQuery {
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.pushToken)
    }

    suspend fun displayNameOf(userId: Long): String? = dbQuery {
        Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.displayName)
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }
}
