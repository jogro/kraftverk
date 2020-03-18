/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.definition

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.provider
import io.kraftverk.common.BeanRef
import io.kraftverk.common.ModuleRef
import io.kraftverk.env.Environment
import io.kraftverk.internal.container.Container
import io.kraftverk.internal.container.beanProviders
import io.kraftverk.internal.container.valueProviders
import io.kraftverk.internal.misc.Consumer
import io.kraftverk.internal.misc.Supplier
import io.kraftverk.module.Modular
import io.kraftverk.provider.get

open class ValueDefinition internal constructor(internal val container: Container) {
    val env: Environment get() = container.environment
    val valueProviders get() = container.valueProviders
    operator fun <T : Any> Value<T>.invoke(): T = provider.get()
}

class ValueSupplierDefinition<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : ValueDefinition(container) {
    fun proceed() = supply()
}

open class BeanDefinition internal constructor(container: Container) : ValueDefinition(container) {
    val beanProviders get() = container.beanProviders
    operator fun <T : Any> Bean<T>.invoke(): T = provider.get()
    operator fun <T : Any> BeanRef<T>.invoke(): T = instance()
    operator fun <M : Modular> ModuleRef<M>.invoke(): M = instance()
}

class BeanSupplierDefinition<T> internal constructor(
    container: Container,
    private val supply: Supplier<T>
) : BeanDefinition(container) {
    fun proceed() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    container: Container,
    private val instance: T,
    private val consume: Consumer<T>
) : BeanDefinition(container) {
    fun proceed() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.container)
open class CustomValueDefinition(parent: ValueDefinition) : ValueDefinition(parent.container)
