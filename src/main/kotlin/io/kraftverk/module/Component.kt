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
    operator fun provideDelegate(thisRef: Modular, property: KProperty<*>): ReadOnlyProperty<Modular, T>
}

interface BeanComponent<out T : Any> : Component<Bean<T>>
interface ValueComponent<out T : Any> : Component<Value<T>>
interface ModuleComponent<out M : Modular> : Component<M>
interface PartitionComponent<M : Modular, out P : Partition<M>> : Component<P>

interface BeanRefComponent<out T : Any> : Component<BeanRef<T>>
interface ModuleRefComponent<out M : Modular> : Component<ModuleRef<M>>

internal class Delegate<T : Any>(private val t: T) : ReadOnlyProperty<Modular, T> {
    override fun getValue(thisRef: Modular, property: KProperty<*>): T {
        return t
    }
}
