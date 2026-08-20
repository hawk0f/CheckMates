package dev.hawk0f.checkmates

import dev.hawk0f.checkmates.net.lichess.LichessAuth
import dev.hawk0f.checkmates.shared.protocol.ShortCode
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow

data class LichessAuthCallback(val code: String?, val state: String?, val error: String?)

object DeepLinkHandler {

    val pendingCode = MutableStateFlow<String?>(null)
    val pendingLichessAuth = MutableStateFlow<LichessAuthCallback?>(null)

    fun handle(url: String) {
        if (url.startsWith(LichessAuth.REDIRECT_URI)) {
            pendingLichessAuth.value = parseLichessCallback(url)
            return
        }
        ShortCode.extractFromText(url)?.let { pendingCode.value = it }
    }

    fun consume(): String? {
        val code = pendingCode.value
        pendingCode.value = null
        return code
    }

    fun consumeLichessAuth(): LichessAuthCallback? {
        val callback = pendingLichessAuth.value
        pendingLichessAuth.value = null
        return callback
    }

    private fun parseLichessCallback(url: String): LichessAuthCallback {
        val parameters = runCatching { Url(url).parameters }.getOrNull()
        return LichessAuthCallback(
            code = parameters?.get("code"),
            state = parameters?.get("state"),
            error = parameters?.get("error")
        )
    }
}
