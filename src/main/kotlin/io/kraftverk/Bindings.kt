/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.BeanDelegate
import io.kraftverk.internal.ValueDelegate

sealed class Binding<out T : Any>

sealed class Bean<out T : Any> : Binding<T>() {
    companion object
}

sealed class Value<out T : Any> : Binding<T>() {
    companion object
}

internal class BeanImpl<T : Any>(val delegate: BeanDelegate<T>) : Bean<T>()
internal class ValueImpl<T : Any>(val delegate: ValueDelegate<T>) : Value<T>()
