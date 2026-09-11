package eu.kanade.tachiyomi.data.coil

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

/** Only the desktop proxy and missing covers need source metadata repair. Local files remain valid. */
internal fun needsCoverRepair(url: String?): Boolean {
    if (url.isNullOrBlank()) return true
    if (url.startsWith("/api/v1/manga/") && url.substringBefore('?').endsWith("/thumbnail")) return true
    val parsed = url.toHttpUrlOrNull() ?: return false
    if (parsed.encodedPath.startsWith("/api/v1/manga/") && parsed.encodedPath.endsWith("/thumbnail")) return true
    val host = parsed.host
    return host == "localhost" || host == "::1" || host == "0.0.0.0" || host.startsWith("127.")
}

internal data class RepairedCover(val url: String, val lastModified: Long)

/**
 * Runs only inside the requesting image's coroutine: scrolling away cancels queued work.
 * One request at a time, a brief debounce, and a bounded failure cooldown avoid a catalogue crawl.
 */
internal class CoverRepairCoordinator(
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val debounceMillis: Long = 250,
    private val requestSpacingMillis: Long = 800,
    private val retryDelayMillis: Long = 60_000,
) {
    private val mutex = Mutex()
    private val retryAfter = linkedMapOf<Long, Long>()
    private var nextRequestAt: Long? = null

    suspend fun resolve(
        mangaId: Long,
        readCached: suspend () -> RepairedCover?,
        repair: suspend () -> RepairedCover,
    ): RepairedCover {
        delay(debounceMillis)
        return mutex.withLock {
            // Another visible item, or a manual refresh, may already have repaired the same manga.
            readCached()?.let { return@withLock it }
            val now = nowMillis()
            if ((retryAfter[mangaId] ?: Long.MIN_VALUE) > now) {
                throw IOException("Couverture indisponible, nouvelle tentative possible dans un instant.")
            }
            delay(nextRequestAt?.let { (it - now).coerceAtLeast(0) } ?: 0)
            nextRequestAt = nowMillis() + requestSpacingMillis
            try {
                repair().also { retryAfter.remove(mangaId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                retryAfter[mangaId] = nowMillis() + retryDelayMillis
                if (retryAfter.size > 256) retryAfter.remove(retryAfter.keys.first())
                throw e
            }
        }
    }
}
