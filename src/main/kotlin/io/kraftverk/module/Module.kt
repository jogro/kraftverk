/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Binding
import io.kraftverk.binding.Component
import io.kraftverk.binding.handler
import io.kraftverk.binding.provider
import io.kraftverk.internal.binding.provider
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.ScopedThreadLocal
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

open class Module : ChildModule<Module>()

open class ChildModule<AM : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST")
    internal val parent: AM = scopedParentModule.get() as AM

    override val container: Container = parent.container
    override val namespace: String = scopedNamespace.get()

    internal fun <T : Any, B : Binding<T>> getInstance(binding: AM.() -> B): T =
        parent.binding().handler.provider.get()
}

internal fun <M : Module> createModule(
    container: Container,
    namespace: String,
    createModule: () -> M
): M {
    val rootModule = RootModule(container)
    return scopedParentModule.use(rootModule) {
        scopedNamespace.use(namespace) {
            createModule()
        }
    }
}

internal fun <AM : AbstractModule, MO : ChildModule<AM>> AbstractModule.createChildModule(
    namespace: String,
    moduleFun: () -> MO
): MO = scopedParentModule.use(this) {
    scopedNamespace.use(namespace) {
        moduleFun()
    }
}

internal fun AbstractModule.qualifyName(name: String) = if (namespace.isBlank()) name else "$namespace.$name"
