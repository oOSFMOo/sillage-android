package eu.kanade.tachiyomi.ui.sillage

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SillageAsuraTest {
    private fun doc(name: String, path: String) = Jsoup.parse(
        javaClass.getResourceAsStream("/sillage/asura/$name.html")!!.bufferedReader().use { it.readText() },
        "https://asurascans.com$path")
    @Test fun `public catalogue exposes titles covers and pagination`() {
        val result = AsuraParser.catalogue(doc("browse", "/browse?page=1"))
        assertTrue(result.mangas.size >= 10)
        assertTrue(result.hasNextPage)
        assertTrue(result.mangas.all { it.title.isNotBlank() && it.thumbnail_url!!.startsWith("https://cdn.asurascans.com/") })
    }
    @Test fun `public series provides genres rating and title`() {
        val doc = doc("detail", "/comics/nano-machine-53fc8424")
        val result = AsuraParser.details(doc)
        assertEquals("Nano Machine", result.title)
        assertTrue(result.genre!!.contains("Murim"))
        assertNotNull(AsuraParser.rating(doc))
    }
    @Test fun `chapter list contains real dates and reader paths`() {
        val result = AsuraParser.chapters(doc("detail", "/comics/nano-machine-53fc8424"))
        assertTrue(result.size >= 300)
        assertTrue(result.all { it.date_upload > 0 && it.url.startsWith("/comics/nano-machine-53fc8424/chapter/") })
    }
    @Test fun `free reader exposes images in order`() {
        val pages = AsuraParser.pages(doc("chapter", "/comics/nano-machine-53fc8424/chapter/1"))
        assertTrue(pages.size > 5)
        assertTrue(pages.first().imageUrl!!.contains("001.webp"))
        assertEquals(pages.size, pages.map { it.imageUrl }.distinct().size)
    }
    @Test fun `missing reader data gives an error rather than empty success`() {
        assertThrows(IllegalStateException::class.java) { AsuraParser.pages(Jsoup.parse("<html>Unavailable</html>")) }
    }
}
