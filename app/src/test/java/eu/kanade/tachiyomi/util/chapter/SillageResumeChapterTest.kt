package eu.kanade.tachiyomi.util.chapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.History
import java.util.Date

class SillageResumeChapterTest {
    private fun chapter(number: Long, read: Boolean = false, page: Long = 0) =
        Chapter.create().copy(id = number, mangaId = 1, chapterNumber = number.toDouble(), read = read, lastPageRead = page)
    private fun history(id: Long, time: Long = 10) = History(id, id, Date(time), 100)

    @Test fun `resume chapter 30 at its saved page instead of unread chapter 1`() {
        val rows = listOf(chapter(1), chapter(29), chapter(30, page = 12), chapter(31))
        val result = chooseResumeChapter(rows, listOf(history(30)))!!
        assertEquals(30L, result.id)
        assertEquals(12L, result.lastPageRead)
    }
    @Test fun `a completed chapter 30 continues at 31 despite earlier unread chapters`() {
        assertEquals(31L, chooseResumeChapter(listOf(chapter(1), chapter(30, true), chapter(31)), listOf(history(30)))!!.id)
    }
    @Test fun `no later chapter keeps the last reading location`() {
        assertEquals(30L, chooseResumeChapter(listOf(chapter(1), chapter(30, true)), listOf(history(30)))!!.id)
    }
    @Test fun `the most recent visit wins even when rereading an earlier chapter`() {
        assertEquals(5L, chooseResumeChapter(listOf(chapter(1), chapter(5, page = 2), chapter(30)), listOf(history(30, 1), history(5, 2)))!!.id)
    }
    @Test fun `saved chapter progress still works after clearing history`() {
        assertEquals(30L, chooseResumeChapter(listOf(chapter(1), chapter(30, page = 4)), emptyList())!!.id)
    }
    @Test fun `a new series starts at its first unread chapter`() {
        assertEquals(1L, chooseResumeChapter(listOf(chapter(1), chapter(30)), emptyList())!!.id)
    }
}
