package dev.hawk0f.checkmates.session

import dev.hawk0f.checkmates.net.ApiClient
import dev.hawk0f.checkmates.platform.CrashStorage
import dev.hawk0f.checkmates.platform.epochMillis
import dev.hawk0f.checkmates.shared.protocol.CrashReportRequest

object CrashUploader {

    suspend fun uploadPending(api: ApiClient, storage: CrashStorageLike = DefaultCrashStorage): Int {
        val traces = storage.pendingTraces()
        if (traces.isEmpty()) {
            return 0
        }
        var uploaded = 0
        for (trace in traces) {
            val sent = runCatching {
                api.reportCrash(
                    CrashReportRequest(
                        platform = storage.platformName,
                        appVersion = storage.appVersion,
                        osVersion = storage.platformVersion,
                        stackTrace = trace,
                        occurredAtMillis = epochMillis()
                    )
                )
            }.isSuccess
            if (!sent) {
                return uploaded
            }
            uploaded++
        }
        storage.clearPending()
        return uploaded
    }
}

interface CrashStorageLike {
    fun pendingTraces(): List<String>
    fun clearPending()
    val platformName: String
    val platformVersion: String
    val appVersion: String
}

object DefaultCrashStorage : CrashStorageLike {
    override fun pendingTraces(): List<String> = CrashStorage.pendingTraces()
    override fun clearPending() = CrashStorage.clearPending()
    override val platformName: String get() = CrashStorage.platformName
    override val platformVersion: String get() = CrashStorage.platformVersion
    override val appVersion: String get() = CrashStorage.appVersion
}
