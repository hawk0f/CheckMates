package dev.hawk0f.checkmates.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings

enum class AppFlow(val id: String) {
    CHECKMATES("checkmates"),
    LICHESS("lichess");

    companion object {
        fun byId(id: String?): AppFlow? = entries.find { it.id == id }
    }
}

object FlowManager {

    private const val KEY_FLOW = "app.flow"

    private val settings: Settings? by lazy { runCatching { Settings() }.getOrNull() }

    var current by mutableStateOf(AppFlow.byId(settings?.getStringOrNull(KEY_FLOW)))
        private set

    val isChosen: Boolean get() = current != null

    fun select(flow: AppFlow) {
        current = flow
        settings?.putString(KEY_FLOW, flow.id)
    }

    fun other(): AppFlow = if (current == AppFlow.LICHESS) AppFlow.CHECKMATES else AppFlow.LICHESS
}
