package io.kraftverk.module.operations

import io.kraftverk.component.BeanComponent
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.internal.component.BeanConfig
import io.kraftverk.internal.component.newBeanComponent

inline fun <reified T : Any> bean(
    lazy: Boolean? = null,
    noinline instance: BeanDefinition.() -> T
): BeanComponent<T> = newBeanComponent(
    BeanConfig(
        type = T::class,
        lazy = lazy,
        createInstance = instance
    )
)
