package eu.kanade.tachiyomi.ui.sillage

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.*
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import java.time.Instant

class AsuraSource : HttpSource() {
    override val id = ID
    override val name = "Asura Scans"
    override val lang = "en"
    override val baseUrl = "https://asurascans.com"
    override val supportsLatest = true
    private val ratings = java.util.concurrent.ConcurrentHashMap<String, Double>()
    fun ratingFor(url: String) = ratings[url]
    override fun headersBuilder() = super.headersBuilder().add("Referer", "$baseUrl/")
    override fun popularMangaRequest(page: Int) = GET("$baseUrl/browse?page=$page", headers)
    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/browse".toHttpUrl().newBuilder().addQueryParameter("page", "$page")
            .addQueryParameter("q", query).build().toString(), headers)
    override fun popularMangaParse(response: Response) = AsuraParser.catalogue(response.asJsoup())
    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)
    override fun searchMangaParse(response: Response) = popularMangaParse(response)
    override fun mangaDetailsParse(response: Response): SManga {
        val doc = response.asJsoup()
        val url = java.net.URI(doc.location()).path
        AsuraParser.rating(doc)?.let { ratings[url] = it }
        return AsuraParser.details(doc)
    }
    override fun chapterListParse(response: Response) = AsuraParser.chapters(response.asJsoup())
    override fun pageListParse(response: Response) = AsuraParser.pages(response.asJsoup())
    override fun imageUrlParse(response: Response): String = error("Image directe attendue")
    companion object { const val ID = 830012340001L }
}

internal object AsuraParser {
    private fun decode(value: JsonElement): JsonElement {
        if (value is JsonObject) return JsonObject(value.mapValues { decode(it.value) })
        if (value !is JsonArray) return value
        val payload = value.getOrNull(1) ?: return JsonNull
        return if (value.firstOrNull()?.jsonPrimitive?.intOrNull == 1)
            JsonArray(payload.jsonArray.map(::decode)) else decode(payload)
    }
    private fun props(doc: Document, component: String): JsonObject {
        val element = doc.selectFirst("astro-island[component-url*=$component]")
            ?: error("Asura : données $component absentes. Réessaie ou ouvre la source dans le navigateur.")
        return decode(Json.parseToJsonElement(element.attr("props"))).jsonObject
    }
    private fun schema(doc: Document) = doc.select("script[type=application/ld+json]").mapNotNull {
        runCatching { Json.parseToJsonElement(it.data()).jsonObject }.getOrNull()
    }.firstOrNull { it["@type"]?.jsonPrimitive?.content == "ComicSeries" }
    private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    fun rating(doc: Document) = schema(doc)?.get("aggregateRating")?.jsonObject?.get("ratingValue")?.jsonPrimitive?.doubleOrNull
    fun catalogue(doc: Document): MangasPage {
        val rows = doc.select("a[href^=/comics/]:has(img)").filter { "/chapter/" !in it.attr("href") }
            .distinctBy { it.attr("href") }.map { element -> SManga.create().apply {
                url = element.attr("href"); title = element.selectFirst("img")!!.attr("alt")
                thumbnail_url = element.selectFirst("img")!!.absUrl("src")
            } }
        check(rows.isNotEmpty() || doc.text().contains("No results", true)) { "Asura : catalogue absent ou indisponible" }
        return MangasPage(rows, doc.selectFirst("a[aria-label=Next page]") != null)
    }
    fun details(doc: Document) = SManga.create().apply {
        val data = schema(doc)
        title = data?.text("name")?.takeIf { it.isNotBlank() } ?: doc.selectFirst("h1")?.text().orEmpty()
        check(title.isNotBlank()) { "Asura : fiche indisponible" }
        thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
        description = data?.text("description") ?: doc.selectFirst("meta[name=description]")?.attr("content")
        genre = data?.get("genre")?.jsonArray?.joinToString { it.jsonPrimitive.content }
        author = data?.get("author")?.jsonObject?.text("name")
        artist = data?.get("illustrator")?.jsonObject?.text("name")
        status = if (doc.text().contains("Status completed", true)) SManga.COMPLETED else SManga.UNKNOWN
        initialized = true
    }
    fun chapters(doc: Document): List<SChapter> {
        val base = java.net.URI(doc.location()).path.trimEnd('/')
        return props(doc, "ChapterList")["chapters"]!!.jsonArray.mapNotNull { value ->
            val row = value.jsonObject
            val until = runCatching { Instant.parse(row.text("early_access_until")) }.getOrNull()
            if (row["is_premium"]?.jsonPrimitive?.booleanOrNull == true || (until != null && until.isAfter(Instant.now()))) return@mapNotNull null
            SChapter.create().apply {
                val number = row.text("number")
                url = "$base/chapter/$number"; name = "Chapitre $number" + row.text("title").takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
                chapter_number = number.toFloatOrNull() ?: -1f
                date_upload = runCatching { Instant.parse(row.text("published_at")).toEpochMilli() }.getOrDefault(0)
            }
        }
    }
    fun pages(doc: Document): List<Page> = props(doc, "ChapterReader")["pages"]?.jsonArray.orEmpty()
        .mapIndexed { index, value -> Page(index, imageUrl = value.jsonObject.text("url")) }
        .also { check(it.isNotEmpty()) { "Asura : ce chapitre est indisponible ou réservé aux abonnés" } }
}
