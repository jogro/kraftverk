/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.AppContext
import io.kraftverk.internal.provider

open class PropertyDefinition internal constructor(internal val appContext: AppContext) {
    val profiles: List<String> by lazy { appContext.profiles }
    operator fun <T : Any> Property<T>.invoke(): T = provider().instance()
}

class PropertySupplierDefinition<T> internal constructor(
    moduleContext: AppContext,
    private val supply: () -> T
) : PropertyDefinition(moduleContext) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(appContext: AppContext) : PropertyDefinition(appContext) {
    operator fun <T : Any> Component<T>.invoke(): T = provider().instance()
}

class BeanSupplierDefinition<T> internal constructor(
    moduleContext: AppContext,
    private val supply: () -> T
) : BeanDefinition(moduleContext) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    moduleContext: AppContext,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(moduleContext) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.appContext)
open class CustomPropertyDefinition(parent: PropertyDefinition) : PropertyDefinition(parent.appContext)
