package dev.hawk0f.checkmates.ble

object BleConstants {
    const val MAX_ADVERTISED_NAME_BYTES = 11
    const val SERVICE_UUID = "1b8c0a4e-3f52-4b8d-9e17-6d2a84c95f01"
    const val CHAR_MOVE_TO_HOST = "1b8c0a4e-3f52-4b8d-9e17-6d2a84c95f02"
    const val CHAR_MOVE_TO_GUEST = "1b8c0a4e-3f52-4b8d-9e17-6d2a84c95f03"
    const val CHAR_FEN = "1b8c0a4e-3f52-4b8d-9e17-6d2a84c95f04"
    const val CHAR_PLAYER_NAME = "1b8c0a4e-3f52-4b8d-9e17-6d2a84c95f05"

    fun advertisedNameBytes(name: String): ByteArray {
        var bytes = name.encodeToByteArray()
        if (bytes.size <= MAX_ADVERTISED_NAME_BYTES) {
            return bytes
        }
        bytes = bytes.copyOf(MAX_ADVERTISED_NAME_BYTES)
        while (bytes.isNotEmpty()) {
            val valid = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.isSuccess
            if (valid) {
                return bytes
            }
            bytes = bytes.copyOf(bytes.size - 1)
        }
        return bytes
    }

    fun advertisedName(bytes: ByteArray?): String? = bytes
        ?.decodeToString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
