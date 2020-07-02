/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.common

import io.kraftverk.core.internal.misc.Supplier
import kotlin.reflect.KClass

/**
 * Represents the outcome of a bean or value declaration.
 */
interface BindingDefinition<T : Any> {
    val name: String
    val type: KClass<T>
    val lazy: Boolean?
    val instance: Supplier<T>
}

data class ValueDefinition<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val lazy: Boolean?,
    val secret: Boolean,
    override val instance: Supplier<T>
) : BindingDefinition<T>

data class BeanDefinition<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val lazy: Boolean?,
    override val instance: Supplier<T>
) : BindingDefinition<T>

interface BeanProcessor {
    fun <T : Any> process(bean: BeanDefinition<T>): BeanDefinition<T>
}

interface ValueProcessor {
    fun <T : Any> process(value: ValueDefinition<T>): ValueDefinition<T>
}
