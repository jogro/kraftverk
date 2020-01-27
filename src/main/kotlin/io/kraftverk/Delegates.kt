package io.kraftverk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface BeanDelegate<out T : Any> : Delegate<Bean<T>>
interface ValueDelegate<out T : Any> : Delegate<Value<T>>
interface ModuleDelegate<out T : Module> : Delegate<T>

interface Delegate<out T> {
    operator fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<T>
}

interface Property<out T> : ReadOnlyProperty<Module, T>
