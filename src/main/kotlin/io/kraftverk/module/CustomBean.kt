package io.kraftverk.module

import io.kraftverk.binding.CustomBean
import io.kraftverk.binding.CustomBeanImpl
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.declaration.ComponentDeclaration
import io.kraftverk.internal.container.createComponentInstance
import io.kraftverk.internal.container.createCustomBean
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : CustomBeanSpi<S>, S : Any> AbstractModule.customBean(
    lazy: Boolean? = null,
    noinline instance: ComponentDeclaration.() -> T
): ComponentDelegateProvider<T, S> =
    customBean(T::class, lazy, { t: T, configure: (S) -> Unit -> t.onConfigure(configure) }, instance)

@PublishedApi
internal fun <T : Any, S : Any> AbstractModule.customBean(
    type: KClass<T>,
    lazy: Boolean? = null,
    onConfigure: (T, (S) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T

): ComponentDelegateProvider<T, S> = object :
    ComponentDelegateProvider<T, S> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, CustomBean<T, S>> {
        val qualifiedName = qualifyName(property.name)
        return createCustomBean(qualifiedName, type, lazy, onConfigure, instance).let(::Delegate)
    }
}

private fun <T : Any, S : Any> AbstractModule.createCustomBean(
    name: String,
    type: KClass<T>,
    lazy: Boolean? = null,
    onConfigure: (T, (S) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T
): CustomBeanImpl<T, S> {
    val config = ComponentDefinition(
        name = name,
        lazy = lazy,
        onConfigure = onConfigure,
        type = type,
        instance = { container.createComponentInstance(instance) }
    )
    return container.createCustomBean(config)
}
