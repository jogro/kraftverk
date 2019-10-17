package io.kraftverk

import io.kraftverk.internal.PrototypeProvider
import io.kotlintest.shouldBe
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrototypeProviderTest {

    private companion object {
        private const val THREAD_COUNT = 10
        private val threadPool = Executors.newFixedThreadPool(THREAD_COUNT)
    }

    private open class LifeCycle {
        open fun onCreate() = 42
        open fun onStart(t: Int) {}
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
        }
        provider.get()
        verify(atLeast = 1, atMost = 1) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
        }
    }

    @Test
    fun testGetManyTimesAsync() {
        val lifeCycleSpy = spyk<LifeCycle>()
        val providerSpy = spyk(newProvider(lifeCycleSpy))
        verify(atLeast = 0, atMost = 0) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
        }

        val leaveGet = CountDownLatch(THREAD_COUNT)
        repeat(THREAD_COUNT) {
            threadPool.submit { providerSpy.get(); leaveGet.countDown() }
        }
        leaveGet.await()

        // Do verifications
        verify(atLeast = THREAD_COUNT, atMost = THREAD_COUNT) {
            providerSpy.get()
        }
        verify(atLeast = THREAD_COUNT, atMost = THREAD_COUNT) {
            lifeCycleSpy.onCreate()
            lifeCycleSpy.onStart(42)
        }
    }

    private fun newProvider(lifeCycleSpy: LifeCycle): PrototypeProvider<@ParameterName(
        name = "t"
    ) Int> {
        return PrototypeProvider(
            Int::class,
            lifeCycleSpy::onCreate,
            lifeCycleSpy::onStart
        )
    }

}

