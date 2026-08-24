package dev.hawk0f.checkmates.platform

expect object CrashStorage {
    fun installHandler()
    fun pendingTraces(): List<String>
    fun clearPending()
    val platformName: String
    val platformVersion: String
    val appVersion: String
}
