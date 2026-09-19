package dev.hawk0f.checkmates

import dev.hawk0f.checkmates.shared.protocol.ShortCode
import kotlinx.coroutines.flow.MutableStateFlow

object DeepLinkHandler {

    val pendingCode = MutableStateFlow<String?>(null)
    val pendingCorrespondenceId = MutableStateFlow<Long?>(null)

    fun handle(url: String) {
        Regex("correspondence/(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let {
            pendingCorrespondenceId.value = it
            return
        }
        ShortCode.extractFromText(url)?.let { pendingCode.value = it }
    }

    fun openCorrespondence(id: Long) {
        pendingCorrespondenceId.value = id
    }

    fun consumeCorrespondence(): Long? {
        val id = pendingCorrespondenceId.value
        pendingCorrespondenceId.value = null
        return id
    }

    fun consume(): String? {
        val code = pendingCode.value
        pendingCode.value = null
        return code
    }
}
