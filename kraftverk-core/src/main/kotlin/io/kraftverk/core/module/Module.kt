/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk.core.module

import io.kraftverk.core.binding.Bean
import io.kraftverk.core.binding.Binding
import io.kraftverk.core.binding.Value
import io.kraftverk.core.binding.delegate
import io.kraftverk.core.common.BindingRef
import io.kraftverk.core.common.Pipe
import io.kraftverk.core.common.PipeDelegate
import io.kraftverk.core.common.PipeImpl
import io.kraftverk.core.common.delegate
import io.kraftverk.core.declaration.BeanConfigurationDeclaration
import io.kraftverk.core.declaration.BeanDeclaration
import io.kraftverk.core.declaration.PipeDeclaration
import io.kraftverk.core.declaration.ValueDeclaration
import io.kraftverk.core.internal.container.configure
import io.kraftverk.core.internal.module.AbstractModule
import io.kraftverk.core.internal.module.ModuleSupport
import io.kraftverk.core.internal.module.createChildModule
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class Module : BasicModule<Module>()

open class ChildModule<PARENT : BasicModule<*>> : BasicModule<PARENT>()

open class BasicModule<PARENT : AbstractModule> : ModuleSupport<PARENT>() {

    /**
     * Declares a [Bean][io.kraftverk.core.binding.Bean] in a Kraftverk managed module.
     * ```kotlin
     * class AppModule : Module() {
     *     val dataSource by bean { HikariDataSource() }  // Bean<HikariDataSource>
     * }
     * ```
     *
     * Bean declarations should be kept short. See [configure] on how to configure the bean properties and the
     * lifecycle of the bean.
     *
     * See [bind] on how to bind a new implementation to the bean after it has been declared.
     *
     * A bean can be used to inject other beans by invoking it as a function in a bean declaration.
     *
     * ```kotlin
     * class AppModule : Module() {
     *     val dataSource by bean { HikariDataSource() }
     *     val repository by bean { Repository(dataSource()) } // <--- Injection
     * }
     * ```
     */
    inline fun <reified T : Any> bean(
        lazy: Boolean? = null,
        noinline instance: BeanDeclaration.() -> T
    ): BeanDelegateProvider<T> = bean(T::class, lazy, instance)

    /**
     * Configures a bean after it has been declared:
     * ```kotlin
     * class JdbcModule : Module() {
     *     val dataSource by bean { HikariDataSource() }
     *     init {
     *         configure(dataSource) {
     *             it.jdbcUrl = [...]
     *             it.username = [...]
     *             it.password = [...]
     *         }
     *     }
     * }
     * ```
     * Manage the bean lifecycle:
     * ```kotlin
     * configure(server) { ds ->
     *     lifecycle {
     *         onCreate { it.start() }
     *         onDestroy { it.stop() }
     *     }
     * }
     * ```
     * A bean cannot be configured after the module has been started.
     */
    fun <T : Any> configure(
        bean: Bean<T>,
        block: BeanConfigurationDeclaration.(T) -> Unit
    ) {
        bean.delegate.configure { instance, lifecycle ->
            BeanConfigurationDeclaration(container, lifecycle).block(instance)
        }
    }

    /**
     * Binds a [Bean] to a new implementation:
     * ```kotlin
     * val app = Kraftverk.manage { AppModule() }
     * app.start {
     *     bind(repository) to { mockk() }
     * }
     * ```
     * Use [callOriginal][io.kraftverk.core.declaration.BeanSupplierInterceptorDeclaration.callOriginal] to
     * get hold of the original implementation.
     * ```kotlin
     * app.start {
     *     bind(repository) to { spyk(callOriginal()) }
     * }
     */
    fun <T : Any> bind(bean: Bean<T>) =
        BeanBinder(container, bean)

    inline fun <reified T : Any> value(
        name: String? = null,
        default: T? = null,
        secret: Boolean = false,
        noinline instance: ValueDeclaration.(Any) -> T
    ): ValueDelegateProvider<T> = value(name, T::class, default, secret, instance)

    /**
     * Binds a [Value] to a new value:
     * ```kotlin
     * val app = Kraftverk.manage { AppModule() }
     * app.start { // this: AppModule
     *     bind(rabbit.username) to { "testuser" }
     * }
     * ```
     */
    fun <T : Any> bind(value: Value<T>) = ValueBinder(container, value)

    fun <T : Any> pipe(block: PipeDeclaration.(T) -> Unit = { }):
            ReadOnlyProperty<BasicModule<*>, Pipe<T>> {
        val pipe: PipeImpl<T> = PipeImpl(PipeDelegate())
        configure(pipe, block)
        return Delegate(pipe)
    }

    fun <T : Any> configure(
        pipe: Pipe<T>,
        block: PipeDeclaration.(T) -> Unit
    ) {
        pipe.delegate.configure { instance ->
            PipeDeclaration(container).block(instance)
        }
    }
}

fun <PARENT : BasicModule<*>, CHILD : BasicModule<PARENT>> PARENT.module(
    name: String? = null,
    instance: () -> CHILD
): ModuleDelegateProvider<PARENT, CHILD> =
    object : ModuleDelegateProvider<PARENT, CHILD> {
        override fun provideDelegate(
            thisRef: BasicModule<*>,
            property: KProperty<*>
        ): ReadOnlyProperty<BasicModule<*>, CHILD> {
            val moduleName = qualifyMemberName(name ?: property.name)
            val module: CHILD = createChildModule(moduleName, instance)
            return Delegate(module)
        }
    }

fun <PARENT : BasicModule<*>, CHILD : ChildModule<PARENT>, T : Any, BINDING : Binding<T>> CHILD.import(
    binding: PARENT.() -> BINDING
): ReadOnlyProperty<BasicModule<*>, BindingRef<T>> {
    val ref = BindingRef { instanceFromParent(binding) }
    return Delegate(ref)
}

fun <PARENT : BasicModule<*>, CHILD : ChildModule<PARENT>> CHILD.parent(
    block: PARENT.() -> Unit
) {
    container.configure { parent.block() }
}
