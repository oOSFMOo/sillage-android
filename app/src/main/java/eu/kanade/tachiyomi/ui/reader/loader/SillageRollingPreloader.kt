package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.util.DataSaver
import exh.util.DataSaver.Companion.getImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** One cancellable image request at a time. Foreground reader demand preempts this job. */
internal class SillageRollingPreloader(context: Context, scope: CoroutineScope) {
    private data class Plan(val manga: Manga, val source: HttpSource, val chapters: List<Chapter>, val currentId: Long)
    private val plan = MutableStateFlow<Plan?>(null)
    private val cache = SillagePreloadCache(context)
    private val readerCache: ChapterCache = Injekt.get()
    private val downloads: DownloadManager = Injekt.get()
    private val preferences: ReaderPreferences = Injekt.get()
    val status = MutableStateFlow("Préchargement : en attente de lecture")

    private val work = scope.launch(Dispatchers.IO) {
            preloadSelection(plan, SillagePreloadCache.foreground, SillagePreloadCache.readerRequests,
                preferences.rollingPreload.changes()).collectLatest { selection ->
                if (selection == null) {
                    status.value = "Préchargement en pause"
                    return@collectLatest
                }
                val (target, count) = selection
                cache.expire()
                cache.heartbeat()
                val window = preloadWindow(target.chapters, target.currentId, count)
                cache.removeRead(target.chapters.filter { it.read }.map { it.id }.toSet(), target.currentId)
                val saver = DataSaver(target.source, Injekt.get<SourcePreferences>())
                while (true) {
                    // Let the visible page establish its requests before spending bandwidth ahead.
                    delay(750)
                    var ready = 0
                    try {
                        for (chapter in window) {
                            cache.heartbeat()
                            if (!downloads.isChapterDownloaded(chapter.name, chapter.scanlator, chapter.url, target.manga.ogTitle, target.manga.source)) {
                                val pages = cache.pages(chapter.id) ?: runCatching {
                                    readerCache.getPageListFromCache(chapter)
                                }.getOrNull() ?: withTimeout(30_000) { target.source.getPageList(chapter.toSChapter()) }
                                check(pages.isNotEmpty())
                                cache.savePages(chapter.id, pages)
                                for (page in pages) {
                                    withTimeout(30_000) {
                                        if (page.imageUrl.isNullOrEmpty()) page.imageUrl = target.source.getImageUrl(page)
                                        val url = requireNotNull(page.imageUrl)
                                        if (cache.image(chapter.id, url) == null && !readerCache.isImageInCache(url)) {
                                            if (!cache.hasSpace()) error("Limite du cache temporaire atteinte (1 Go maximum)")
                                            cache.saveImage(chapter.id, url, target.source.getImage(page, dataSaver = saver))
                                        }
                                        cache.savePages(chapter.id, pages)
                                    }
                                }
                            }
                            if (chapter.id != target.currentId) ready++
                            status.value = "$ready/${window.size - 1} chapitres prêts en avance · cache temporaire"
                        }
                        // Revalidate files periodically: Android or the normal page cache may evict them.
                        delay(30_000)
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        status.value = "$ready/${window.size - 1} prêts · réseau lent, nouvel essai bientôt"
                        delay(15_000)
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) {
                        status.value = "$ready/${window.size - 1} prêts · en attente du réseau, de la source ou d’espace"
                        delay(15_000)
                    }
                }
            }
    }

    fun release(readIds: Set<Long>) {
        work.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            work.join()
            cache.removeRead(readIds, -1)
        }
    }

    fun follow(manga: Manga, source: HttpSource, chapters: List<Chapter>, currentId: Long) {
        if (plan.value?.currentId != currentId || plan.value?.manga?.id != manga.id) {
            plan.value = Plan(manga, source, chapters, currentId)
        }
    }
}
