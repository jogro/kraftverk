/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.common

import io.kraftverk.internal.misc.Supplier
import kotlin.reflect.KClass

interface BindingConfig<T : Any> {
    val name: String
    val type: KClass<T>
    val lazy: Boolean
    val instance: Supplier<T>
}

data class ValueConfig<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val lazy: Boolean,
    val secret: Boolean,
    override val instance: Supplier<T>
) : BindingConfig<T>

data class BeanConfig<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val lazy: Boolean,
    override val instance: Supplier<T>
) : BindingConfig<T>

interface BeanProcessor {
    fun <T : Any> process(bean: BeanConfig<T>): BeanConfig<T>
}

interface ValueProcessor {
    fun <T : Any> process(value: ValueConfig<T>): ValueConfig<T>
}
