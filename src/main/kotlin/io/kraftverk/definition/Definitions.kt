/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.definition

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.provider
import io.kraftverk.env.Environment
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.provider.get

open class ValueDefinition internal constructor(val env: Environment) {
    operator fun <T : Any> Value<T>.invoke(): T = provider.get()
}

class ValueSupplierDefinition<T> internal constructor(
    env: Environment,
    private val supply: InstanceFactory<T>
) : ValueDefinition(env) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(env: Environment) : ValueDefinition(env) {
    operator fun <T : Any> Bean<T>.invoke(): T = provider.get()
}

class BeanSupplierDefinition<T> internal constructor(
    env: Environment,
    val supply: InstanceFactory<T>
) : BeanDefinition(env) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    env: Environment,
    private val instance: T,
    private val consume: Consumer<T>
) : BeanDefinition(env) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.env)
open class CustomValueDefinition(parent: ValueDefinition) : ValueDefinition(parent.env)
