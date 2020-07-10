package io.kraftverk.core.internal.module

import io.kraftverk.core.binding.Binding
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.internal.container.Container
import io.kraftverk.core.internal.misc.ScopedThreadLocal
import io.kraftverk.core.provider.get

private val scopedParentModule = ScopedThreadLocal<AbstractModule>()
private val scopedNamespace = ScopedThreadLocal<String>()

open class ScopedModule<PARENT : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST") // The price we pay for using a thread local instead of a constructor arg
    internal val parent: PARENT = scopedParentModule.get() as PARENT

    override val container: Container = parent.container
    override val namespace: String = scopedNamespace.get()

    internal fun <T : Any, BINDING : Binding<T>> instanceFromParent(binding: PARENT.() -> BINDING): T =
        parent.binding().delegate.provider.get()
}

internal class RootModule(override val container: Container) : AbstractModule() {
    override val namespace: String = ""
}

internal fun <M : ScopedModule<*>> createModule(
    container: Container,
    namespace: String,
    instance: () -> M
): M {
    val rootModule = RootModule(container)
    return scopedParentModule.use(rootModule) {
        scopedNamespace.use(namespace) {
            instance()
        }
    }
}

internal fun <PARENT : ScopedModule<*>, CHILD : ScopedModule<PARENT>> PARENT.createChildModule(
    namespace: String,
    instance: () -> CHILD
): CHILD = scopedParentModule.use(this) {
    scopedNamespace.use(namespace) {
        instance()
    }
}
