package dev.hawk0f.checkmates.platform

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual suspend fun currentPushToken(): String? = suspendCancellableCoroutine { continuation ->
    runCatching {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (continuation.isActive) {
                continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }.onFailure {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }
}
