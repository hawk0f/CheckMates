package dev.hawk0f.checkmates.shared

import dev.hawk0f.checkmates.shared.util.base64UrlNoPadding
import dev.hawk0f.checkmates.shared.util.hex
import dev.hawk0f.checkmates.shared.util.sha256
import kotlin.test.Test
import kotlin.test.assertEquals

class DigestTest {

    @Test
    fun sha256MatchesKnownVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(sha256(ByteArray(0)))
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(sha256("abc".encodeToByteArray()))
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex(sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()))
        )
    }

    @Test
    fun sha256HandlesLongInput() {
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            hex(sha256(ByteArray(1_000_000) { 'a'.code.toByte() }))
        )
    }

    @Test
    fun base64UrlIsUrlSafeAndUnpadded() {
        assertEquals("", base64UrlNoPadding(ByteArray(0)))
        assertEquals("YQ", base64UrlNoPadding("a".encodeToByteArray()))
        assertEquals("YWI", base64UrlNoPadding("ab".encodeToByteArray()))
        assertEquals("YWJj", base64UrlNoPadding("abc".encodeToByteArray()))
        assertEquals("_-4", base64UrlNoPadding(byteArrayOf(0xff.toByte(), 0xee.toByte())))
    }
}
