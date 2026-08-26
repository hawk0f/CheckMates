package dev.hawk0f.checkmates.ui.preview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreviewCatalogTest {

    @Test
    fun previewIdsAreUnique() {
        val ids = previewSpecs.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate preview ids: $ids")
    }

    @Test
    fun everySpecHasAPreviewFunction() {
        val functions = listOf(
            "dev.hawk0f.checkmates.ui.preview.ComponentPreviewsKt",
            "dev.hawk0f.checkmates.ui.preview.BoardPreviewsKt",
            "dev.hawk0f.checkmates.ui.preview.ScreenPreviewsKt"
        ).flatMap { name ->
            Class.forName(name).declaredMethods.map { it.name.substringBefore('$') }
        }.filter { it.endsWith("Preview") }.toSet()
        assertTrue(functions.isNotEmpty())
        assertEquals(previewSpecs.size, functions.size, "preview functions: $functions")
    }
}
