package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.hawk0f.checkmates.shared.protocol.GameHistoryItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface LocalGameHistoryPersistence {
    fun games(): List<GameHistoryItem>
    fun add(game: GameHistoryItem)
}

object LocalGameHistoryStore : LocalGameHistoryPersistence {

    private const val KEY_GAMES = "history.localGames"
    private const val MAX_GAMES = 50

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(GameHistoryItem.serializer())

    override fun games(): List<GameHistoryItem> {
        val raw = settings?.getStringOrNull(KEY_GAMES) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull() ?: emptyList()
    }

    override fun add(game: GameHistoryItem) {
        val settings = settings ?: return
        val updated = mergeGameHistory(listOf(game), games()).take(MAX_GAMES)
        settings[KEY_GAMES] = json.encodeToString(serializer, updated)
    }
}

fun mergeGameHistory(
    preferred: List<GameHistoryItem>,
    fallback: List<GameHistoryItem>
): List<GameHistoryItem> = buildList {
    for (candidate in preferred + fallback) {
        val duplicate = any { existing ->
            gameHistoryKey(existing) == gameHistoryKey(candidate) &&
                kotlin.math.abs(existing.finishedAtMillis - candidate.finishedAtMillis) < 60_000
        }
        if (!duplicate) {
            add(candidate)
        }
    }
}.sortedByDescending { it.finishedAtMillis }

private fun gameHistoryKey(game: GameHistoryItem): String = listOf(
    game.mode,
    game.whiteName,
    game.blackName,
    game.winner?.name.orEmpty(),
    game.reason.name,
    game.startFen.orEmpty(),
    game.uciHistory.joinToString(" ")
).joinToString("|")
