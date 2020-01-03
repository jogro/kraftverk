/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal

abstract class InternalModule {
    internal val container: Container = ModuleCreationContext.container
    internal val namespace: String = ModuleCreationContext.namespace
}
