/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.CustomBean
import io.kraftverk.binding.Value
import io.kraftverk.common.ComponentRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DelegateProvider<out T> {
    operator fun provideDelegate(thisRef: AbstractModule, property: KProperty<*>): ReadOnlyProperty<AbstractModule, T>
}

interface BeanDelegateProvider<out T : Any> : DelegateProvider<Bean<T>>
interface ComponentDelegateProvider<out T : Any, S : Any> : DelegateProvider<CustomBean<T, S>>
interface ValueDelegateProvider<out T : Any> : DelegateProvider<Value<T>>
interface ModuleDelegateProvider<AM : AbstractModule, out MO : ChildModule<AM>> : DelegateProvider<MO>

interface ComponentRefDelegateProvider<out T : Any> : DelegateProvider<ComponentRef<T>>

internal class Delegate<out T : Any>(private val t: T) : ReadOnlyProperty<AbstractModule, T> {
    override fun getValue(thisRef: AbstractModule, property: KProperty<*>): T = t
}
