package eu.kanade.tachiyomi.ui.sillage

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class SillageTravelTest {
    private fun rows() = (1L..200L).map { Chapter.create().copy(id = it, mangaId = 1) }
    @Test fun `a flight download starts at chapter 30 and contains 100 chapters`() {
        val result = travelChapters(rows(), 30, 100)
        assertEquals(100, result.size)
        assertEquals(30L, result.first().id)
        assertEquals(129L, result.last().id)
    }
    @Test fun `read chapters are not downloaded again`() {
        val result = travelChapters(rows().map { if (it.id == 31L) it.copy(read = true) else it }, 30, 2)
        assertEquals(listOf(30L, 32L), result.map { it.id })
    }
    @Test fun `unknown resume point does not restart from chapter one`() {
        assertTrue(travelChapters(rows(), null, 5).isEmpty())
    }
    @Test fun `a request cannot exceed the displayed maximum`() {
        assertThrows(IllegalArgumentException::class.java) { travelChapters(rows(), 1, 501) }
    }
}
