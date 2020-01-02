/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

internal class Provider<T : Any>(
    val type: KClass<T>,
    private val create: () -> T,
    private val onCreate: (T) -> Unit,
    private val onDestroy: (T) -> Unit
) {

    @Volatile
    var instanceId: Int? = null
        private set

    @Volatile
    private var instance: T? = null

    fun instance(): T {
        return instance ?: synchronized(this) {
            instance ?: create().also {
                instance = it
                instanceId = currentInstanceId.incrementAndGet()
                onCreate(it)
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
        val currentInstanceId = AtomicInteger()
    }

}
