/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.common.BeanProcessor
import io.kraftverk.common.ValueProcessor
import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.provider.get

private val scopedContainer = ScopedThreadLocal<Container>()
private val scopedNamespace = ScopedThreadLocal<String>()
private val scopedPartitionRoot = ScopedThreadLocal<AbstractModule>()

sealed class AbstractModule {
    internal val logger = createLogger { }

    internal val container: Container = scopedContainer.get()
    internal val namespace: String = scopedNamespace.get()
}

open class Module : AbstractModule()

open class Partition<M : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST")
    internal val root: M = scopedPartitionRoot.get() as M

    internal fun <T : Any, B : Binding<T>> getInstance(binding: M.() -> B): T = root.binding().provider.get()
}

internal fun <M : Module> createRootModule(
    lazy: Boolean = false,
    env: Environment,
    namespace: String,
    beanProcessors: List<BeanProcessor>,
    valueProcessors: List<ValueProcessor>,
    createModule: () -> M
): M = scopedContainer.use(Container(lazy, env, beanProcessors, valueProcessors)) {
    scopedNamespace.use(namespace) {
        createModule()
    }
}

internal fun <M : Module> AbstractModule.createModule(
    namespace: String,
    moduleFun: () -> M
): M = scopedNamespace.use(namespace) {
    moduleFun()
}

internal fun <M : AbstractModule, SM : Partition<M>> AbstractModule.createPartition(
    namespace: String,
    moduleFun: () -> SM
): SM = scopedPartitionRoot.use(this) {
    scopedNamespace.use(namespace) {
        moduleFun()
    }
}

internal fun AbstractModule.qualifyName(name: String) = if (namespace.isBlank()) name else "$namespace.$name"

private class ScopedThreadLocal<T> {

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
