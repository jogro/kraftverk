package io.kraftverk.module

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.binding.handler
import io.kraftverk.definition.BeanSupplierDefinition
import io.kraftverk.definition.ValueSupplierDefinition
import io.kraftverk.internal.binding.onBind
import io.kraftverk.internal.container.Container

fun <T : Any> Module.bind(bean: Bean<T>) = BeanBinder(container, bean)

class BeanBinder<T : Any> internal constructor(
    private val container: Container,
    private val bean: Bean<T>
) {
    infix fun to(block: BeanSupplierDefinition<T>.() -> T) {
        bean.handler.onBind { next ->
            BeanSupplierDefinition(container.environment, next).block()
        }
    }
}

fun <T : Any> Module.bind(value: Value<T>) = ValueBinder(container, value)

class ValueBinder<T : Any> internal constructor(
    private val container: Container,
    private val value: Value<T>
) {
    infix fun to(block: ValueSupplierDefinition<T>.() -> T) {
        value.handler.onBind { next ->
            ValueSupplierDefinition(container.environment, next).block()
        }
    }
}
