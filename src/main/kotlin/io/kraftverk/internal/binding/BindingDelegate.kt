/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.provider.Provider

internal abstract class BindingDelegate<T : Any> {
    abstract fun start()
    abstract fun initialize(lazy: Boolean)
    abstract fun stop()
    abstract val provider: Provider<T>
}
