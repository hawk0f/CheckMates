package dev.hawk0f.checkmates.ble

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BleConstantsTest {

    @Test
    fun advertisedNamePreservesShortName() {
        val bytes = BleConstants.advertisedNameBytes("Alice")

        assertEquals("Alice", BleConstants.advertisedName(bytes))
    }

    @Test
    fun advertisedNameTruncatesWithoutBreakingUtf8() {
        val bytes = BleConstants.advertisedNameBytes("Александр")

        assertTrue(bytes.size <= BleConstants.MAX_ADVERTISED_NAME_BYTES)
        assertEquals("Алекс", BleConstants.advertisedName(bytes))
    }

    @Test
    fun emptyAdvertisementHasNoName() {
        assertNull(BleConstants.advertisedName(byteArrayOf()))
    }
}
