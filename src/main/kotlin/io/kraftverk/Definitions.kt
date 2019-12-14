/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.Registry
import io.kraftverk.internal.provider

open class PropertyDefinition internal constructor(internal val registry: Registry) {
    val profiles: List<String> by lazy { registry.profiles }
    operator fun <T : Any> Property<T>.invoke(): T = provider().instance()
}

class PropertySupplierDefinition<T> internal constructor(
    registry: Registry,
    private val supply: () -> T
) : PropertyDefinition(registry) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(registry: Registry) : PropertyDefinition(registry) {
    operator fun <T : Any> Binding<T>.invoke(): T = provider().instance()
}

class BeanSupplierDefinition<T> internal constructor(
    registry: Registry,
    private val supply: () -> T
) : BeanDefinition(registry) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    registry: Registry,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(registry) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.registry)
open class CustomPropertyDefinition(parent: PropertyDefinition) : PropertyDefinition(parent.registry)
