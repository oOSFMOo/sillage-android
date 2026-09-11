package eu.kanade.tachiyomi.util.chapter

import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Reading history takes precedence over unread chapters skipped at the beginning. */
suspend fun List<Chapter>.getResumeChapter(manga: Manga): Chapter? {
    val repository = Injekt.get<HistoryRepository>()
    val history = map { it.mangaId }.distinct().flatMap { repository.getHistoryByMangaId(it) }
    return chooseResumeChapter(sortedWith(getChapterSort(manga, sortDescending = false)), history)
}

internal fun chooseResumeChapter(ordered: List<Chapter>, history: List<History>): Chapter? {
    val positions = ordered.withIndex().associate { it.value.id to it.index }
    val lastVisit = history.filter { it.readAt != null && it.chapterId in positions }.maxByOrNull { it.readAt!!.time }
    val anchor = lastVisit?.let { positions[it.chapterId] }
        ?: ordered.indexOfLast { it.lastPageRead > 0 || it.read }
    if (anchor < 0) return ordered.firstOrNull { !it.read }
    val chapter = ordered[anchor]
    if (!chapter.read) return chapter
    return ordered.drop(anchor + 1).firstOrNull { !it.read } ?: chapter
}
