package io.kraftverk.internal.component

import io.kraftverk.binding.Bean
import io.kraftverk.binding.Value
import io.kraftverk.component.BeanComponent
import io.kraftverk.component.SubModuleComponent
import io.kraftverk.component.ValueComponent
import io.kraftverk.definition.BeanDefinition
import io.kraftverk.definition.ValueDefinition
import io.kraftverk.internal.container.newBean
import io.kraftverk.internal.container.newValue
import io.kraftverk.internal.module.BasicModule
import io.kraftverk.internal.module.createSubModule
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@PublishedApi
internal fun <T : Any> newBeanComponent(
    config: BeanConfig<T>
): BeanComponent<T> = object :
    BeanComponent<T> {

    override fun provideDelegate(
        thisRef: BasicModule,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule, Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef)
        val bean = thisRef.container.newBean(beanName, config)
        return object :
            ReadOnlyProperty<BasicModule, Bean<T>> {
            override fun getValue(thisRef: BasicModule, property: KProperty<*>): Bean<T> {
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
        thisRef: BasicModule,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule, Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val value = thisRef.container.newValue(valueName, config)
        return object :
            ReadOnlyProperty<BasicModule, Value<T>> {
            override fun getValue(thisRef: BasicModule, property: KProperty<*>): Value<T> {
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

internal fun <M : BasicModule> newSubModuleComponent(
    name: String? = null,
    subModule: () -> M
): SubModuleComponent<M> = object :
    SubModuleComponent<M> {

    override fun provideDelegate(
        thisRef: BasicModule,
        property: KProperty<*>
    ): ReadOnlyProperty<BasicModule, M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = createSubModule(moduleName) { subModule() }
        return object : ReadOnlyProperty<BasicModule, M> {
            override fun getValue(thisRef: BasicModule, property: KProperty<*>): M {
                return module
            }
        }
    }
}

private fun String.toQualifiedName(module: BasicModule) =
    (if (module.namespace.isBlank()) this else "${module.namespace}.$this")

private val spinalRegex = "([A-Z]+)".toRegex()

private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
