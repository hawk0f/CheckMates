package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dev.hawk0f.checkmates.shared.opening.OpeningLine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface OpeningProgressPersistence {
    fun bestStreak(lineId: String): Int
    fun mistakes(lineId: String): Int
    fun saveResult(lineId: String, mistakes: Int, streak: Int)
    fun customLines(): List<OpeningLine> = emptyList()
    fun saveCustomLine(line: OpeningLine) = Unit
}

object OpeningProgressStore : OpeningProgressPersistence {

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }
    private val json = Json { ignoreUnknownKeys = true }
    private val linesSerializer = ListSerializer(OpeningLine.serializer())

    override fun bestStreak(lineId: String): Int = settings?.get("openings.$lineId.streak", 0) ?: 0

    override fun mistakes(lineId: String): Int = settings?.get("openings.$lineId.mistakes", 0) ?: 0

    override fun saveResult(lineId: String, mistakes: Int, streak: Int) {
        val settings = settings ?: return
        settings["openings.$lineId.streak"] = streak
        settings["openings.$lineId.mistakes"] = mistakes
    }

    override fun customLines(): List<OpeningLine> {
        val raw = settings?.getStringOrNull(KEY_CUSTOM_LINES) ?: return emptyList()
        return runCatching { json.decodeFromString(linesSerializer, raw) }.getOrNull() ?: emptyList()
    }

    override fun saveCustomLine(line: OpeningLine) {
        val settings = settings ?: return
        val updated = customLines().filterNot { it.id == line.id } + line
        settings[KEY_CUSTOM_LINES] = json.encodeToString(linesSerializer, updated)
    }

    private const val KEY_CUSTOM_LINES = "openings.customLines"
}
