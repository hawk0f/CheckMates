package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import dev.hawk0f.checkmates.platform.epochMillis
import dev.hawk0f.checkmates.shared.protocol.ClockMode
import dev.hawk0f.checkmates.shared.protocol.TimeControl

data class SavedHotseatGame(
    val uciHistory: List<String>,
    val timeControl: TimeControl?,
    val whiteMillis: Long?,
    val blackMillis: Long?,
    val savedAtMillis: Long,
    val seriesWhiteWins: Int,
    val seriesBlackWins: Int,
    val seriesDraws: Int
)

interface HotseatGamePersistence {
    fun load(): SavedHotseatGame?
    fun save(game: SavedHotseatGame)
    fun clear()
}

object HotseatGameStore : HotseatGamePersistence {

    private const val KEY_HISTORY = "hotseat.history"
    private const val KEY_INITIAL_SECONDS = "hotseat.initialSeconds"
    private const val KEY_INCREMENT_SECONDS = "hotseat.incrementSeconds"
    private const val KEY_WHITE_MILLIS = "hotseat.whiteMillis"
    private const val KEY_BLACK_MILLIS = "hotseat.blackMillis"
    private const val KEY_CLOCK_MODE = "hotseat.clockMode"
    private const val KEY_BLACK_INITIAL_SECONDS = "hotseat.blackInitialSeconds"
    private const val KEY_BLACK_INCREMENT_SECONDS = "hotseat.blackIncrementSeconds"
    private const val KEY_SAVED_AT = "hotseat.savedAt"
    private const val KEY_SERIES_WHITE = "hotseat.seriesWhite"
    private const val KEY_SERIES_BLACK = "hotseat.seriesBlack"
    private const val KEY_SERIES_DRAWS = "hotseat.seriesDraws"

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }

    override fun save(game: SavedHotseatGame) {
        val settings = settings ?: return
        settings.putString(KEY_HISTORY, game.uciHistory.joinToString(" "))
        settings.putInt(KEY_INITIAL_SECONDS, game.timeControl?.initialSeconds ?: -1)
        settings.putInt(KEY_INCREMENT_SECONDS, game.timeControl?.incrementSeconds ?: -1)
        settings.putString(KEY_CLOCK_MODE, game.timeControl?.mode?.id ?: "")
        settings.putInt(KEY_BLACK_INITIAL_SECONDS, game.timeControl?.blackInitialSeconds ?: -1)
        settings.putInt(KEY_BLACK_INCREMENT_SECONDS, game.timeControl?.blackIncrementSeconds ?: -1)
        settings.putLong(KEY_WHITE_MILLIS, game.whiteMillis ?: -1)
        settings.putLong(KEY_BLACK_MILLIS, game.blackMillis ?: -1)
        settings.putLong(KEY_SAVED_AT, game.savedAtMillis)
        settings.putInt(KEY_SERIES_WHITE, game.seriesWhiteWins)
        settings.putInt(KEY_SERIES_BLACK, game.seriesBlackWins)
        settings.putInt(KEY_SERIES_DRAWS, game.seriesDraws)
    }

    override fun load(): SavedHotseatGame? {
        val settings = settings ?: return null
        val history = settings.getStringOrNull(KEY_HISTORY)
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?: return null
        if (history.isEmpty()) {
            return null
        }
        val initialSeconds = settings.getInt(KEY_INITIAL_SECONDS, -1)
        val incrementSeconds = settings.getInt(KEY_INCREMENT_SECONDS, -1)
        val timeControl = if (initialSeconds >= 0 && incrementSeconds >= 0) {
            TimeControl(
                initialSeconds = initialSeconds,
                incrementSeconds = incrementSeconds,
                mode = ClockMode.byId(settings.getStringOrNull(KEY_CLOCK_MODE)),
                blackInitialSeconds = settings.getInt(KEY_BLACK_INITIAL_SECONDS, -1).takeIf { it >= 0 },
                blackIncrementSeconds = settings.getInt(KEY_BLACK_INCREMENT_SECONDS, -1).takeIf { it >= 0 }
            )
        } else {
            null
        }
        return SavedHotseatGame(
            uciHistory = history,
            timeControl = timeControl,
            whiteMillis = settings.getLong(KEY_WHITE_MILLIS, -1).takeIf { it >= 0 },
            blackMillis = settings.getLong(KEY_BLACK_MILLIS, -1).takeIf { it >= 0 },
            savedAtMillis = settings.getLong(KEY_SAVED_AT, epochMillis()),
            seriesWhiteWins = settings.getInt(KEY_SERIES_WHITE, 0),
            seriesBlackWins = settings.getInt(KEY_SERIES_BLACK, 0),
            seriesDraws = settings.getInt(KEY_SERIES_DRAWS, 0)
        )
    }

    override fun clear() {
        val settings = settings ?: return
        for (key in listOf(
            KEY_HISTORY,
            KEY_INITIAL_SECONDS,
            KEY_INCREMENT_SECONDS,
            KEY_WHITE_MILLIS,
            KEY_BLACK_MILLIS,
            KEY_SAVED_AT,
            KEY_SERIES_WHITE,
            KEY_SERIES_BLACK,
            KEY_SERIES_DRAWS
        )) {
            settings.remove(key)
        }
    }
}
