package eu.kanade.tachiyomi.ui.sillage

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import eu.kanade.tachiyomi.source.model.SManga

class SillageJapscanTest {
    @Test fun `catalogue reads French cards and pagination`() {
        val doc = Jsoup.parse("""
            <ul class="mangas-list"><li class="manga-block"><a href="/manga/one-piece"><img data-src="/img/one.jpg">One Piece</a></li></ul>
            <ul class="pagination"><li>1</li><li>2</li></ul>
        """, "https://japscan.st/mangas/")
        val page = JapscanParser.catalogue(doc)
        assertEquals("One Piece", page.mangas.single().title)
        assertEquals("/manga/one-piece", page.mangas.single().url)
        assertTrue(page.hasNextPage)
    }
    @Test fun `details keeps author genre synopsis and status`() {
        val doc = Jsoup.parse("""
            <main id="main"><div class="card-body"><h1>One Piece</h1><img src="/img/one.jpg">
            <p>Auteur(s): Eiichiro Oda</p><p>Artiste(s): Eiichiro Oda</p><p>Genre(s): Aventure, Action</p>
            <div>Synopsis</div><p>Une grande aventure.</p><p>Statut: En cours</p></div></main>
        """, "https://japscan.st/manga/one-piece")
        val manga = JapscanParser.details(doc)
        assertEquals("Eiichiro Oda", manga.author)
        assertEquals("Aventure, Action", manga.genre)
        assertEquals("Une grande aventure.", manga.description)
        assertEquals(SManga.ONGOING, manga.status)
    }
    @Test fun `chapter parser keeps decimal chapters and removes duplicate urls`() {
        val doc = Jsoup.parse("""
            <div id="list_chapters"><div class="list_chapters"><a href="/manga/one-piece/30">Chapitre 30</a><a href="/manga/one-piece/30">Chapitre 30</a><a href="/manga/one-piece/30.5">Chapitre 30.5</a></div></div>
        """, "https://japscan.st/manga/one-piece")
        val chapters = JapscanParser.chapters(doc)
        assertEquals(listOf(30f, 30.5f), chapters.map { it.chapter_number })
    }
    @Test fun `page parser accepts direct images and rejects an empty or challenge page`() {
        val doc = Jsoup.parse("<div class='chapter-content'><img src='https://cdn.japscan.st/1.jpg'><img src='https://cdn.japscan.st/2.jpg'></div>")
        assertEquals(listOf("https://cdn.japscan.st/1.jpg", "https://cdn.japscan.st/2.jpg"), JapscanParser.pages(doc).map { it.imageUrl })
        assertThrows(IllegalStateException::class.java) { JapscanParser.pages(Jsoup.parse("<html>Cloudflare</html>")) }
    }
}
