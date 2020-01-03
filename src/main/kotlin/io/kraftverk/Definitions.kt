/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.provider

open class PropertyDefinition internal constructor(val environment: Environment) {
    operator fun <T : Any> Property<T>.invoke(): T = provider().instance()
}

class PropertySupplierDefinition<T> internal constructor(
    environment: Environment,
    private val supply: () -> T
) : PropertyDefinition(environment) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(environment: Environment) : PropertyDefinition(environment) {
    operator fun <T : Any> Binding<T>.invoke(): T = provider().instance()
}

class BeanSupplierDefinition<T> internal constructor(
    environment: Environment,
    private val supply: () -> T
) : BeanDefinition(environment) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    environment: Environment,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(environment) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.environment)
open class CustomPropertyDefinition(parent: PropertyDefinition) : PropertyDefinition(parent.environment)
