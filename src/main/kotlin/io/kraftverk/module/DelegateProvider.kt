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

interface DelegateProvider<out T> {
    operator fun provideDelegate(thisRef: AbstractModule, property: KProperty<*>): ReadOnlyProperty<AbstractModule, T>
}

interface BeanDelegateProvider<out T : Any, S : Any> : DelegateProvider<Bean<T, S>>
interface ValueDelegateProvider<out T : Any> : DelegateProvider<Value<T>>
interface ModuleDelegateProvider<AM : AbstractModule, out MO : ChildModule<AM>> : DelegateProvider<MO>

interface BeanRefDelegateProvider<out T : Any> : DelegateProvider<BeanRef<T>>
interface ModuleRefDelegateProvider<out AM : AbstractModule> : DelegateProvider<ModuleRef<AM>>

internal class Delegate<out T : Any>(private val t: T) : ReadOnlyProperty<AbstractModule, T> {
    override fun getValue(thisRef: AbstractModule, property: KProperty<*>): T {
        return t
    }
}
