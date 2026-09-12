package eu.kanade.tachiyomi.ui.sillage

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** A resumable initial import; subsequent runs read the source's latest feed only. */
class CatalogueImportWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getLong("source", 0)
        val manager = Injekt.get<SourceManager>()
        manager.isInitialized.first { it }
        val source = manager.get(id) ?: return@withContext Result.failure()
        CatalogueStore(applicationContext).use { store ->
            var state = store.state(id)
            if (state.complete && !inputData.getBoolean("latest", false)) return@withContext Result.success()
            if (state.complete && !source.supportsLatest) {
                store.state(id, state.copy(message = "Cette source ne fournit pas de flux de nouveautés. Les séries restent actualisées à l’ouverture."))
                return@withContext Result.success()
            }
            val started = System.currentTimeMillis()
            val checkpoints = applicationContext.getSharedPreferences("sillage-import-checkpoints", Context.MODE_PRIVATE)
            val checkpointKey = "$id-${if (state.complete) "latest" else "initial"}"
            var checked = checkpoints.getInt("$checkpointKey-checked", 0)
            var incomplete = checkpoints.getInt("$checkpointKey-incomplete", 0)
            var page = if (state.complete) checkpoints.getInt("$checkpointKey-page", 1) else state.page
            var firstPage = checkpoints.getStringSet("$checkpointKey-head", emptySet()).orEmpty().toList()
            val previousFrontier = state.frontier.toSet()
            val frontier = SillageUpdateFrontier(previousFrontier,
                checkpoints.getStringSet("$checkpointKey-seen", emptySet()).orEmpty())
            val seenPages = mutableSetOf<List<String>>()
            try {
                while (true) {
                    store.state(id, state.copy(message = "${source.name} · ${if (state.complete) "Nouveautés" else "Import"} · page $page · $checked séries vérifiées"))
                    val result = withTimeout(60_000) {
                        if (state.complete) source.getLatestUpdates(page) else source.getPopularManga(page)
                    }
                    val keys = result.mangas.map { it.url }
                    if (!seenPages.add(keys)) throw IllegalStateException("La source répète la même page. Import conservé ; réessaie plus tard.")
                    if (page == 1) {
                        firstPage = keys
                        checkpoints.edit().putStringSet("$checkpointKey-head", keys.toSet()).apply()
                    }
                    val entries = mutableListOf<CatalogueSeries>()
                    val offset = checkpoints.getInt("$checkpointKey-offset", 0)
                    result.mangas.forEachIndexed { position, manga ->
                        if (position < offset) return@forEachIndexed
                        if (System.currentTimeMillis() - started > 360_000) {
                            store.state(id, state.copy(message = "Reprise en attente · $checked séries vérifiées · page $page"))
                            return@withContext Result.retry()
                        }
                        // Save discovery immediately, even if one detail endpoint is unavailable.
                        var entry = CatalogueSeries(manga.title, id.toString(), manga.url, manga.thumbnail_url,
                            manga.genre?.split(',')?.map(String::trim).orEmpty(), sourceName = source.name)
                        try {
                            delay(700)
                            val detail = withTimeout(12_000) {
                                source.getMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = true)
                            }
                            entry = entry.copy(cover = detail.manga.thumbnail_url ?: entry.cover,
                                genres = detail.manga.genre?.split(',')?.map(String::trim).orEmpty(), chapters = detail.chapters.size, rating = (source as? AsuraSource)?.ratingFor(manga.url))
                            val local = Injekt.get<tachiyomi.domain.manga.repository.MangaRepository>()
                                .getMangaByUrlAndSourceId(manga.url, id)
                            if (local?.favorite == true && detail.chapters.isNotEmpty()) {
                                Injekt.get<eu.kanade.domain.chapter.interactor.SyncChaptersWithSource>()
                                    .await(detail.chapters, local, source)
                            }
                        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                            incomplete += 1
                            // A slow title must not cancel the entire source import.
                        } catch (e: CancellationException) { throw e
                        } catch (_: Exception) { incomplete += 1 }
                        entries.add(entry)
                        // A long page cannot erase already fetched cards if Android stops the worker.
                        store.save(listOf(entry))
                        checked += 1
                        checkpoints.edit().putInt("$checkpointKey-offset", position + 1)
                            .putInt("$checkpointKey-checked", checked).putInt("$checkpointKey-incomplete", incomplete).apply()
                    }
                    SillageCatalogue.invalidate()
                    val reachedFrontier = state.complete && frontier.reached(page, keys)
                    checkpoints.edit().putStringSet("$checkpointKey-seen", frontier.seen.toSet()).apply()
                    val baseline = state.complete && previousFrontier.isEmpty() && page >= 5
                    val recentLimit = state.complete && page >= 10 && !reachedFrontier
                    if (!result.hasNextPage || keys.isEmpty() || reachedFrontier || baseline || recentLimit) {
                        state = state.copy(complete = true, page = 1,
                            frontier = if (state.complete) firstPage else emptyList(),
                            message = "${source.name} · Terminé le ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss"))} · $checked séries vérifiées" +
                                (if (incomplete > 0) " · $incomplete fiches incomplètes (source indisponible)" else "") +
                                (if (recentLimit) " · Vérification partielle : limite de 10 pages récentes atteinte" else "") +
                                (if (baseline) " · 5 pages récentes" else ""))
                        store.state(id, state)
                        applicationContext.getSharedPreferences("sillage-refresh-results", Context.MODE_PRIVATE).edit()
                            .putLong("last-finished", System.currentTimeMillis()).apply()
                        checkpoints.edit().remove("$checkpointKey-page").remove("$checkpointKey-offset").remove("$checkpointKey-head")
                            .remove("$checkpointKey-checked").remove("$checkpointKey-incomplete")
                            .remove("$checkpointKey-seen").apply()
                        return@withContext Result.success()
                    }
                    page += 1
                    checkpoints.edit().putInt("$checkpointKey-page", page).putInt("$checkpointKey-offset", 0).apply()
                    if (!state.complete) {
                        state = state.copy(page = page)
                        store.state(id, state)
                    }
                    if (System.currentTimeMillis() - started > 360_000) {
                        store.state(id, state.copy(message = "Reprise en attente · $checked séries vérifiées · page $page"))
                        return@withContext Result.retry()
                    }
                    delay(1000)
                }
                @Suppress("UNREACHABLE_CODE") Result.success()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                store.state(id, state.copy(message = "${source.name} · Délai dépassé. Import conservé ; utilise Réessayer."))
                Result.failure()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                store.state(id, state.copy(message = "${source.name} · Échec : ${e.message ?: "source indisponible"}. Utilise Réessayer."))
                Result.failure()
            }
        }
    }

    companion object {
        fun enqueue(context: Context, source: Source, latest: Boolean = false) {
            val work = OneTimeWorkRequestBuilder<CatalogueImportWorker>()
                .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 30, java.util.concurrent.TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf("source" to source.id, "latest" to latest))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("sillage-import-${source.id}", ExistingWorkPolicy.KEEP, work)
        }
    }
}
