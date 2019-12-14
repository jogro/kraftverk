/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.BeanDelegate
import io.kraftverk.internal.PropertyDelegate

sealed class Binding<out T : Any>
sealed class Bean<out T : Any> : Binding<T>()
sealed class Property<out T : Any> : Binding<T>()

internal class BeanImpl<T : Any>(val delegate: BeanDelegate<T>) : Bean<T>()
internal class PropertyImpl<T : Any>(val delegate: PropertyDelegate<T>) : Property<T>()
