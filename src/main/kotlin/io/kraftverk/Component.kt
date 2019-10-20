/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.PropertyBinding
import io.kraftverk.internal.PrototypeBinding
import io.kraftverk.internal.SingletonBinding

sealed class Component
sealed class Bean<out T : Any> : Component()
sealed class SingletonBean<out T : Any> : Bean<T>()
sealed class PrototypeBean<out T : Any> : Bean<T>()
sealed class Property : Component()

internal class PrototypeBeanImpl<T : Any>(val binding: PrototypeBinding<T>) : PrototypeBean<T>()
internal class SingletonBeanImpl<T : Any>(val binding: SingletonBinding<T>) : SingletonBean<T>()
internal class PropertyImpl(val binding: PropertyBinding) : Property()
