package io.kraftverk.module

import io.kraftverk.binding.Component
import io.kraftverk.binding.ComponentImpl
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.declaration.ComponentDeclaration
import io.kraftverk.internal.container.createComponent
import io.kraftverk.internal.container.createComponentInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : ComponentSpi<S>, S : Any> AbstractModule.component(
    lazy: Boolean? = null,
    noinline instance: ComponentDeclaration.() -> T
): ComponentDelegateProvider<T, S> =
    component(T::class, lazy, { t: T, shape: (S) -> Unit -> t.onShape(shape) }, instance)

@PublishedApi
internal fun <T : Any, S : Any> AbstractModule.component(
    type: KClass<T>,
    lazy: Boolean? = null,
    onShape: (T, (S) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T

): ComponentDelegateProvider<T, S> = object :
    ComponentDelegateProvider<T, S> {

    override fun provideDelegate(
        thisRef: AbstractModule,
        property: KProperty<*>
    ): ReadOnlyProperty<AbstractModule, Component<T, S>> {
        val componentName = qualifyName(property.name)
        return createComponent(componentName, type, lazy, onShape, instance).let(::Delegate)
    }
}

private fun <T : Any, S : Any> AbstractModule.createComponent(
    propertyName: String,
    type: KClass<T>,
    lazy: Boolean? = null,
    onShape: (T, (S) -> Unit) -> Unit,
    instance: ComponentDeclaration.() -> T
): ComponentImpl<T, S> {
    val componentName = qualifyName(propertyName)
    val config = ComponentDefinition(
        name = componentName,
        lazy = lazy,
        onShape = onShape,
        type = type,
        instance = { container.createComponentInstance(instance) }
    )
    return container.createComponent(config)
}
