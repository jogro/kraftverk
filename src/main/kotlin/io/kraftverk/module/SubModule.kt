package io.kraftverk.module

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun <M : Module> Module.module(
    name: String? = null,
    instance: () -> M
): SubModuleComponent<M> = object :
    SubModuleComponent<M> {

    override fun provideDelegate(
        thisRef: Module,
        property: KProperty<*>
    ): ReadOnlyProperty<Module, M> {
        val moduleName = qualifyName(name ?: property.name)
        logger.debug { "Creating sub module '$moduleName'" }
        val module = createSubModule(moduleName, instance)
        logger.debug { "Created sub module '$moduleName'" }
        return Delegate(module)
    }
}
