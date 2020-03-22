/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.ContainerDefinition
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.provider.get

private val scopedParentModule = ScopedThreadLocal<AbstractModule>()
private val scopedNamespace = ScopedThreadLocal<String>()

sealed class AbstractModule {
    internal val logger = createLogger { }
    internal abstract val container: Container
    internal abstract val namespace: String
}

internal class RootModule(override val container: Container) : AbstractModule() {
    override val namespace: String = ""
}

open class Module : ModuleOf<Module>()

open class ModuleOf<AM : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST")
    internal val parent: AM = scopedParentModule.get() as AM

    override val container: Container = parent.container
    override val namespace: String = scopedNamespace.get()

    internal fun <T : Any, B : Binding<T>> getInstance(binding: AM.() -> B): T = parent.binding().provider.get()
}

internal fun <M : Module> createModule(
    containerDefinition: ContainerDefinition,
    namespace: String,
    createModule: () -> M
): M {
    val rootModule = createRootModule(containerDefinition)
    return scopedParentModule.use(rootModule) {
        scopedNamespace.use(namespace) {
            createModule()
        }
    }
}

private fun createRootModule(definition: ContainerDefinition) = with(definition) {
    RootModule(
        Container(lazy, environment, beanProcessors, valueProcessors)
    )
}

internal fun <AM : AbstractModule, MO : ModuleOf<AM>> AbstractModule.createModuleOf(
    namespace: String,
    moduleFun: () -> MO
): MO = scopedParentModule.use(this) {
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
