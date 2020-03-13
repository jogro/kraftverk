package io.kraftverk.module

import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import kotlin.reflect.KClass

class BeanParams<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean? = null,
    val instance: BeanDefinition.() -> T
)

fun defaultBeanParamsTransformer() = object : BeanParamsTransformer {
    override fun <T : Any> transform(
        namespace: String,
        propertyName: String,
        params: BeanParams<T>
    ) = params
}

interface BeanParamsTransformer {
    fun <T : Any> transform(namespace: String, propertyName: String, params: BeanParams<T>): BeanParams<T>
}

class ValueParams<T : Any>(
    val name: String? = null,
    val type: KClass<T>,
    val default: String? = null,
    val lazy: Boolean? = null,
    val secret: Boolean = false,
    val instance: ValueDefinition.(Any) -> T
)

fun defaultValueParamsTransformer() = object : ValueParamsTransformer {
    override fun <T : Any> transform(
        namespace: String,
        propertyName: String,
        params: ValueParams<T>
    ) = params
}

interface ValueParamsTransformer {
    fun <T : Any> transform(namespace: String, propertyName: String, params: ValueParams<T>): ValueParams<T>
}
