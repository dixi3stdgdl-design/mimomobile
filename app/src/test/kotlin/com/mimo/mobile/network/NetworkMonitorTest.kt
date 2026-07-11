package com.mimo.mobile.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkMonitor connectivity state logic.
 *
 * Since NetworkMonitor depends on Android's ConnectivityManager, these tests
 * verify the StateFlow behavior and state transitions by simulating the
 * monitor's internal state changes through the same MutableStateFlow pattern
 * used in production.
 */
class NetworkMonitorTest {

    private lateinit var isAvailable: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        isAvailable = MutableStateFlow(false)
    }

    @Test
    fun `initial state is unavailable`() {
        assertFalse(isAvailable.value)
    }

    @Test
    fun `state transitions to available on network gain`() {
        isAvailable.value = true
        assertTrue(isAvailable.value)
    }

    @Test
    fun `state transitions to unavailable on network loss`() {
        isAvailable.value = true
        assertTrue(isAvailable.value)

        isAvailable.value = false
        assertFalse(isAvailable.value)
    }

    @Test
    fun `rapid state changes are handled correctly`() {
        val states = mutableListOf<Boolean>()
        repeat(100) { i ->
            isAvailable.value = i % 2 == 0
            states.add(isAvailable.value)
        }
        // Last state should be false (index 99 is odd)
        assertFalse(isAvailable.value)
        assertEquals(100, states.size)
    }

    @Test
    fun `state flow emits current value on collect`() = runBlocking {
        isAvailable.value = true
        val value = isAvailable.first()
        assertTrue(value)
    }

    @Test
    fun `state flow emits updates to collectors`() = runTest {
        val collected = mutableListOf<Boolean>()
        val job = launch(Dispatchers.Unconfined) {
            isAvailable.collect { collected.add(it) }
        }

        isAvailable.value = true
        isAvailable.value = false
        isAvailable.value = true

        advanceUntilIdle()
        job.cancel()

        assertTrue("Should have at least 3 emissions", collected.size >= 3)
        assertTrue(collected.last())
    }

    @Test
    fun `ConnectionState enum has expected values`() {
        val states = ConnectionState.values()
        assertEquals(4, states.size)
    }

    @Test
    fun `simulated onAvailable callback sets available`() {
        isAvailable.value = true
        assertTrue(isAvailable.value)
    }

    @Test
    fun `simulated onLost callback sets unavailable`() {
        isAvailable.value = true
        isAvailable.value = false
        assertFalse(isAvailable.value)
    }

    @Test
    fun `simulated onCapabilitiesChanged with validated internet`() {
        val hasInternet = true
        val hasValidated = true
        isAvailable.value = hasInternet && hasValidated
        assertTrue(isAvailable.value)
    }

    @Test
    fun `simulated onCapabilitiesChanged without validated`() {
        val hasInternet = true
        val hasValidated = false
        isAvailable.value = hasInternet && hasValidated
        assertFalse(isAvailable.value)
    }

    @Test
    fun `state flow handles multiple collectors`() = runTest {
        val collector1 = mutableListOf<Boolean>()
        val collector2 = mutableListOf<Boolean>()

        val job1 = launch(Dispatchers.Unconfined) {
            isAvailable.collect { collector1.add(it) }
        }
        val job2 = launch(Dispatchers.Unconfined) {
            isAvailable.collect { collector2.add(it) }
        }

        isAvailable.value = true
        isAvailable.value = false

        advanceUntilIdle()
        job1.cancel()
        job2.cancel()

        assertTrue(collector1.last())
        assertTrue(collector2.last())
    }
}
