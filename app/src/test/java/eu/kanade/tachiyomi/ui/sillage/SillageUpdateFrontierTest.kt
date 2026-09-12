package eu.kanade.tachiyomi.ui.sillage

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SillageUpdateFrontierTest {
    @Test fun `one pinned title does not hide newer entries on later pages`() {
        val frontier = SillageUpdateFrontier(setOf("pinned", "a", "b"))
        assertFalse(frontier.reached(1, listOf("pinned", "new")))
        assertFalse(frontier.reached(2, listOf("another-new", "a")))
        assertTrue(frontier.reached(3, listOf("b")))
    }

    @Test fun `scan includes a second page even when first page is unchanged`() {
        val frontier = SillageUpdateFrontier(setOf("a"))
        assertFalse(frontier.reached(1, listOf("a")))
        assertTrue(frontier.reached(2, listOf("new")))
    }

    @Test fun `frontier survives a worker restart`() {
        val frontier = SillageUpdateFrontier(setOf("a", "b"), setOf("a"))
        assertTrue(frontier.reached(3, listOf("b")))
    }

    @Test fun `first scan has no prior boundary`() {
        assertFalse(SillageUpdateFrontier(emptySet()).reached(2, listOf("a")))
    }
}
