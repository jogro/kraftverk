/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kraftverk.internal.Runtime
import io.kraftverk.internal.provider

open class PropertyDefinition internal constructor(internal val runtime: Runtime) {
    val profiles: List<String> by lazy { runtime.profiles }
    operator fun <T : Any> Property<T>.invoke(): T = provider().instance()
}

class PropertySupplierDefinition<T> internal constructor(
    runtime: Runtime,
    private val supply: () -> T
) : PropertyDefinition(runtime) {
    fun next() = supply()
}

open class BeanDefinition internal constructor(runtime: Runtime) : PropertyDefinition(runtime) {
    operator fun <T : Any> Component<T>.invoke(): T = provider().instance()
}

class BeanSupplierDefinition<T> internal constructor(
    runtime: Runtime,
    private val supply: () -> T
) : BeanDefinition(runtime) {
    fun next() = supply()
}

class BeanConsumerDefinition<T> internal constructor(
    runtime: Runtime,
    private val instance: T,
    private val consume: (T) -> Unit
) : BeanDefinition(runtime) {
    fun next() = consume(instance)
}

open class CustomBeanDefinition(parent: BeanDefinition) : BeanDefinition(parent.runtime)
open class CustomPropertyDefinition(parent: PropertyDefinition) : PropertyDefinition(parent.runtime)
