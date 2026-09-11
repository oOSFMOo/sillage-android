package eu.kanade.tachiyomi.ui.sillage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import tachiyomi.domain.manga.model.Manga
import java.text.Normalizer
import java.util.Locale
import java.util.zip.GZIPInputStream

/** The discovery index is deliberately separate from the reader's library database. */
@Serializable
internal data class CatalogueDocument(
    val version: Int,
    val generatedAt: String,
    val series: List<CatalogueSeries>,
)

@Serializable
internal data class CatalogueSeries(
    val title: String,
    val sourceId: String,
    val url: String,
    val cover: String? = null,
    val genres: List<String> = emptyList(),
    val chapters: Int? = null,
    val rating: Double? = null,
    val sourceName: String,
    val editions: List<CatalogueSeries> = emptyList(),
) {
    val key: String get() = "$sourceId:$url"

    fun preservingKnownFields(previous: CatalogueSeries?): CatalogueSeries = copy(
        cover = cover?.takeIf { it.isNotBlank() } ?: previous?.cover,
        genres = genres.ifEmpty { previous?.genres.orEmpty() },
        chapters = chapters ?: previous?.chapters,
        rating = rating ?: previous?.rating,
    )

    fun toManga(): Manga = Manga.create().copy(
        source = sourceId.toLong(),
        url = url,
        ogTitle = title,
        ogThumbnailUrl = cover?.takeIf { it.startsWith("https://") || it.startsWith("http://") },
        ogGenre = genres,
        initialized = false,
    )
}

internal enum class CatalogueSort(val label: String) {
    CHAPTERS("Plus de chapitres"),
    RATING("Meilleures notes"),
    TITLE("Titre A → Z"),
}

internal data class CatalogueIndex(
    val document: CatalogueDocument,
    val searchKeys: List<String>,
    val genreKeys: List<Set<String>>,
)

internal object SillageCatalogue {
    private val mutex = Mutex()
    private var cached: CatalogueIndex? = null
    val revision = kotlinx.coroutines.flow.MutableStateFlow(0)
    fun invalidate() { cached = null; revision.value += 1 }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun load(context: Context): CatalogueIndex = withContext(Dispatchers.IO) {
        mutex.withLock {
            cached ?: run {
                val document = context.assets.open("sillage-catalogue.bin").use { raw ->
                    GZIPInputStream(raw).use { stream ->
                        Json { ignoreUnknownKeys = true }.decodeFromStream<CatalogueDocument>(stream)
                    }
                }
                check(document.version == 1) { "Version de catalogue non prise en charge" }
                val combined = CatalogueStore(context).use { store -> merge(document.series, store.entries()) }
                CatalogueIndex(
                    document = document.copy(series = combined),
                    searchKeys = combined.map { normalize(it.title) },
                    genreKeys = combined.map { series -> series.genres.map(::normalize).toSet() },
                ).also { cached = it }
            }
        }
    }

    internal fun merge(base: List<CatalogueSeries>, updates: List<CatalogueSeries>): List<CatalogueSeries> {
        val groups = linkedMapOf<String, MutableMap<String, CatalogueSeries>>()
        val knownGroups = mutableMapOf<String, String>()
        base.forEach { item ->
            val group = normalize(item.title)
            val editions = groups.getOrPut(group) { linkedMapOf() }
            (item.editions.ifEmpty { listOf(item.copy(editions = emptyList())) }).forEach {
                editions[it.key] = it
                knownGroups[it.key] = group
            }
        }
        updates.forEach { item ->
            val group = knownGroups[item.key] ?: normalize(item.title)
            val editions = groups.getOrPut(group) { linkedMapOf() }
            val previous = editions[item.key]
            editions[item.key] = item.copy(
                rating = item.rating ?: previous?.rating,
                genres = item.genres.ifEmpty { previous?.genres.orEmpty() },
                chapters = item.chapters ?: previous?.chapters,
                cover = item.cover ?: previous?.cover,
            )
        }
        return groups.values.map { editions ->
            val sorted = editions.values.sortedByDescending { it.chapters ?: -1 }
            val ratings = sorted.mapNotNull { it.rating }
            sorted.first().copy(
                genres = sorted.flatMap { it.genres }.distinct(),
                rating = ratings.takeIf { it.isNotEmpty() }?.average(),
                editions = sorted.map { it.copy(editions = emptyList()) },
            )
        }
    }

    suspend fun filter(
        index: CatalogueIndex,
        query: String,
        genre: String,
        minimumChapters: Int,
        sort: CatalogueSort,
    ): List<CatalogueSeries> = withContext(Dispatchers.Default) {
        val words = normalize(query).split(' ').filter(String::isNotBlank)
        val normalizedGenre = normalize(genre)
        val candidates = index.document.series.filterIndexed { position, series ->
            words.all { it in index.searchKeys[position] } &&
                (genre.isBlank() || index.genreKeys[position].any { matchesGenre(it, normalizedGenre) }) &&
                (minimumChapters == 0 || (series.chapters ?: -1) >= minimumChapters)
        }
        when (sort) {
            CatalogueSort.CHAPTERS -> candidates.sortedWith(
                compareByDescending<CatalogueSeries> { it.chapters ?: -1 }.thenBy { it.title.lowercase(Locale.ROOT) },
            )
            CatalogueSort.RATING -> candidates.sortedWith(
                compareByDescending<CatalogueSeries> { it.rating ?: -1.0 }.thenByDescending { it.chapters ?: -1 },
            )
            CatalogueSort.TITLE -> candidates.sortedBy { it.title.lowercase(Locale.ROOT) }
        }
    }

    private fun matchesGenre(value: String, genre: String): Boolean = when (genre) {
        "arts martiaux" -> value in setOf("arts martiaux", "art martial", "martial arts", "art martiaux")
        "aventure" -> value in setOf("aventure", "aventures", "adventure")
        else -> value == genre
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .trim()
}
