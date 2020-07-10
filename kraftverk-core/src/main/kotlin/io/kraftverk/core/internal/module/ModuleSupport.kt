package io.kraftverk.core.internal.module

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.Value
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.declaration.BeanDeclaration
import io.kraftverk.core.declaration.BeanSupplierInterceptorDeclaration
import io.kraftverk.core.declaration.ValueDeclaration
import io.kraftverk.core.declaration.ValueSupplierDeclaration
import io.kraftverk.core.internal.container.Container
import io.kraftverk.core.internal.container.createBean
import io.kraftverk.core.internal.container.createValue
import io.kraftverk.core.module.BasicModule
import io.kraftverk.core.module.BeanDelegateProvider
import io.kraftverk.core.module.Delegate
import io.kraftverk.core.module.ValueDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class ModuleSupport<PARENT : AbstractModule> : ScopedModule<PARENT>() {

    @PublishedApi
    internal fun <T : Any> bean(
        type: KClass<T>,
        lazy: Boolean? = null,
        instance: BeanDeclaration.() -> T
    ): BeanDelegateProvider<T> = object :
        BeanDelegateProvider<T> {
        override fun provideDelegate(
            thisRef: BasicModule<*>,
            property: KProperty<*>
        ): ReadOnlyProperty<BasicModule<*>, Bean<T>> {
            val qualifiedName = qualifyMemberName(property.name)
            return container.createBean(qualifiedName, type, lazy, instance).let(::Delegate)
        }
    }

    private val spinalRegex = "([A-Z]+)".toRegex()
    private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()

    @PublishedApi
    internal fun <T : Any> value(
        name: String? = null,
        type: KClass<T>,
        default: T? = null,
        secret: Boolean = false,
        instance: ValueDeclaration.(Any) -> T

    ): ValueDelegateProvider<T> =
        object : ValueDelegateProvider<T> {

            override fun provideDelegate(
                thisRef: BasicModule<*>,
                property: KProperty<*>
            ): ReadOnlyProperty<BasicModule<*>, Value<T>> {
                val qualifiedName = qualifyMemberName(name ?: property.name).toSpinalCase()
                return container.createValue(qualifiedName, secret, type, default, instance)
            }
        }

    /**
     * Helper class for the module bind method.
     */
    class BeanBinder<T : Any> internal constructor(
        private val container: Container,
        private val bean: Bean<T>
    ) {
        infix fun to(block: BeanSupplierInterceptorDeclaration<T>.() -> T) {
            bean.delegate.bind { ctx, callOriginal ->
                BeanSupplierInterceptorDeclaration(
                    ctx,
                    container,
                    callOriginal
                ).block()
            }
        }
    }

    /**
     * Helper class for the module bind method.
     */
    class ValueBinder<T : Any> internal constructor(
        private val container: Container,
        private val value: Value<T>
    ) {
        infix fun to(block: ValueSupplierDeclaration<T>.() -> T) {
            value.delegate.bind { callOriginal ->
                ValueSupplierDeclaration(container, callOriginal).block()
            }
        }
    }
}
