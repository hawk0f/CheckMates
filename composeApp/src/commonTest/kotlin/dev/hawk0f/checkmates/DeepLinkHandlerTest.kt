package dev.hawk0f.checkmates

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeepLinkHandlerTest {

    @AfterTest
    fun clear() {
        DeepLinkHandler.consume()
        DeepLinkHandler.consumeCorrespondence()
    }

    @Test
    fun correspondenceNotificationLinkIsRoutedToTheGame() {
        DeepLinkHandler.handle("checkmates://correspondence/42")

        assertEquals(42, DeepLinkHandler.consumeCorrespondence())
    }
}
