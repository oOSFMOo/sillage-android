package eu.kanade.tachiyomi.util.chapter

import tachiyomi.domain.chapter.model.Chapter

/**
 * Returns a copy of the list with duplicate chapters removed
 */
fun List<Chapter>.removeDuplicates(currentChapter: Chapter): List<Chapter> {
    return groupBy {
        // Unknown numbers must never collapse all specials into one chapter.
        if (it.chapterNumber.isFinite() && it.chapterNumber >= 0) "number:${it.chapterNumber}" else "id:${it.id}"
    }
        .map { (_, chapters) ->
            chapters.find { it.id == currentChapter.id }
                ?: chapters.find { it.scanlator == currentChapter.scanlator }
                ?: chapters.first()
        }
}
