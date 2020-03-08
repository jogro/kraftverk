/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.definition

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.provider
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.beanProviders
import io.kraftverk.internal.container.valueProviders
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.InstanceFactory
import io.kraftverk.provider.get

open class ValueDefinition internal constructor(internal val container: Container) {
    val valueProviders get() = container.valueProviders
    operator fun <T : Any> Value<T>.invoke(): T = provider.get()
}

class ValueSupplierDefinition<T> internal constructor(
    container: Container,
    private val supply: InstanceFactory<T>
) : ValueDefinition(container) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(container: Container) : ValueDefinition(container) {
    val beanProviders get() = container.beanProviders
    operator fun <T : Any> Bean<T>.invoke(): T = provider.get()
}

class BeanSupplierDefinition<T> internal constructor(
    container: Container,
    val supply: InstanceFactory<T>
) : BeanDefinition(container) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    container: Container,
    private val instance: T,
    private val consume: Consumer<T>
) : BeanDefinition(container) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.container)
open class CustomValueDefinition(parent: ValueDefinition) : ValueDefinition(parent.container)
