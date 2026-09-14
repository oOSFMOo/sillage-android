package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.util.chapter.removeDuplicates
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class SillagePreloadPolicyTest {
    private fun rows() = (1L..60L).map { Chapter.create().copy(id = it, chapterNumber = it.toDouble(), mangaId = 1) }
    @Test fun `chapter 30 is loaded first followed by ten chapters in order`() {
        assertEquals((30L..40L).toList(), preloadWindow(rows(), 30).map { it.id })
    }
    @Test fun `advancing slides the window to chapter 41`() {
        assertEquals((31L..41L).toList(), preloadWindow(rows(), 31).map { it.id })
    }
    @Test fun `lower counts and disabled preload are respected`() {
        for (count in listOf(1, 3, 5, 10)) assertEquals(count + 1, preloadWindow(rows(), 30, count).size)
        assertTrue(preloadWindow(rows(), 30, 0).isEmpty())
    }
    @Test fun `read chapters are skipped without losing ten unread ahead`() {
        val chapters = rows().map { if (it.id == 31L) it.copy(read = true) else it }
        assertEquals(listOf(30L) + (32L..41L).toList(), preloadWindow(chapters, 30).map { it.id })
    }
    @Test fun `end of series and missing anchor never wrap to beginning`() {
        assertEquals(listOf(60L), preloadWindow(rows(), 60).map { it.id })
        assertTrue(preloadWindow(rows(), 99).isEmpty())
    }
    @Test fun `expiry starts at two hours and handles clock rollback`() {
        assertFalse(preloadExpired(100, 100 + PRELOAD_EXPIRY_MS - 1))
        assertTrue(preloadExpired(100, 100 + PRELOAD_EXPIRY_MS))
        assertFalse(preloadExpired(100, 50))
        assertFalse(preloadExpired(0, PRELOAD_EXPIRY_MS))
    }
    @Test fun `duplicates preserve selected version and its translation group`() {
        val original = rows().take(2).map { it.copy(scanlator = "Official") }
        val alternate = original.map { it.copy(id = it.id + 10, scanlator = "Unofficial") }
        assertEquals(listOf(11L, 12L), listOf(original[0], alternate[0], original[1], alternate[1])
            .removeDuplicates(alternate[0]).map { it.id })
    }
    @Test fun `fractional and unknown chapter numbers stay separate`() {
        val chapters = rows().take(5).mapIndexed { i, chapter -> chapter.copy(chapterNumber = listOf(30.0, 30.5, -1.0, -1.0, Double.NaN)[i]) }
        assertEquals(5, chapters.removeDuplicates(chapters.first()).size)
    }
}
