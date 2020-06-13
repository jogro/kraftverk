/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.common.BeanRef
import io.kraftverk.common.ModuleRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Component<out T> {
    operator fun provideDelegate(thisRef: AbstractModule, property: KProperty<*>): ReadOnlyProperty<AbstractModule, T>
}

interface BeanComponent<out T : Any> : Component<Bean<T>>
interface ValueComponent<out T : Any> : Component<Value<T>>
interface ModuleComponent<AM : AbstractModule, out MO : ChildModule<AM>> : Component<MO>

interface BeanRefComponent<out T : Any> : Component<BeanRef<T>>
interface ModuleRefComponent<out AM : AbstractModule> : Component<ModuleRef<AM>>

internal class Delegate<T : Any>(private val t: T) : ReadOnlyProperty<AbstractModule, T> {
    override fun getValue(thisRef: AbstractModule, property: KProperty<*>): T {
        return t
    }
}
