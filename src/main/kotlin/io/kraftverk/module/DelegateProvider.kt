/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.XBean
import io.kraftverk.common.BeanRef
import io.kraftverk.common.ModuleRef
import io.kraftverk.common.XBeanRef
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DelegateProvider<out T> {
    operator fun provideDelegate(thisRef: AbstractModule, property: KProperty<*>): ReadOnlyProperty<AbstractModule, T>
}

interface ComponentDelegateProvider<out T : Any, S : Any> : DelegateProvider<XBean<T, S>>
interface BeanDelegateProvider<out T : Any> : DelegateProvider<Bean<T>>
interface ValueDelegateProvider<out T : Any> : DelegateProvider<Value<T>>
interface ModuleDelegateProvider<AM : AbstractModule, out MO : ChildModule<AM>> : DelegateProvider<MO>

interface BeanRefDelegateProvider<out T : Any> : DelegateProvider<BeanRef<T>>
interface XBeanRefDelegateProvider<out T : Any> : DelegateProvider<XBeanRef<T>>
interface ModuleRefDelegateProvider<out AM : AbstractModule> : DelegateProvider<ModuleRef<AM>>

internal class Delegate<out T : Any>(private val t: T) : ReadOnlyProperty<AbstractModule, T> {
    override fun getValue(thisRef: AbstractModule, property: KProperty<*>): T {
        return t
    }
}
