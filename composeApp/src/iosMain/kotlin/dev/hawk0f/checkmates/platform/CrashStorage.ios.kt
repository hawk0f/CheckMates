package dev.hawk0f.checkmates.platform

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

private const val KEY_PENDING = "crash.pending"
private const val MAX_TRACE_CHARS = 8_000
private const val SEPARATOR = "\n---\n"

actual object CrashStorage {

    @OptIn(ExperimentalNativeApi::class)
    actual fun installHandler() {
        setUnhandledExceptionHook { error ->
            val trace = (error.stackTraceToString()).take(MAX_TRACE_CHARS)
            val existing = NSUserDefaults.standardUserDefaults.stringForKey(KEY_PENDING)
            val combined = if (existing.isNullOrBlank()) trace else existing + SEPARATOR + trace
            NSUserDefaults.standardUserDefaults.setObject(combined, KEY_PENDING)
            NSUserDefaults.standardUserDefaults.synchronize()
        }
    }

    actual fun pendingTraces(): List<String> {
        val stored = NSUserDefaults.standardUserDefaults.stringForKey(KEY_PENDING) ?: return emptyList()
        return stored.split(SEPARATOR).filter { it.isNotBlank() }
    }

    actual fun clearPending() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_PENDING)
    }

    actual val platformName: String get() = "ios"

    actual val platformVersion: String get() = UIDevice.currentDevice.systemVersion

    actual val appVersion: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
}
