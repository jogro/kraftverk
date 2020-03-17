/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.BeanRef
import io.kraftverk.binding.Value
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Component<out T> {
    operator fun provideDelegate(thisRef: ModuleSupport, property: KProperty<*>): ReadOnlyProperty<ModuleSupport, T>
}

interface BeanComponent<out T : Any> : Component<Bean<T>>
interface ValueComponent<out T : Any> : Component<Value<T>>
interface ModuleComponent<out M : ModuleSupport> : Component<M>
interface PartitionComponent<M : ModuleSupport, out P : PartitionOf<M>> : Component<P>

interface BeanRefComponent<out T : Any> : Component<BeanRef<T>>

internal class Delegate<T : Any>(private val t: T) : ReadOnlyProperty<ModuleSupport, T> {
    override fun getValue(thisRef: ModuleSupport, property: KProperty<*>): T {
        return t
    }
}
