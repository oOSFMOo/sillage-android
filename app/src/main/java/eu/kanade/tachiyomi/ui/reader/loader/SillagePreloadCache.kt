package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Response

/** Only owns temporary reader files. Never touches DownloadManager's directories. */
internal class SillagePreloadCache(context: Context) {
    private val root = File(context.cacheDir, "sillage-rolling-preload").apply { mkdirs() }
    private val preferences = context.getSharedPreferences("sillage-preload", Context.MODE_PRIVATE)
    private fun directory(id: Long) = File(root, id.toString())
    fun image(id: Long, url: String): File? = File(directory(id), DiskUtil.hashKeyForDisk(url))
        .takeIf { !isExpired() && it.isFile && it.length() > 0 }
    fun pages(id: Long): List<Page>? = runCatching {
        check(!isExpired())
        Json.decodeFromString<List<Page>>(File(directory(id), "pages.json").readText())
    }.getOrNull()

    fun savePages(id: Long, pages: List<Page>) = synchronized(lock) {
        val directory = directory(id).apply { mkdirs() }
        val temporary = File(directory, "pages.tmp")
        temporary.writeText(Json.encodeToString(pages))
        check(temporary.renameTo(File(directory, "pages.json")))
    }

    suspend fun saveImage(id: Long, url: String, response: Response) = kotlinx.coroutines.coroutineScope {
        // Cancel an in-flight body read too, not just the HTTP request before headers arrive.
        val closer = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try { kotlinx.coroutines.awaitCancellation() } finally { response.close() }
        }
        try {
        response.use {
            val directory = directory(id).apply { mkdirs() }
            val target = File(directory, DiskUtil.hashKeyForDisk(url))
            val temporary = File(directory, target.name + ".part")
            try {
                val used = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                it.body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        val bytes = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(bytes)
                            if (count < 0) break
                            output.write(bytes, 0, count)
                            written += count
                            if (used + written > 1024L * 1024 * 1024 || written > 64L * 1024 * 1024 || root.usableSpace < 256L * 1024 * 1024) {
                                throw IOException("Espace insuffisant pour le préchargement")
                            }
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                check(temporary.length() > 0 && temporary.renameTo(target))
            } finally { temporary.delete() }
        }
        } finally { closer.cancel() }
    }

    fun invalidateImage(id: Long, url: String) { File(directory(id), DiskUtil.hashKeyForDisk(url)).delete() }

    fun hasSpace(): Boolean = root.usableSpace > 320L * 1024 * 1024 &&
        root.walkTopDown().filter { it.isFile }.sumOf { it.length() } < 1024L * 1024 * 1024

    fun removeRead(ids: Set<Long>, currentId: Long) = synchronized(lock) {
        ids.filter { it != currentId }.forEach { directory(it).deleteRecursively() }
    }

    fun heartbeat() { preferences.edit().putLong("last-active", System.currentTimeMillis()).apply() }
    private fun isExpired() = preloadExpired(preferences.getLong("last-active", 0), System.currentTimeMillis())
    fun expire(): Boolean = synchronized(lock) {
        if (isExpired()) {
            root.listFiles()?.forEach { it.deleteRecursively() }
            true
        } else false
    }

    companion object {
        private val lock = Any()
        private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var startup: kotlinx.coroutines.Job? = null
        val foreground = MutableStateFlow(false)
        val readerRequests = MutableStateFlow(0)
        suspend fun <T> readerRequest(block: suspend () -> T): T {
            readerRequests.update { it + 1 }
            try { return block() } finally { readerRequests.update { it - 1 } }
        }
        fun onStart(context: Context) {
            foreground.value = false
            startup?.cancel()
            startup = lifecycleScope.launch {
                val cache = SillagePreloadCache(context)
                cache.expire()
                currentCoroutineContext().ensureActive()
                cache.heartbeat()
                foreground.value = true
            }
            WorkManager.getInstance(context).cancelUniqueWork("sillage-preload-expiry")
        }
        fun onStop(context: Context) {
            startup?.cancel()
            foreground.value = false
            SillagePreloadCache(context).heartbeat()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "sillage-preload-expiry", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SillagePreloadExpiryWorker>()
                    .setInitialDelay(PRELOAD_EXPIRY_MS, TimeUnit.MILLISECONDS).build(),
            )
        }
    }
}

class SillagePreloadExpiryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!SillagePreloadCache.foreground.value) SillagePreloadCache(applicationContext).expire()
        Result.success()
    }
}
