/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.provider

open class ValueDefinition internal constructor(val env: Environment) {
    operator fun <T : Any> Value<T>.invoke(): T = provider().instance()
}

class ValueSupplierDefinition<T> internal constructor(
    env: Environment,
    private val supply: () -> T
) : ValueDefinition(env) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(env: Environment) : ValueDefinition(env) {
    operator fun <T : Any> Bean<T>.invoke(): T = provider().instance()
}

class BeanSupplierDefinition<T> internal constructor(
    env: Environment,
    private val supply: () -> T
) : BeanDefinition(env) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    env: Environment,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(env) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.env)
open class CustomValueDefinition(parent: ValueDefinition) : ValueDefinition(parent.env)
