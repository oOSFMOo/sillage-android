package eu.kanade.tachiyomi.ui.reader.loader

import tachiyomi.domain.chapter.model.Chapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal fun <T : Any> preloadSelection(plan: Flow<T?>, foreground: Flow<Boolean>, requests: Flow<Int>, count: Flow<Int>): Flow<Pair<T, Int>?> =
    combine(plan, foreground, requests, count) { target, active, waiting, amount ->
        if (active && waiting == 0 && amount > 0 && target != null) target to amount else null
    }

internal const val PRELOAD_EXPIRY_MS = 2 * 60 * 60 * 1000L

internal fun preloadWindow(chapters: List<Chapter>, currentId: Long, count: Int = 10): List<Chapter> {
    if (count <= 0) return emptyList()
    val position = chapters.indexOfFirst { it.id == currentId }
    if (position < 0) return emptyList()
    return listOf(chapters[position]) + chapters.drop(position + 1).filterNot { it.read }.take(count.coerceAtMost(10))
}

internal fun preloadExpired(lastActive: Long, now: Long): Boolean =
    lastActive > 0 && now - lastActive >= PRELOAD_EXPIRY_MS
