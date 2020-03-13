/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Component<out T> {
    operator fun provideDelegate(thisRef: Module, property: KProperty<*>): ReadOnlyProperty<Module, T>
}

interface BeanComponent<out T : Any> : Component<Bean<T>>
interface ValueComponent<out T : Any> : Component<Value<T>>
interface SubModuleComponent<out T : Module> : Component<T>

internal class Delegate<T : Any>(private val t: T) : ReadOnlyProperty<Module, T> {
    override fun getValue(thisRef: Module, property: KProperty<*>): T {
        return t
    }
}
