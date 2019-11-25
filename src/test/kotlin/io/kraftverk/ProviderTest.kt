package io.kraftverk

import io.kraftverk.internal.Provider
import io.kotlintest.shouldBe
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProviderTest {

    private companion object {
        private const val THREAD_COUNT = 20
        private val threadPool = Executors.newFixedThreadPool(THREAD_COUNT)
    }

    private open class LifeCycle {
        open fun onCreate() = 42
        open fun onStart(t: Int) {}
        open fun onDestroy(t: Int) {}
    }

    @Test
    fun testType() {
        val provider = newProvider(LifeCycle())
        provider.type.shouldBe(Int::class)
    }

    @Test
    fun testGetOnce() {
        val lifeCycleSpy = spyk<LifeCycle>()
        val provider = newProvider(lifeCycleSpy)
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
            lifeCycleSpy.onDestroy(42)
        }
        provider.get()
        verify(atLeast = 1, atMost = 1) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
        }
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onDestroy(42)
        }
    }

    @Test
    fun testGetManyTimesAsync() {
        val enterOnCreate = CountDownLatch(1)
        val leaveOnCreate = CountDownLatch(1)
        val latchedLifeCycle = object : LifeCycle() {
            override fun onCreate(): Int {
                enterOnCreate.countDown()
                leaveOnCreate.await()
                return super.onCreate()
            }
        }
        val lifeCycleSpy = spyk(latchedLifeCycle)
        val providerSpy = spyk(newProvider(lifeCycleSpy))
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
            lifeCycleSpy.onDestroy(42)
        }

        // Start thread 1
        threadPool.submit { providerSpy.get() }

        // Make sure that thread 1 has entered onCreate method
        enterOnCreate.await()

        // Start the other threads
        val enterGet = CountDownLatch(THREAD_COUNT - 1)
        val leaveGet = CountDownLatch(THREAD_COUNT - 1)
        repeat(THREAD_COUNT - 1) {
            threadPool.submit { enterGet.countDown(); providerSpy.get(); leaveGet.countDown() }
        }
        // Wait for the other threads to enter the get method
        enterGet.await()

        // Thread 1, still inside the lifeCycle.onCreate method, has been awaiting a leaveOnCreate count down.
        // Now, it is time.
        leaveOnCreate.countDown()

        // Last step, wait for the other threads to having performed the gets
        leaveGet.await()

        // Do verifications
        verify(atLeast = THREAD_COUNT, atMost = THREAD_COUNT) {
            providerSpy.get()
        }
        verify(atLeast = 1, atMost = 1) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
        }
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onDestroy(42)
        }
    }

    @Test
    fun testRemoveOnce() {
        val lifeCycleSpy = spyk<LifeCycle>()
        val provider = newProvider(lifeCycleSpy)
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onDestroy(42)
        }
        provider.get()
        provider.destroy()
        verify(atLeast = 1, atMost = 1) {
            lifeCycleSpy.onDestroy(42)
        }
    }

    @Test
    fun testRemoveManyTimesAsync() {
        val enterOnDestroy = CountDownLatch(1)
        val leaveOnDestroy = CountDownLatch(1)
        val latchedLifeCycle = object : LifeCycle() {
            override fun onDestroy(t: Int) {
                enterOnDestroy.countDown()
                leaveOnDestroy.await()
            }
        }
        val lifeCycleSpy = spyk(latchedLifeCycle)
        val providerSpy = spyk(newProvider(lifeCycleSpy))
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onDestroy(42)
        }

        // Start Thread 1
        threadPool.submit { providerSpy.get(); providerSpy.destroy() }

        // Make sure that Thread 1 has entered onDestroy method
        enterOnDestroy.await()

        // Start the other threads
        val enterRemove = CountDownLatch(THREAD_COUNT - 1)
        val leaveRemove = CountDownLatch(THREAD_COUNT - 1)
        repeat(THREAD_COUNT - 1) {
            threadPool.submit { enterRemove.countDown(); providerSpy.destroy(); leaveRemove.countDown() }
        }
        // Wait for the other threads to enter the remove method
        enterRemove.await()

        // Thread 1, still inside the lifeCycle.onDestroy method, has been awaiting a leaveOnDestroy count down.
        // Now, it is time.
        leaveOnDestroy.countDown()

        // Last step, wait for the other threads to having performed the removes
        leaveRemove.await()

        // Do verifications
        verify(atLeast = THREAD_COUNT, atMost = THREAD_COUNT) {
            providerSpy.destroy()
        }
        verify(atLeast = 1, atMost = 1) {
            lifeCycleSpy.onDestroy(42)
        }
    }

    private fun newProvider(lifeCycleSpy: LifeCycle): Provider<@ParameterName(
        name = "t"
    ) Int> {
        return Provider(
            Int::class,
            lifeCycleSpy::onCreate,
            lifeCycleSpy::onStart,
            lifeCycleSpy::onDestroy
        )
    }

}

