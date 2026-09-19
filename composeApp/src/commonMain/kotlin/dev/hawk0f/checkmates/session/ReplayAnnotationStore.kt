package dev.hawk0f.checkmates.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class ReplayAnnotation(
    val gameId: Long,
    val ply: Int,
    val text: String
)

interface ReplayAnnotationPersistence {
    fun annotations(gameId: Long): Map<Int, String>
    fun save(gameId: Long, ply: Int, text: String)
}

object ReplayAnnotationStore : ReplayAnnotationPersistence {

    private const val KEY_ANNOTATIONS = "replay.annotations"
    private const val MAX_ANNOTATIONS = 500

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(ReplayAnnotation.serializer())

    override fun annotations(gameId: Long): Map<Int, String> = load()
        .filter { it.gameId == gameId }
        .associate { it.ply to it.text }

    override fun save(gameId: Long, ply: Int, text: String) {
        val settings = settings ?: return
        val normalized = text.trim()
        val retained = load().filterNot { it.gameId == gameId && it.ply == ply }
        val updated = if (normalized.isEmpty()) {
            retained
        } else {
            retained + ReplayAnnotation(gameId, ply, normalized)
        }
        settings[KEY_ANNOTATIONS] = json.encodeToString(serializer, updated.takeLast(MAX_ANNOTATIONS))
    }

    private fun load(): List<ReplayAnnotation> {
        val raw = settings?.getStringOrNull(KEY_ANNOTATIONS) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull() ?: emptyList()
    }
}
