/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.provider

import io.kraftverk.declaration.LifecycleActions
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.Supplier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

private val logger = createLogger { }

internal class Singleton<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean,
    private val createInstance: Supplier<T>,
    private val onShape: (T, LifecycleActions) -> Unit
) {

    private val lifecycle = LifecycleActions()

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
                onShape(i3.value, lifecycle)
                lifecycle.onCreate()
                instance = i3
                i3.value
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun destroy() {
        synchronized(this) {
            val i = instance
            if (i != null) {
                try {
                    lifecycle.onDestroy()
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
