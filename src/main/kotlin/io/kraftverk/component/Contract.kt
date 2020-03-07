package io.kraftverk.component

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.internal.module.BasicModule
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Component<out T> {
    operator fun provideDelegate(thisRef: BasicModule, property: KProperty<*>): ReadOnlyProperty<BasicModule, T>
}

interface BeanComponent<out T : Any> : Component<Bean<T>>
interface ValueComponent<out T : Any> : Component<Value<T>>
interface SubModuleComponent<out T : BasicModule> : Component<T>
