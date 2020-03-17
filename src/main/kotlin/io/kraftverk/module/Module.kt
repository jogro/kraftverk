/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Value
import io.kraftverk.binding.provider
import io.kraftverk.common.BeanProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.provider.get

private val threadBoundSubModuleRoot = ThreadBound<ModuleSupport>()
private val threadBoundContainer = ThreadBound<Container>()
private val threadBoundNamespace = ThreadBound<String>()

sealed class ModuleSupport {
    internal val logger = createLogger { }

    internal val container: Container = threadBoundContainer.get()
    internal val namespace: String = threadBoundNamespace.get()
}

/**
 * A [Module] is the place where [Bean]s and [Value]s are defined.
 */
open class Module : ModuleSupport()

open class PartitionOf<M : ModuleSupport> : ModuleSupport() {

    @Suppress("UNCHECKED_CAST")
    private val root: M = threadBoundSubModuleRoot.get() as M

    @PublishedApi
    internal fun <T : Any, B : Binding<T>> getInstance(binding: M.() -> B): T = root.binding().provider.get()
}

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

internal fun <M : Module> ModuleSupport.createModule(
    namespace: String,
    moduleFun: () -> M
): M = threadBoundNamespace.use(namespace) {
    moduleFun()
}

internal fun <M : ModuleSupport, SM : PartitionOf<M>> ModuleSupport.createPartition(
    namespace: String,
    moduleFun: () -> SM
): SM = threadBoundSubModuleRoot.use(this) {
    threadBoundNamespace.use(namespace) {
        moduleFun()
    }
}

internal fun ModuleSupport.qualifyName(name: String) = if (namespace.isBlank()) name else "$namespace.$name"

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
