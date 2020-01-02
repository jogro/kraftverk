package io.kraftverk.internal

abstract class InternalModule {
    internal val container: Container = ModuleCreationContext.container
    internal val namespace: String = ModuleCreationContext.namespace
}