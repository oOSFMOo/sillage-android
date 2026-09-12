package eu.kanade.tachiyomi.ui.sillage

internal data class SimilarSeries(val series: CatalogueSeries, val sharedGenres: List<String>, val score: Double)

internal fun similarSeries(title: String, genres: List<String>, rows: List<CatalogueSeries>, minimum: Int = 0): List<SimilarSeries> {
    fun tags(values: List<String>) = values.map(SillageCatalogue::canonicalGenre).map { it.lowercase() }.toSet()
    val wanted = tags(genres)
    if (wanted.isEmpty()) return emptyList()
    val frequencies = rows.flatMap { tags(it.genres) }.groupingBy { it }.eachCount()
    return rows.filter { !it.title.equals(title, true) && (minimum == 0 || (it.chapters ?: -1) >= minimum) }
        .mapNotNull { row ->
            val common = tags(row.genres).intersect(wanted)
            if (common.isEmpty()) null else SimilarSeries(row,
                row.genres.map(SillageCatalogue::canonicalGenre).distinct().filter { it.lowercase() in common },
                common.sumOf { 1.0 / kotlin.math.sqrt((frequencies[it] ?: 1).toDouble()) } / kotlin.math.sqrt(tags(row.genres).size.toDouble()))
        }.sortedWith(compareByDescending<SimilarSeries> { it.score }.thenByDescending { it.series.chapters ?: -1 })
        .distinctBy { it.series.title.lowercase() }.take(12)
}
