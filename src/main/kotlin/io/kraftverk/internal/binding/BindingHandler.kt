/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.binding

import io.kraftverk.provider.Provider

internal abstract class BindingHandler<T : Any>(config: BindingConfig<T>) {

    @Volatile
    internal var state: State<T> = State.Defining(config.copy())

    abstract fun createProvider(config: BindingConfig<T>): Provider<T>

    internal sealed class State<out T : Any> {

        class Defining<T : Any>(
            val config: BindingConfig<T>
        ) : State<T>()

        class Started<T : Any>(
            val provider: Provider<T>
        ) : State<T>()

        object Destroyed : State<Nothing>()
    }
}
