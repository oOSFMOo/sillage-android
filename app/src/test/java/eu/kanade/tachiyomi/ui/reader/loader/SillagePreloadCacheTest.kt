package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.model.Page
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SillagePreloadCacheTest {
    @TempDir lateinit var directory: File
    private val lastActive = AtomicLong(System.currentTimeMillis())
    private fun cache(): SillagePreloadCache {
        val preferences = mockk<SharedPreferences>()
        every { preferences.getLong(any(), any()) } answers { lastActive.get() }
        val context = mockk<Context>()
        every { context.cacheDir } returns directory
        every { context.getSharedPreferences(any(), any()) } returns preferences
        return SillagePreloadCache(context)
    }
    private fun response() = Response.Builder().request(Request.Builder().url("https://example.org/image").build())
        .protocol(Protocol.HTTP_1_1).code(200).message("OK").body("image-bytes".toResponseBody()).build()

    @Test fun `prefetched pages and image files are reusable without another network request`() = runTest {
        val cache = cache()
        cache.savePages(30, listOf(Page(0, "page", "https://example.org/image")))
        cache.saveImage(30, "https://example.org/image", response())
        assertEquals("https://example.org/image", cache.pages(30)!!.single().imageUrl)
        assertEquals("image-bytes", cache.image(30, "https://example.org/image")!!.readText())
        assertFalse(File(directory, "sillage-rolling-preload/30").listFiles()!!.any { it.extension == "part" })
    }
    @Test fun `read cleanup preserves current chapter and retained downloads`() = runTest {
        val cache = cache()
        val permanent = File(directory, "permanent-download.cbz").apply { writeText("keep") }
        cache.saveImage(30, "same-url", response())
        cache.saveImage(31, "same-url", response())
        cache.removeRead(setOf(30, 31), 31)
        assertNull(cache.image(30, "same-url"))
        assertNotNull(cache.image(31, "same-url"))
        assertEquals("keep", permanent.readText())
    }
    @Test fun `expired files are unavailable then cleaned without touching other cache`() = runTest {
        val cache = cache()
        val other = File(directory, "other-cache").apply { writeText("keep") }
        cache.savePages(30, listOf(Page(0)))
        cache.saveImage(30, "url", response())
        lastActive.set(System.currentTimeMillis() - PRELOAD_EXPIRY_MS - 1000)
        assertNull(cache.pages(30))
        assertNull(cache.image(30, "url"))
        assertTrue(cache.expire())
        assertTrue(File(directory, "sillage-rolling-preload").listFiles()!!.isEmpty())
        assertEquals("keep", other.readText())
    }
}
