package eu.kanade.tachiyomi.ui.reader.loader

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SillagePreloadPriorityTest {
    @Test fun `visible page interrupts background transfer then permits restart`() = runTest {
        val requests = MutableStateFlow(0)
        var started = 0
        var cancelled = 0
        backgroundScope.launch {
            preloadSelection(MutableStateFlow<String?>("series"), MutableStateFlow(true), requests, MutableStateFlow(10))
                .collectLatest { target ->
                    if (target != null) {
                        started++
                        try { awaitCancellation() } finally { cancelled++ }
                    }
                }
        }
        runCurrent()
        assertEquals(1, started)
        requests.value = 1
        runCurrent()
        assertEquals(1, cancelled)
        requests.value = 0
        runCurrent()
        assertEquals(2, started)
    }
    @Test fun `backgrounding and disabling cancel work and changing quantity applies immediately`() = runTest {
        val foreground = MutableStateFlow(true)
        val count = MutableStateFlow(10)
        var current: Int? = null
        backgroundScope.launch {
            preloadSelection(MutableStateFlow<String?>("series"), foreground, MutableStateFlow(0), count)
                .collectLatest { target -> current = target?.second }
        }
        runCurrent()
        assertEquals(10, current)
        count.value = 3
        runCurrent()
        assertEquals(3, current)
        foreground.value = false
        runCurrent()
        assertNull(current)
        foreground.value = true
        count.value = 0
        runCurrent()
        assertNull(current)
    }
}
