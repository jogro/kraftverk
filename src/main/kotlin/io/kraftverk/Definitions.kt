/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.provider

open class PropertyDefinition internal constructor(val profiles: List<String>) {
    operator fun <T : Any> Property<T>.invoke(): T = provider().instance()
}

class PropertySupplierDefinition<T> internal constructor(
    profiles: List<String>,
    private val supply: () -> T
) : PropertyDefinition(profiles) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(profiles: List<String>) : PropertyDefinition(profiles) {
    operator fun <T : Any> Binding<T>.invoke(): T = provider().instance()
}

class BeanSupplierDefinition<T> internal constructor(
    profiles: List<String>,
    private val supply: () -> T
) : BeanDefinition(profiles) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    profiles: List<String>,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(profiles) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.profiles)
open class CustomPropertyDefinition(parent: PropertyDefinition) : PropertyDefinition(parent.profiles)
