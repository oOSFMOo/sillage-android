package eu.kanade.tachiyomi.ui.sillage

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SillageCatalogueTest {
    @Test
    fun `an unavailable detail page cannot erase metadata saved by an earlier import`() {
        val known = edition("1", 120, 8.5).copy(cover = "https://example.org/cover.jpg")
        val incomplete = known.copy(cover = "", genres = emptyList(), chapters = null, rating = null)
        assertEquals(known, incomplete.preservingKnownFields(known))
    }

    @Test
    fun `the shipped catalogue can be decoded by the Android model`() {
        val file = java.io.File("src/main/assets/sillage-catalogue.bin")
        val json = java.util.zip.GZIPInputStream(file.inputStream()).bufferedReader().use { it.readText() }
        val document = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<CatalogueDocument>(json)
        assertEquals(81527, document.series.size)
        assertTrue(document.series.any { it.editions.size > 1 })
        assertTrue(document.series.all { it.sourceId.toLongOrNull() != null && it.url.isNotBlank() })
    }

    private fun edition(source: String, count: Int, rating: Double? = null) = CatalogueSeries(
        title = "Une aventure", sourceId = source, url = "/series/1", genres = listOf("Martial Arts"),
        chapters = count, rating = rating, sourceName = source,
    )

    @Test
    fun `merging chooses the most complete edition and averages known ratings`() {
        val result = SillageCatalogue.merge(listOf(edition("1", 60, 8.0)), listOf(edition("2", 100, 6.0))).single()
        assertEquals("2", result.sourceId)
        assertEquals(7.0, result.rating)
        assertEquals(2, result.editions.size)
    }

    @Test
    fun `updating a known edition does not create a clone or erase metadata`() {
        val before = edition("1", 60, 8.0)
        val result = SillageCatalogue.merge(listOf(before), listOf(before.copy(chapters = 61, rating = null, genres = emptyList()))).single()
        assertEquals(61, result.chapters)
        assertEquals(8.0, result.rating)
        assertEquals(before.genres, result.genres)
        assertEquals(1, result.editions.size)
    }

    @Test
    fun `chapter threshold excludes unknown counts and genre aliases are searchable`() = runTest {
        val rows = listOf(edition("1", 60), edition("2", 20), edition("3", 0).copy(chapters = null))
        val index = CatalogueIndex(CatalogueDocument(1, "", rows), rows.map { "une aventure" }, rows.map { setOf("martial arts") })
        val filtered = SillageCatalogue.filter(index, "aventure", "Arts martiaux", 50, CatalogueSort.CHAPTERS)
        assertEquals(listOf("1"), filtered.map { it.sourceId })
        assertTrue(SillageCatalogue.filter(index, "absent", "", 0, CatalogueSort.TITLE).isEmpty())
    }
}
