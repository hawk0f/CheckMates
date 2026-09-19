package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dev.hawk0f.checkmates.shared.puzzle.PuzzleElo
import dev.hawk0f.checkmates.shared.puzzle.Puzzle
import dev.hawk0f.checkmates.shared.puzzle.PuzzleProgress
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface PuzzlePersistence {
    fun loadProgress(): Map<String, PuzzleProgress>
    fun saveProgress(progress: Map<String, PuzzleProgress>)
    fun loadRating(): Int
    fun saveRating(rating: Int)
    fun loadStreak(): Int
    fun saveStreak(streak: Int)
    fun loadPersonalPuzzles(): List<Puzzle>
    fun savePersonalPuzzle(puzzle: Puzzle)
}

object PuzzleStore : PuzzlePersistence {

    private const val KEY_PROGRESS = "puzzles.progress"
    private const val KEY_RATING = "puzzles.rating"
    private const val KEY_STREAK = "puzzles.streak"
    private const val KEY_PERSONAL = "puzzles.personal"

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(PuzzleProgress.serializer())
    private val puzzleSerializer = ListSerializer(Puzzle.serializer())

    override fun loadProgress(): Map<String, PuzzleProgress> {
        val raw = settings?.getStringOrNull(KEY_PROGRESS) ?: return emptyMap()
        val entries = runCatching { json.decodeFromString(serializer, raw) }.getOrNull() ?: return emptyMap()
        return entries.associateBy { it.puzzleId }
    }

    override fun saveProgress(progress: Map<String, PuzzleProgress>) {
        val settings = settings ?: return
        settings[KEY_PROGRESS] = json.encodeToString(serializer, progress.values.toList())
    }

    override fun loadRating(): Int = settings?.get(KEY_RATING, PuzzleElo.DEFAULT_RATING) ?: PuzzleElo.DEFAULT_RATING

    override fun saveRating(rating: Int) {
        settings?.set(KEY_RATING, rating)
    }

    override fun loadStreak(): Int = settings?.get(KEY_STREAK, 0) ?: 0

    override fun saveStreak(streak: Int) {
        settings?.set(KEY_STREAK, streak)
    }

    override fun loadPersonalPuzzles(): List<Puzzle> {
        val raw = settings?.getStringOrNull(KEY_PERSONAL) ?: return emptyList()
        return runCatching { json.decodeFromString(puzzleSerializer, raw) }.getOrNull() ?: emptyList()
    }

    override fun savePersonalPuzzle(puzzle: Puzzle) {
        val settings = settings ?: return
        val current = loadPersonalPuzzles()
        val updated = (current.filterNot { it.id == puzzle.id } + puzzle).takeLast(MAX_PERSONAL_PUZZLES)
        settings[KEY_PERSONAL] = json.encodeToString(puzzleSerializer, updated)
    }

    private const val MAX_PERSONAL_PUZZLES = 200
}
