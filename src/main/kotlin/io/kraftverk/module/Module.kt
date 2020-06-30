/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Binding
import io.kraftverk.binding.handler
import io.kraftverk.common.BeanRef
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.logging.createLogger
import io.kraftverk.internal.misc.ScopedThreadLocal
import io.kraftverk.provider.get
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

sealed class AbstractModule {
    internal val logger = createLogger { }
    internal abstract val container: Container
    internal abstract val namespace: String
}

internal class RootModule(override val container: Container) : AbstractModule() {
    override val namespace: String = ""
}

private val scopedParentModule = ScopedThreadLocal<AbstractModule>()
private val scopedNamespace = ScopedThreadLocal<String>()

sealed class BasicModule<PARENT : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST")
    internal val parent: PARENT = scopedParentModule.get() as PARENT

    override val container: Container = parent.container
    override val namespace: String = scopedNamespace.get()

    internal fun <T : Any, BINDING : Binding<T>> getInstance(binding: PARENT.() -> BINDING): T =
        parent.binding().handler.provider.get()
}

open class Module : BasicModule<Module>()

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

/*
This needs to be an extension method, since this is the only way to capture PARENT as
a "THIS" type parameter. We want the CHILD to be exactly BasicModule<PARENT>.
 */
internal fun <PARENT : BasicModule<*>, CHILD : BasicModule<PARENT>> PARENT.createChildModule(
    namespace: String,
    moduleFun: () -> CHILD
): CHILD = scopedParentModule.use(this) {
    scopedNamespace.use(namespace) {
        moduleFun()
    }
}

fun <PARENT : BasicModule<*>, CHILD : BasicModule<PARENT>> PARENT.module(
    name: String? = null,
    instance: () -> CHILD
): ModuleDelegateProvider<PARENT, CHILD> =
    object : ModuleDelegateProvider<PARENT, CHILD> {
        override fun provideDelegate(
            thisRef: BasicModule<*>,
            property: KProperty<*>
        ): ReadOnlyProperty<BasicModule<*>, CHILD> {
            val moduleName = qualifyName(name ?: property.name)
            val module: CHILD = createChildModule(moduleName, instance)
            return Delegate(module)
        }
    }

open class ChildModule<PARENT : BasicModule<*>> : BasicModule<PARENT>()

fun <PARENT : BasicModule<*>, CHILD : ChildModule<PARENT>, T : Any, COMPONENT : Bean<T>> CHILD.ref(
    bean: PARENT.() -> COMPONENT
): BeanRefDelegateProvider<T> =
    object : BeanRefDelegateProvider<T> {
        override fun provideDelegate(
            thisRef: BasicModule<*>,
            property: KProperty<*>
        ): ReadOnlyProperty<BasicModule<*>, BeanRef<T>> {
            val ref = BeanRef { getInstance(bean) }
            return Delegate(ref)
        }
    }

internal fun AbstractModule.qualifyName(name: String) = if (namespace.isBlank()) name else "$namespace.$name"
