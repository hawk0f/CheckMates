package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

interface OpeningProgressPersistence {
    fun bestStreak(lineId: String): Int
    fun mistakes(lineId: String): Int
    fun saveResult(lineId: String, mistakes: Int, streak: Int)
}

object OpeningProgressStore : OpeningProgressPersistence {

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }

    override fun bestStreak(lineId: String): Int = settings?.get("openings.$lineId.streak", 0) ?: 0

    override fun mistakes(lineId: String): Int = settings?.get("openings.$lineId.mistakes", 0) ?: 0

    override fun saveResult(lineId: String, mistakes: Int, streak: Int) {
        val settings = settings ?: return
        settings["openings.$lineId.streak"] = streak
        settings["openings.$lineId.mistakes"] = mistakes
    }
}
