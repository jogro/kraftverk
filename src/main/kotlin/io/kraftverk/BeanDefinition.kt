/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.AppContext
import io.kraftverk.internal.provider

sealed class BeanDefinition(internal val moduleContext: AppContext) {

    val profiles: List<String> by lazy { moduleContext.profiles }

    fun <T : Any> inject(bean: Bean<T>): T = bean.provider().get()

    fun inject(property: Property): String = property.provider().get()

    operator fun <T : Any> Bean<T>.invoke(): T = inject(this)

    operator fun Property.invoke(): String = inject(this)

}

class SingletonDefinition internal constructor(moduleContext: AppContext) :
    BeanDefinition(moduleContext)

class PrototypeDefinition internal constructor(moduleContext: AppContext) :
    BeanDefinition(moduleContext)

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