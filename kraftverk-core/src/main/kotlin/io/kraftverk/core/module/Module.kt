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
import io.kraftverk.core.declaration.BeanSupplierInterceptorDeclaration
import io.kraftverk.core.declaration.PipeDeclaration
import io.kraftverk.core.declaration.ValueDeclaration
import io.kraftverk.core.declaration.ValueSupplierDeclaration
import io.kraftverk.core.internal.container.Container
import io.kraftverk.core.internal.container.configure
import io.kraftverk.core.internal.container.createBean
import io.kraftverk.core.internal.container.createValue
import io.kraftverk.core.internal.logging.createLogger
import io.kraftverk.core.internal.misc.ScopedThreadLocal
import io.kraftverk.core.provider.get
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

sealed class AbstractModule {
    internal val logger = createLogger { }
    internal abstract val container: Container
    internal abstract val namespace: String
}

internal class RootModule(override val container: Container) : AbstractModule() {
    override val namespace: String = ""
}

private val scopedParentModule =
    ScopedThreadLocal<AbstractModule>()
private val scopedNamespace = ScopedThreadLocal<String>()

sealed class BasicModule<PARENT : AbstractModule> : AbstractModule() {

    @Suppress("UNCHECKED_CAST")
    internal val parent: PARENT = scopedParentModule.get() as PARENT

    override val container: Container = parent.container
    override val namespace: String = scopedNamespace.get()

    internal fun <T : Any, BINDING : Binding<T>> getInstance(binding: PARENT.() -> BINDING): T =
        parent.binding().delegate.provider.get()

    /**
     * The bean method declares a [Bean][io.kraftverk.core.binding.Bean].
     * ```kotlin
     * class AppModule : Module() {
     *     val dataSource by bean { HikariDataSource() }  // Bean<HikariDataSource>
     * }
     * ```
     *
     * A Bean can be used to inject other beans:
     *
     * ```kotlin
     * class AppModule : Module() {
     *     val dataSource by bean { HikariDataSource() }
     *     val repository by bean { Repository(dataSource()) } // <--- Injection
     * }
     * ```
     *
     * Note that injection occurs by syntactically invoking the Bean as a function (operator invoke).
     *
     * An important feature is the ability to rebind a Bean after it has been declared but the module still
     * hasn't been started[io.kraftverk.core.managed.start]. This feature provides the foundation for mocking etc,
     * see bind[bind].
     */
    inline fun <reified T : Any> bean(
        lazy: Boolean? = null,
        noinline instance: BeanDeclaration.() -> T
    ): BeanDelegateProvider<T> = bean(T::class, lazy, instance)

    inline fun <reified T : Any> value(
        name: String? = null,
        default: T? = null,
        secret: Boolean = false,
        noinline instance: ValueDeclaration.(Any) -> T
    ): ValueDelegateProvider<T> = value(name, T::class, default, secret, instance)

    fun <T : Any> pipe(block: PipeDeclaration.(T) -> Unit = { }):
            ReadOnlyProperty<BasicModule<*>, Pipe<T>> {
        val pipe: PipeImpl<T> = PipeImpl(PipeDelegate())
        configure(pipe, block)
        return Delegate(pipe)
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

    /**
     * Binds a [Value] to a new value:
     * ```kotlin
     * val app = Kraftverk.manage { AppModule() }
     * app.start { // this: AppModule
     *     bind(rabbit.username) to { "testuser" }
     * }
     * ```
     */
    fun <T : Any> bind(value: Value<T>) =
        ValueBinder(container, value)

    /**
     * A helper method for configuring a bean after it has been declared, for example:
     * ```kotlin
     * class JdbcModule : Module() {
     *     [...]
     *     val dataSource by bean { HikariDataSource() }
     *     init {
     *         configure(dataSource) { ds ->
     *             ds.jdbcUrl = [...]
     *             ds.username = [...]
     *             ds.password = [...]
     *         }
     *     }
     * }
     * ```
     */
    fun <T : Any> configure(
        bean: Bean<T>,
        block: BeanConfigurationDeclaration.(T) -> Unit
    ) {
        bean.delegate.configure { instance, lifecycle ->
            BeanConfigurationDeclaration(container, lifecycle).block(instance)
        }
    }

    fun <T : Any> configure(
        pipe: Pipe<T>,
        block: PipeDeclaration.(T) -> Unit
    ) {
        pipe.delegate.configure { instance ->
            PipeDeclaration(container).block(instance)
        }
    }

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
            val qualifiedName = qualifyName(property.name)
            return container.createBean(qualifiedName, type, lazy, instance).let(::Delegate)
        }
    }

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
                val qualifiedName = qualifyName(name ?: property.name).toSpinalCase()
                return container.createValue(qualifiedName, secret, type, default, instance)
            }
        }
}

open class Module : BasicModule<Module>()

internal fun <M : Module> createModule(
    container: Container,
    namespace: String,
    instance: () -> M
): M {
    val rootModule = RootModule(container)
    return scopedParentModule.use(rootModule) {
        scopedNamespace.use(namespace) {
            instance()
        }
    }
}

/*
This needs to be an extension method, since this is the only way to capture PARENT as
a "THIS" type parameter. We want the CHILD to be exactly BasicModule<PARENT>.
 */
internal fun <PARENT : BasicModule<*>, CHILD : BasicModule<PARENT>> PARENT.createChildModule(
    namespace: String,
    instance: () -> CHILD
): CHILD = scopedParentModule.use(this) {
    scopedNamespace.use(namespace) {
        instance()
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
            val moduleName = qualifyName(name ?: property.name)
            val module: CHILD = createChildModule(moduleName, instance)
            return Delegate(module)
        }
    }

open class ChildModule<PARENT : BasicModule<*>> : BasicModule<PARENT>()

fun <PARENT : BasicModule<*>, CHILD : ChildModule<PARENT>, T : Any, BINDING : Binding<T>> CHILD.import(
    binding: PARENT.() -> BINDING
): BindingRefDelegateProvider<T> =
    object : BindingRefDelegateProvider<T> {
        override fun provideDelegate(
            thisRef: BasicModule<*>,
            property: KProperty<*>
        ): ReadOnlyProperty<BasicModule<*>, BindingRef<T>> {
            val ref = BindingRef { getInstance(binding) }
            return Delegate(ref)
        }
    }

fun <PARENT : BasicModule<*>, CHILD : ChildModule<PARENT>> CHILD.parent(
    block: PARENT.() -> Unit
) {
    container.configure { parent.block() }
}

internal fun AbstractModule.qualifyName(name: String) = if (namespace.isBlank()) name else "$namespace.$name"

/**
 * Helper class for the [Module.bind] method.
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
 * Helper class for the [Module.bind] method.
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

private val spinalRegex = "([A-Z]+)".toRegex()
private fun String.toSpinalCase() = replace(spinalRegex, "\\-$1").toLowerCase()
