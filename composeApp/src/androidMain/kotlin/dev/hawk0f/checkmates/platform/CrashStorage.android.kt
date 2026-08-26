package dev.hawk0f.checkmates.platform

import android.os.Build
import dev.hawk0f.checkmates.ble.BleAppContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private const val MAX_TRACE_CHARS = 8_000
private const val MAX_PENDING_FILES = 5

actual object CrashStorage {

    private val directory: File?
        get() = BleAppContext.applicationContext?.filesDir?.let { File(it, "crashes").apply { mkdirs() } }

    private var handlerInstalled = false

    actual fun installHandler() {
        if (handlerInstalled) {
            return
        }
        handlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { store(error) }
            previous?.uncaughtException(thread, error)
        }
    }

    actual fun pendingTraces(): List<String> {
        val files = directory?.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        return files.takeLast(MAX_PENDING_FILES).mapNotNull { file ->
            runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
        }
    }

    actual fun clearPending() {
        directory?.listFiles()?.forEach { it.delete() }
    }

    actual val platformName: String get() = "android"

    actual val platformVersion: String get() = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    actual val appVersion: String
        get() {
            val context = BleAppContext.applicationContext ?: return "unknown"
            return runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            }.getOrDefault("unknown")
        }

    private fun store(error: Throwable) {
        val target = directory ?: return
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        File(target, "crash-${System.currentTimeMillis()}.txt")
            .writeText(writer.toString().take(MAX_TRACE_CHARS))
    }
}
