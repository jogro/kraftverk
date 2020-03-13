package io.kraftverk.module

import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import kotlin.reflect.KClass

class BeanParams<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean? = null,
    val instance: BeanDefinition.() -> T
)

fun defaultBeanParamsProcessor() = object : BeanParamsProcessor {
    override fun <T : Any> process(
        namespace: String,
        propertyName: String,
        params: BeanParams<T>
    ) = params
}

interface BeanParamsProcessor {
    fun <T : Any> process(namespace: String, propertyName: String, params: BeanParams<T>): BeanParams<T>
}

class ValueParams<T : Any>(
    val name: String? = null,
    val type: KClass<T>,
    val default: T? = null,
    val lazy: Boolean? = null,
    val secret: Boolean = false,
    val instance: ValueDefinition.(Any) -> T
)

fun defaultValueParamsProcessor() = object : ValueParamsProcessor {
    override fun <T : Any> process(
        namespace: String,
        propertyName: String,
        params: ValueParams<T>
    ) = params
}

interface ValueParamsProcessor {
    fun <T : Any> process(namespace: String, propertyName: String, params: ValueParams<T>): ValueParams<T>
}
