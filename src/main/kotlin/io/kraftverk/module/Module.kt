/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.common.BeanProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.logging.createLogger

private val threadBoundContainer = ThreadBound<Container>()
private val threadBoundNamespace = ThreadBound<String>()

/**
 * A [Module] is the place where [Bean]s and [Value]s are defined.
 */
open class Module {

    internal val logger = createLogger { }

    internal val container: Container = threadBoundContainer.get()
    internal val namespace: String = threadBoundNamespace.get()

    internal companion object
}

internal fun Module.qualifyName(name: String) =
    if (namespace.isBlank()) name else "$namespace.$name"

internal fun <M : Module> createRootModule(
    lazy: Boolean = false,
    env: Environment,
    namespace: String,
    beanProcessors: List<BeanProcessor>,
    valueProcessors: List<ValueProcessor>,
    createModule: () -> M
): M = threadBoundContainer.use(Container(lazy, env, beanProcessors, valueProcessors)) {
    threadBoundNamespace.use(namespace) {
        createModule()
    }
}

internal fun <M : Module> createSubModule(
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
