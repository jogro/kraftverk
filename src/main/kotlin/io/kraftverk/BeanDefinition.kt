/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.AppContext
import io.kraftverk.internal.provider

open class BeanDefinition internal constructor(internal val moduleContext: AppContext) {

    val profiles: List<String> by lazy { moduleContext.profiles }

    operator fun <T : Any> Bean<T>.invoke(): T = provider().get()

    operator fun <T : Any> Property<T>.invoke(): T = provider().get()

}

class ConsumerDefinition<T> internal constructor(
    moduleContext: AppContext,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(moduleContext) {
    fun next() = consume(instance)
}

class SupplierDefinition<T> internal constructor(
    moduleContext: AppContext,
    private val supply: () -> T
) : BeanDefinition(moduleContext) {
    fun next() = supply()
}

abstract class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.moduleContext)