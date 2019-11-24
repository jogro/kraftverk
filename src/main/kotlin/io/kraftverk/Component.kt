/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.SingletonBinding

sealed class Component
sealed class Bean<out T : Any> : Component()
sealed class Property<out T : Any> : Component()

internal class BeanImpl<T : Any>(val binding: SingletonBinding<T>) : Bean<T>()
internal class PropertyImpl<T : Any>(val binding: SingletonBinding<T>) : Property<T>()
