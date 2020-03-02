package io.kraftverk.internal.module

import io.kraftverk.internal.container.Container

open class InternalModule {
    internal val container: Container = ModuleCreationContext.container
    internal val namespace: String = ModuleCreationContext.namespace

    internal companion object
}
