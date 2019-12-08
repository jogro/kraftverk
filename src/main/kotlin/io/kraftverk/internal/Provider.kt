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

    private val holder: Lazy<InstanceHolder> = lazy {
        create().let {
            InstanceHolder(currentInstanceId.incrementAndGet(), it).apply {
                onCreate(it)
            }
        }
    }

    val instanceId: Int?
        get() = if (holder.isInitialized()) holder.value.id else null

    fun instance(): T = holder.value.instance

    fun destroy() {
        if (holder.isInitialized()) destroyOnce
    }

    private val destroyOnce: Unit by lazy {
        onDestroy(holder.value.instance)
    }

    companion object {
        val currentInstanceId = AtomicInteger(0)
    }

    private inner class InstanceHolder(val id: Int, val instance: T)

}
