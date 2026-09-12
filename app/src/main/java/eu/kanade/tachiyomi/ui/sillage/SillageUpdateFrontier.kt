package eu.kanade.tachiyomi.ui.sillage

/** A single pinned or reordered title must not end a latest-feed scan. */
internal class SillageUpdateFrontier(
    private val previous: Set<String>,
    alreadySeen: Set<String> = emptySet(),
) {
    val seen = alreadySeen.toMutableSet()

    fun reached(page: Int, keys: List<String>): Boolean {
        seen.addAll(keys.filter { it in previous })
        return page >= 2 && previous.isNotEmpty() && seen.containsAll(previous)
    }
}
