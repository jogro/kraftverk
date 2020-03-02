package io.kraftverk.component

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.module.Module
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Component<out T> {
    operator fun provideDelegate(thisRef: Module, property: KProperty<*>): ComponentProperty<T>
}

interface ComponentProperty<out T> : ReadOnlyProperty<Module, T>

interface BeanComponent<out T : Any> : Component<Bean<T>>

interface ValueComponent<out T : Any> : Component<Value<T>>

interface ModuleComponent<out T : Module> : Component<T>
