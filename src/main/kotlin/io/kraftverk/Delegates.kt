package io.kraftverk

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface BeanDelegate<out T : Any> : Delegate<Bean<T>>
interface ValueDelegate<out T : Any> : Delegate<Value<T>>
interface ModuleDelegate<out T : Module> : Delegate<T>

interface Delegate<out T> {
    operator fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<T>
}

interface Property<out T> : ReadOnlyProperty<Module, T>

@PublishedApi
internal fun <T : Any> newBeanDelegate(
    config: BeanConfig<T>
): BeanDelegate<T> = object : BeanDelegate<T> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<Bean<T>> {
        val beanName = property.name.toQualifiedName(thisRef)
        val bean = thisRef.container.newBean(beanName, config)
        return object : Property<Bean<T>> {
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
    val refreshable: Boolean?,
    val createInstance: BeanDefinition.() -> T
)

@PublishedApi
internal fun <T : Any> newValueDelegate(
    name: String?,
    config: ValueConfig<T>
): ValueDelegate<T> = object : ValueDelegate<T> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<Value<T>> {
        val valueName = (name ?: property.name).toQualifiedName(thisRef).toSpinalCase()
        val value = thisRef.container.newValue(valueName, config)
        return object : Property<Value<T>> {
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

internal fun <M : Module> newModuleDelegate(
    name: String? = null,
    subModule: () -> M
): ModuleDelegate<M> = object : ModuleDelegate<M> {

    override fun provideDelegate(thisRef: Module, property: KProperty<*>): Property<M> {
        val moduleName = (name ?: property.name).toQualifiedName(thisRef)
        val module = ModuleCreationContext.use(moduleName) { subModule() }
        return object : Property<M> {
            override fun getValue(thisRef: Module, property: KProperty<*>): M {
                return module
            }
        }
    }

}

private fun String.toQualifiedName(module: Module) =
    (if (module.namespace.isBlank()) this else "${module.namespace}.$this")
