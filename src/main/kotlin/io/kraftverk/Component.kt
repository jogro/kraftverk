/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.Binding

sealed class Component<out T : Any>
sealed class Bean<out T : Any> : Component<T>()
sealed class Property<out T : Any> : Component<T>()

internal class BeanImpl<T : Any>(val binding: Binding<T>) : Bean<T>()
internal class PropertyImpl<T : Any>(val binding: Binding<T>) : Property<T>()
