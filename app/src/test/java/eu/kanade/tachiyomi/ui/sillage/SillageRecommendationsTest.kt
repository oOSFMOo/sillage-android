package eu.kanade.tachiyomi.ui.sillage

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SillageRecommendationsTest {
    private fun row(title: String, genres: List<String>, count: Int? = 100) =
        CatalogueSeries(title, "1", "/$title", genres = genres, chapters = count, sourceName = "Test")
    @Test fun `recommendations exclude the current title and unrelated genres`() {
        val result = similarSeries("A", listOf("Murim"), listOf(row("A", listOf("Murim")), row("B", listOf("Murim")), row("C", listOf("Romance"))))
        assertEquals(listOf("B"), result.map { it.series.title })
        assertEquals(listOf("Murim"), result.single().sharedGenres)
    }
    @Test fun `genre aliases match and unknown chapter counts do not pass a minimum`() {
        val rows = listOf(row("B", listOf("Martial Arts")), row("C", listOf("Arts martiaux"), null))
        assertEquals(listOf("B"), similarSeries("A", listOf("Arts martiaux"), rows, 50).map { it.series.title })
    }
    @Test fun `empty metadata does not produce invented recommendations`() {
        assertTrue(similarSeries("A", emptyList(), listOf(row("B", listOf("Murim")))).isEmpty())
    }
}
