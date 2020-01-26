/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger { }

internal class Provider<T : Any>(
    val container: Container,
    val type: KClass<T>,
    val lazy: Boolean,
    val refreshable: Boolean,
    private val create: () -> T,
    private val onCreate: (T) -> Unit,
    private val onDestroy: (T) -> Unit
) {

    @Volatile
    private var instance: Instance<T>? = null

    val instanceId: Int?
        get() = synchronized(this) {
            instance?.id
        }

    fun prepare() {
        if (!lazy) instance()
    }

    fun instance(): T {
        container.checkIsRunning()
        val i = instance
        if (i != null) {
            return i.value
        }
        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2.value
            } else {
                val i3 = Instance(create(), currentInstanceId.incrementAndGet())
                onCreate(i3.value)
                instance = i3
                i3.value
            }
        }
    }

    fun refresh() {
        if (refreshable) destroy()
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
