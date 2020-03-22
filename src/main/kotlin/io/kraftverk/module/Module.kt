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
import io.kraftverk.env.environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.provider.get

private val scopedContainer = ScopedThreadLocal<Container>()
private val scopedNamespace = ScopedThreadLocal<String>()
private val scopedParent = ScopedThreadLocal<AbstractModule>()

sealed class AbstractModule {
    internal val logger = createLogger { }
    internal abstract val container: Container
    internal abstract val namespace: String
}

object Root : AbstractModule() {
    override val container: Container = Container(false, environment(), emptyList(), emptyList())
    override val namespace: String = ""
}

open class Module : ModuleOf<Module>()

open class ModuleOf<AM : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST")
    internal val root: AM = scopedParent.get() as AM

    override val container: Container = scopedContainer.get()
    override val namespace: String = scopedNamespace.get()

    internal fun <T : Any, B : Binding<T>> getInstance(binding: AM.() -> B): T = root.binding().provider.get()
}

internal fun <M : Module> createRootModule(
    lazy: Boolean = false,
    env: Environment,
    namespace: String,
    beanProcessors: List<BeanProcessor>,
    valueProcessors: List<ValueProcessor>,
    createModule: () -> M
): M = scopedContainer.use(Container(lazy, env, beanProcessors, valueProcessors)) {
    scopedParent.use(Root) {
        scopedNamespace.use(namespace) {
            createModule()
        }
    }
}

internal fun <AM : AbstractModule, MO : ModuleOf<AM>> AbstractModule.createModuleOf(
    namespace: String,
    moduleFun: () -> MO
): MO = scopedParent.use(this) {
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
