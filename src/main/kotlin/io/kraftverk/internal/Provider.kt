/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

internal sealed class Provider<out T : Any> {
    abstract fun get(): T
    internal abstract fun destroy()
}

internal class PrototypeProvider<T : Any>(
    val type: KClass<T>,
    private val onCreate: () -> T,
    private val onStart: (T) -> Unit
) : Provider<T>() {

    override fun get() = onCreate().also {
        onStart(it)
    }

    override fun destroy() {
    }
}

internal class SingletonProvider<T : Any>(
    val type: KClass<T>,
    private val onCreate: () -> T,
    private val onStart: (T) -> Unit,
    private val onDestroy: (T) -> Unit
) : Provider<T>() {

    @Volatile
    var instanceId: Int? = null
        private set

    @Volatile
    private var instance: T? = null

    override fun get(): T {
        return instance ?: synchronized(this) {
            instance ?: onCreate().also {
                instance = it
                instanceId = currentInstanceId.incrementAndGet()
                onStart(it)
            }
        }
    }

    override fun destroy() {
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
