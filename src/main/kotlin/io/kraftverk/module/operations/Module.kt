package io.kraftverk.module.operations

import io.kraftverk.component.ModuleComponent
import io.kraftverk.internal.component.newModuleComponent
import io.kraftverk.module.Module

fun <M : Module> module(
    name: String? = null,
    module: () -> M
): ModuleComponent<M> =
    newModuleComponent(
        name,
        module
    )
