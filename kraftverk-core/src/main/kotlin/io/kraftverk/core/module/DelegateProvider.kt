/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.module

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.Value
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DelegateProvider<out T> {
    operator fun provideDelegate(thisRef: BasicModule<*>, property: KProperty<*>): ReadOnlyProperty<BasicModule<*>, T>
}

interface BeanDelegateProvider<out T : Any> : DelegateProvider<Bean<T>>
interface ValueDelegateProvider<out T : Any> : DelegateProvider<Value<T>>
interface ModuleDelegateProvider<PARENT : BasicModule<*>, out CHILD : BasicModule<PARENT>> :
    DelegateProvider<CHILD>

internal class Delegate<out T : Any>(private val t: T) : ReadOnlyProperty<BasicModule<*>, T> {
    override fun getValue(thisRef: BasicModule<*>, property: KProperty<*>): T = t
}
