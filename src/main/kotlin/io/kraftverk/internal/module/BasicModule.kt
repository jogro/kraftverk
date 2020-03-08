/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.module

import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.beanProviders
import io.kraftverk.internal.container.valueProviders

private val threadBoundContainer = ThreadBound<Container>()
private val threadBoundNamespace = ThreadBound<String>()

open class BasicModule {
    internal val container: Container = threadBoundContainer.get()
    internal val namespace: String = threadBoundNamespace.get()

    internal fun start() = container.start()
    internal fun stop() = container.stop()

    internal val beanProviders get() = container.beanProviders
    internal val valueProviders get() = container.valueProviders

    internal companion object
}

internal fun <M : BasicModule> createRootModule(
    lazy: Boolean = false,
    env: Environment,
    namespace: String,
    createModule: () -> M
): M = threadBoundContainer.use(Container(lazy, env)) {
    threadBoundNamespace.use(namespace) {
        createModule()
    }
}

internal fun <M : BasicModule> createSubModule(
    namespace: String,
    moduleFun: () -> M
): M = threadBoundNamespace.use(namespace) {
    moduleFun()
}

private class ThreadBound<T> {

    private val threadLocal = ThreadLocal<T>()

    fun get(): T = threadLocal.get() ?: throw IllegalStateException()

    fun <R> use(value: T, block: () -> R): R {
        val previous: T? = threadLocal.get()
        threadLocal.set(value)
        try {
            return block()
        } finally {
            if (previous == null) {
                threadLocal.remove()
            } else {
                threadLocal.set(previous)
            }
        }
    }
}
