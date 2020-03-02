package io.kraftverk.internal.component

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.component.BeanComponent
import io.kraftverk.component.ComponentProperty
import io.kraftverk.component.ModuleComponent
import io.kraftverk.component.ValueComponent
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.container.newBean
import io.kraftverk.internal.container.newValue
import io.kraftverk.internal.module.ModuleCreationContext
import io.kraftverk.module.Module
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@PublishedApi
internal fun <T : Any> newBeanComponent(
    config: BeanConfig<T>
): BeanComponent<T> = object :
    BeanComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ComponentProperty<Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef)
        val bean = thisRef.container.newBean(beanName, config)
        return object :
            ComponentProperty<Bean<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Bean<T> {
                return bean
            }
        }
    }
}

@PublishedApi
internal data class BeanConfig<T : Any>(
    val type: KClass<T>,
    val lazy: Boolean?,
    val createInstance: BeanDefinition.() -> T
)

@PublishedApi
internal fun <T : Any> newValueComponent(
    name: String?,
    config: ValueConfig<T>
): ValueComponent<T> = object :
    ValueComponent<T> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ComponentProperty<Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val value = thisRef.container.newValue(valueName, config)
        return object :
            ComponentProperty<Value<T>> {
            override fun getValue(thisRef: Module, property: KProperty<*>): Value<T> {
                return value
            }
        }
    }
}

@PublishedApi
internal data class ValueConfig<T : Any>(
    val type: KClass<T>,
    val default: String?,
    val lazy: Boolean?,
    val secret: Boolean,
    val createInstance: ValueDefinition.(String) -> T
)

internal fun <M : Module> newModuleComponent(
    name: String? = null,
    subModule: () -> M
): ModuleComponent<M> = object :
    ModuleComponent<M> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ComponentProperty<M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = ModuleCreationContext.use(moduleName) { subModule() }
        return object : ComponentProperty<M> {
            override fun getValue(thisRef: Module, property: KProperty<*>): M {
                return module
            }
        }
    }
}

private fun String.toQualifiedName(module: Module) =
    (if (module.namespace.isBlank()) this else "${module.namespace}.$this")

private val spinalRegex = "([A-Z]+)".toRegex()

private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
