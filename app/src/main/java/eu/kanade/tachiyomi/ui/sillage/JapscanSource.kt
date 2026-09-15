package eu.kanade.tachiyomi.ui.sillage

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Response
import org.jsoup.nodes.Element

/** Japscan French source. The site may require its Cloudflare/WebView check before reading. */
class JapscanSource : HttpSource() {
    override val id = ID
    override val name = "Japscan (VF)"
    override val lang = "fr"
    override val baseUrl = "https://japscan.st"
    override val supportsLatest = true

    override fun headersBuilder() = super.headersBuilder().add("Referer", "$baseUrl/")
    override fun popularMangaRequest(page: Int) = GET("$baseUrl/mangas/?sort=popular&p=$page", headers)
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/mangas/?sort=updated&p=$page", headers)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        if (query.isBlank()) popularMangaRequest(page) else POST(
            "$baseUrl/ls/", headers.newBuilder().add("X-Requested-With", "XMLHttpRequest").build(),
            FormBody.Builder().add("search", query).build(),
        )

    override fun popularMangaParse(response: Response) = JapscanParser.catalogue(response.asJsoup())
    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)
    override fun searchMangaParse(response: Response): MangasPage {
        if (response.request.url.encodedPath == "/ls/") {
            val json = kotlinx.serialization.json.Json.parseToJsonElement(response.body.string()).jsonArray
            return MangasPage(json.mapNotNull { it as? JsonObject }.map { JapscanParser.searchJson(it) }, false)
        }
        return popularMangaParse(response)
    }
    override fun mangaDetailsParse(response: Response) = JapscanParser.details(response.asJsoup())
    override fun chapterListParse(response: Response) = JapscanParser.chapters(response.asJsoup())
    override fun pageListParse(response: Response) = JapscanParser.pages(response.asJsoup())
    override fun imageUrlParse(response: Response): String = error("Japscan : ouvre la source dans WebView pour résoudre la vérification anti-bot")

    companion object { const val ID = 830012340002L }
}

internal object JapscanParser {
    fun catalogue(doc: org.jsoup.nodes.Document): MangasPage {
        val rows = doc.select(".mangas-list .manga-block").mapNotNull { block ->
            val link = block.selectFirst("a[href]") ?: return@mapNotNull null
            SManga.create().apply {
                url = link.attr("href")
                title = link.text().trim()
                thumbnail_url = block.selectFirst("img")?.absUrl("data-src")?.ifBlank { block.selectFirst("img")?.absUrl("src") }
            }
        }.filter { it.title.isNotBlank() }
        check(rows.isNotEmpty() || doc.text().contains("aucun résultat", true)) { "Japscan : catalogue indisponible ou vérification Cloudflare" }
        return MangasPage(rows, doc.selectFirst(".pagination > li:last-child:not(.disabled)") != null)
    }
    fun searchJson(json: JsonObject) = SManga.create().apply {
        url = json["url"]?.jsonPrimitive?.content.orEmpty()
        title = json["name"]?.jsonPrimitive?.content.orEmpty()
        thumbnail_url = json["image"]?.jsonPrimitive?.content?.let { if (it.startsWith("http")) it else "${JapscanSource().baseUrl}$it" }
    }
    fun details(doc: org.jsoup.nodes.Document) = SManga.create().apply {
        thumbnail_url = doc.selectFirst("#main .card-body img")?.absUrl("src")
        val info = doc.selectFirst("#main .card-body") ?: error("Japscan : fiche indisponible")
        title = info.selectFirst("h1,h2")?.text()?.trim() ?: doc.title()
        author = info.select("p").firstOrNull { it.text().contains("Auteur") }?.text()?.substringAfter(":")?.trim()
        artist = info.select("p").firstOrNull { it.text().contains("Artiste") }?.text()?.substringAfter(":")?.trim()
        genre = info.select("p").firstOrNull { it.text().contains("Genre") }?.text()?.substringAfter(":")?.trim()
        description = info.selectFirst("div:contains(Synopsis) + p")?.text()?.trim()
        status = when { info.text().contains("en cours", true) -> SManga.ONGOING; info.text().contains("terminé", true) -> SManga.COMPLETED; else -> SManga.UNKNOWN }
    }
    fun chapters(doc: org.jsoup.nodes.Document): List<SChapter> = doc.select("#list_chapters .list_chapters").flatMap { row ->
        row.select("a[href]").mapNotNull { link ->
            val number = Regex("(?i)chapitre\\s*([0-9]+(?:\\.[0-9]+)?)").find(link.text())?.groupValues?.get(1)
                ?: Regex("([0-9]+(?:\\.[0-9]+)?)").find(link.text())?.groupValues?.get(1)
                ?: return@mapNotNull null
            SChapter.create().apply { url = link.attr("href"); name = link.text().trim(); chapter_number = number.toFloat(); date_upload = 0L }
        }
    }.distinctBy { it.url }
    fun pages(doc: org.jsoup.nodes.Document): List<Page> {
        val direct = doc.select(".chapter-content img, #chapter img, .reader img").mapIndexedNotNull { index, image ->
            image.absUrl("src").takeIf { it.startsWith("http") }?.let { Page(index, imageUrl = it) }
        }
        check(direct.isNotEmpty()) { "Japscan : résous la vérification Cloudflare dans WebView puis réouvre le chapitre" }
        return direct
    }
}
