package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import dev.hawk0f.checkmates.platform.epochMillis

data class SavedOnlineGame(
    val gameId: String,
    val playerToken: String,
    val myName: String,
    val opponentName: String?,
    val savedAtMillis: Long
)

interface OnlineGamePersistence {
    fun load(): SavedOnlineGame?
    fun save(game: SavedOnlineGame)
    fun clear()
}

object OnlineGameStore : OnlineGamePersistence {

    const val MAX_AGE_MILLIS = 12L * 60 * 60 * 1000

    private const val KEY_GAME_ID = "online.gameId"
    private const val KEY_PLAYER_TOKEN = "online.playerToken"
    private const val KEY_MY_NAME = "online.myName"
    private const val KEY_OPPONENT_NAME = "online.opponentName"
    private const val KEY_SAVED_AT = "online.savedAt"

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }

    override fun save(game: SavedOnlineGame) {
        val settings = settings ?: return
        settings.putString(KEY_GAME_ID, game.gameId)
        settings.putString(KEY_PLAYER_TOKEN, game.playerToken)
        settings.putString(KEY_MY_NAME, game.myName)
        settings.putString(KEY_OPPONENT_NAME, game.opponentName.orEmpty())
        settings.putLong(KEY_SAVED_AT, game.savedAtMillis)
    }

    override fun load(): SavedOnlineGame? {
        val settings = settings ?: return null
        val gameId = settings.getStringOrNull(KEY_GAME_ID)?.takeIf { it.isNotBlank() } ?: return null
        val playerToken = settings.getStringOrNull(KEY_PLAYER_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        val savedAt = settings.getLong(KEY_SAVED_AT, 0)
        if (epochMillis() - savedAt > MAX_AGE_MILLIS) {
            clear()
            return null
        }
        return SavedOnlineGame(
            gameId = gameId,
            playerToken = playerToken,
            myName = settings.getStringOrNull(KEY_MY_NAME).orEmpty().ifBlank { "Player" },
            opponentName = settings.getStringOrNull(KEY_OPPONENT_NAME)?.takeIf { it.isNotBlank() },
            savedAtMillis = savedAt
        )
    }

    override fun clear() {
        val settings = settings ?: return
        for (key in listOf(KEY_GAME_ID, KEY_PLAYER_TOKEN, KEY_MY_NAME, KEY_OPPONENT_NAME, KEY_SAVED_AT)) {
            settings.remove(key)
        }
    }
}
