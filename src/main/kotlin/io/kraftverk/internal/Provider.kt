/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

import kotlin.reflect.KClass

internal sealed class Provider<T : Any> {
    abstract val type: KClass<T>
    abstract fun get(): T
    internal abstract fun destroy()
}

internal class PrototypeProvider<T : Any>(
    override val type: KClass<T>,
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
    override val type: KClass<T>,
    private val onCreate: () -> T,
    private val onStart: (T) -> Unit,
    private val onDestroy: (T) -> Unit
) : Provider<T>() {

    @Volatile
    private var instance: T? = null

    override fun get(): T {
        return instance ?: synchronized(this) {
            instance ?: onCreate().also {
                instance = it
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

}
