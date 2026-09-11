package eu.kanade.tachiyomi.data.coil

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class CoverRepairCoordinatorTest {
    @Test
    fun `detects desktop proxies without replacing local and custom covers`() {
        listOf(
            null,
            "",
            "/api/v1/manga/58827/thumbnail",
            "/api/v1/manga/58827/thumbnail?useCache=true",
            "http://127.0.0.1:4567/api/v1/manga/58827/thumbnail",
            "http://localhost:4567/cover.jpg",
            "http://[::1]:4567/cover.jpg",
            "http://192.168.1.32:4567/api/v1/manga/58827/thumbnail",
        ).forEach { assertTrue(needsCoverRepair(it), it) }
        listOf(
            "https://images.example.org/cover.jpg",
            "/storage/emulated/0/Books/cover.jpg",
            "file:///data/user/0/app/cover.jpg",
            "content://media/external/images/18",
            "Custom-cover",
        ).forEach { assertFalse(needsCoverRepair(it), it) }
    }

    @Test
    fun `concurrent image requests repair a manga once and reuse persisted metadata`() = runTest {
        val coordinator = CoverRepairCoordinator(nowMillis = { testScheduler.currentTime })
        var saved: RepairedCover? = null
        var requests = 0
        val covers = List(20) {
            async {
                coordinator.resolve(1, readCached = { saved }) {
                    requests++
                    delay(100)
                    RepairedCover("https://example.org/cover.jpg", 12).also { saved = it }
                }
            }
        }.awaitAll()
        assertEquals(1, requests)
        assertEquals(1, covers.distinct().size)
    }

    @Test
    fun `scroll cancellation drops queued repairs and requests remain spaced`() = runTest {
        val coordinator = CoverRepairCoordinator(nowMillis = { testScheduler.currentTime })
        val starts = mutableListOf<Long>()
        val scrolledAway = launch {
            coordinator.resolve(1, readCached = { null }) {
                error("A cover that left the screen should never request metadata")
            }
        }
        delay(100)
        scrolledAway.cancel()
        List(3) { index ->
            async {
                coordinator.resolve(index.toLong() + 2, readCached = { null }) {
                    starts += testScheduler.currentTime
                    delay(50)
                    RepairedCover("https://example.org/$index.jpg", 12)
                }
            }
        }.awaitAll()
        assertEquals(3, starts.size)
        assertTrue(starts.zipWithNext().all { (first, second) -> second - first >= 800 })
    }

    @Test
    fun `failed repairs retry after a bounded cooldown`() = runTest {
        val coordinator = CoverRepairCoordinator(nowMillis = { testScheduler.currentTime })
        var requests = 0
        suspend fun tryRepair(): Result<RepairedCover> = runCatching {
            coordinator.resolve(1, readCached = { null }) {
                requests++
                throw IOException("offline")
            }
        }
        assertTrue(tryRepair().isFailure)
        assertTrue(tryRepair().isFailure)
        assertEquals(1, requests)
        advanceTimeBy(60_000)
        assertTrue(tryRepair().isFailure)
        assertEquals(2, requests)
    }

    @Test
    fun `cancelling an active request releases the queue without imposing a failure cooldown`() = runTest {
        val coordinator = CoverRepairCoordinator(nowMillis = { testScheduler.currentTime })
        var requests = 0
        val active = launch {
            coordinator.resolve(1, readCached = { null }) {
                requests++
                delay(10_000)
                error("Cancelled source request must not finish")
            }
        }
        delay(500)
        active.cancel()
        active.join()
        val repaired = coordinator.resolve(1, readCached = { null }) {
            requests++
            RepairedCover("https://example.org/cover.jpg", 12)
        }
        assertEquals("https://example.org/cover.jpg", repaired.url)
        assertEquals(2, requests)
        assertTrue(testScheduler.currentTime < 60_000)
    }
}
