/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.common.BeanRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DelegateProvider<out T> {
    operator fun provideDelegate(thisRef: BasicModule<*>, property: KProperty<*>): ReadOnlyProperty<BasicModule<*>, T>
}

interface BeanDelegateProvider<out T : Any> : DelegateProvider<Bean<T>>
interface ValueDelegateProvider<out T : Any> : DelegateProvider<Value<T>>
interface ModuleDelegateProvider<PARENT : BasicModule<*>, out CHILD : BasicModule<PARENT>> : DelegateProvider<CHILD>

interface BeanRefDelegateProvider<out T : Any> : DelegateProvider<BeanRef<T>>

internal class Delegate<out T : Any>(private val t: T) : ReadOnlyProperty<BasicModule<*>, T> {
    override fun getValue(thisRef: BasicModule<*>, property: KProperty<*>): T = t
}
