/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.component.ModuleComponent
import io.kraftverk.internal.component.newModuleComponent
import io.kraftverk.internal.module.InternalModule

abstract class Module : InternalModule()

fun <M : Module> module(
    name: String? = null,
    module: () -> M
): ModuleComponent<M> =
    newModuleComponent(
        name,
        module
    )
