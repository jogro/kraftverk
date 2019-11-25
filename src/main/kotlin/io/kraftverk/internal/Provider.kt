/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

internal class Provider<T : Any>(
    val type: KClass<T>,
    private val onCreate: () -> T,
    private val onStart: (T) -> Unit,
    private val onDestroy: (T) -> Unit
) {

    @Volatile
    var instanceId: Int? = null
        private set

    @Volatile
    private var instance: T? = null

    fun get(): T {
        return instance ?: synchronized(this) {
            instance ?: onCreate().also {
                instance = it
                instanceId = currentInstanceId.incrementAndGet()
                onStart(it)
            }
        }
    }

    fun destroy() {
        instance?.also {
            synchronized(this) {
                instance?.also {
                    onDestroy(it)
                    instance = null
                }
            }
        }
    }

    companion object {
        val currentInstanceId = AtomicInteger(0)
    }

}
