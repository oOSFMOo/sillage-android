package eu.kanade.tachiyomi.data.coil

import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.time.Instant

internal object ImportedCoverResolver {
    private val mangaRepository: MangaRepository by injectLazy()
    private val coordinator = CoverRepairCoordinator()

    suspend fun resolve(mangaId: Long, source: HttpSource): RepairedCover = withIOContext {
        var manga: Manga? = null
        coordinator.resolve(
            mangaId = mangaId,
            readCached = {
                manga = mangaRepository.getMangaById(mangaId)
                val latest = checkNotNull(manga)
                latest.thumbnailUrl?.takeUnless(::needsCoverRepair)?.let {
                    RepairedCover(it, latest.coverLastModified)
                }
            },
            repair = {
                val latest = checkNotNull(manga)
                if (latest.source != source.id) throw IOException("La source de cette série a changé.")
                val details = source.getMangaUpdate(
                    manga = latest.toSManga(),
                    chapters = emptyList(),
                    fetchDetails = true,
                    fetchChapters = false,
                ).manga
                val coverUrl = details.thumbnail_url?.takeIf {
                    it.toHttpUrlOrNull() != null && !needsCoverRepair(it)
                } ?: throw IOException("La source n'a pas fourni de couverture.")
                val lastModified = Instant.now().toEpochMilli()
                // Never alter categories, favourites, chapter lists, or reading progress here.
                val saved = mangaRepository.update(
                    MangaUpdate(id = mangaId, thumbnailUrl = coverUrl, coverLastModified = lastModified),
                )
                if (!saved) throw IOException("La couverture n'a pas pu être enregistrée.")
                RepairedCover(coverUrl, lastModified)
            },
        )
    }
}
