package io.kraftverk.internal.binding

import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class Provider<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean,
    private val createInstance: InstanceFactory<T>,
    private val onCreate: Consumer<T>,
    private val onDestroy: Consumer<T>
) {

    @Volatile
    private var instance: Instance<T>? = null

    val instanceId: Int?
        get() = synchronized(this) {
            instance?.id
        }

    fun initialize() {
        if (!lazy) get()
    }

    fun get(): T {
        val i = instance
        if (i != null) {
            return i.value
        }
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2.value
            } else {
                val i3 = Instance(
                    createInstance(),
                    currentInstanceId.incrementAndGet()
                )
                onCreate(i3.value)
                instance = i3
                i3.value
            }
        }
    }

    fun destroy() {
        synchronized(this) {
            val i = instance
            if (i != null) {
                try {
                    onDestroy(i.value)
                } catch (ex: Exception) {
                    logger.error("Couldn't destroy bean", ex)
                }
                instance = null
            }
        }
    }

    companion object {
        val currentInstanceId = AtomicInteger()
    }

    data class Instance<T : Any>(val value: T, val id: Int)
}
