package eu.kanade.tachiyomi.ui.sillage

import tachiyomi.domain.chapter.model.Chapter

internal fun travelChapters(ordered: List<Chapter>, resumeId: Long?, count: Int): List<Chapter> {
    require(count in 1..500)
    val position = ordered.indexOfFirst { it.id == resumeId }
    if (position < 0) return emptyList()
    return ordered.drop(position).filter { !it.read }.take(count)
}
