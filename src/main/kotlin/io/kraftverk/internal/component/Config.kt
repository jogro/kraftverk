/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.internal.component

import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import kotlin.reflect.KClass

@PublishedApi
internal data class BeanConfig<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean?,
    val createInstance: BeanDefinition.() -> T
)

@PublishedApi
internal data class ValueConfig<T : Any>(
    val type: KClass<T>,
    val default: String?,
    val lazy: Boolean?,
    val secret: Boolean,
    val createInstance: ValueDefinition.(String) -> T
)
