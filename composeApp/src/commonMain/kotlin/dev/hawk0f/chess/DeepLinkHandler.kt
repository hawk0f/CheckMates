package dev.hawk0f.chess

import dev.hawk0f.chess.shared.protocol.ShortCode
import kotlinx.coroutines.flow.MutableStateFlow

object DeepLinkHandler {

    val pendingCode = MutableStateFlow<String?>(null)

    fun handle(url: String) {
        ShortCode.extractFromText(url)?.let { pendingCode.value = it }
    }

    fun consume(): String? {
        val code = pendingCode.value
        pendingCode.value = null
        return code
    }
}
