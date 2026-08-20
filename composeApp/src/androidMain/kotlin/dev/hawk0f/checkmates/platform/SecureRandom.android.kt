package dev.hawk0f.checkmates.platform

import java.security.SecureRandom

private val random = SecureRandom()

actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    random.nextBytes(bytes)
    return bytes
}
