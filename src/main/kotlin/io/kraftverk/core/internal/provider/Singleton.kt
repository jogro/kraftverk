/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.internal.provider

import io.kraftverk.core.declaration.LifecycleActions
import io.kraftverk.core.internal.logging.createLogger
import io.kraftverk.core.internal.misc.Supplier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

private val logger = createLogger { }

internal class Singleton<T : Any>(
    val type: KClass<T>,
    private val lazy: Boolean?,
    private val createInstance: Supplier<T>,
    private val onConfigure: (T) -> Unit,
    private val lifecycleActions: LifecycleActions
) {

    @Volatile
    private var instance: Instance<T>? = null

    val instanceId: Int?
        get() = synchronized(this) {
            instance?.id
        }

    fun initialize(lazy: Boolean) {
        if (!(this.lazy ?: lazy)) get()
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
                onConfigure(i3.value)
                lifecycleActions.onCreate()
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
                    lifecycleActions.onDestroy()
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
