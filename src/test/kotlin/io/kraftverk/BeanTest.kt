/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.matchers.collections.containExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kraftverk.binding.Bean
import io.kraftverk.binding.Binding
import io.kraftverk.binding.provider
import io.kraftverk.common.BeanDefinition
import io.kraftverk.common.BeanProcessor
import io.kraftverk.declaration.BeanDeclaration
import io.kraftverk.managed.Managed
import io.kraftverk.managed.beanProviders
import io.kraftverk.managed.customize
import io.kraftverk.managed.get
import io.kraftverk.managed.invoke
import io.kraftverk.managed.registerProcessor
import io.kraftverk.managed.start
import io.kraftverk.managed.stop
import io.kraftverk.module.Module
import io.kraftverk.module.bean
import io.kraftverk.module.bind
import io.kraftverk.module.onCreate
import io.kraftverk.module.onDestroy
import io.kraftverk.provider.BeanProvider
import io.kraftverk.provider.Provider
import io.kraftverk.provider.definition
import io.kraftverk.provider.get
import io.kraftverk.provider.type
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlin.concurrent.thread
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.full.isSubclassOf

class BeanTest : StringSpec() {

    private val widget = mockk<Widget>(relaxed = true)

    private val childWidget = mockk<Widget>(relaxed = true)

    private val widgetFactory = mockk<WidgetFactory>()

    inner class AppModule(private val lazy: Boolean? = null) : Module() {

        val childWidget by bean(lazy = this.lazy) {
            widgetFactory.createWidget(widget())
        }

        val widget by bean(lazy = this.lazy) {
            widgetFactory.createWidget()
        }

        init {
            onCreate(childWidget) { it.start() }
            onCreate(widget) { it.start() }
            onDestroy(widget) { it.stop() }
            onDestroy(childWidget) { it.stop() }
        }
    }

    override fun beforeTest(testCase: TestCase) {
        clearAllMocks()
        every { widgetFactory.createWidget() } returns widget
        every { widgetFactory.createWidget(widget) } returns childWidget
    }

    init {

        "bean instantiation is eager by default" {
            Kraftverk.start { AppModule() }
            verifyThatAllBeansAreInstantiated()
        }

        "bean instantiation is eager when specified for the bean" {
            Kraftverk.start(lazy = true) {
                AppModule(lazy = false)
            }
            verifyThatAllBeansAreInstantiated()
        }

        "bean instantiation is lazy when managed lazily" {
            Kraftverk.start(lazy = true) {
                AppModule()
            }
            verifyThatNoBeansAreInstantiated()
        }

        "bean instantiation is lazy when specified for the bean" {
            Kraftverk.start { AppModule(lazy = true) }
            verifyThatNoBeansAreInstantiated()
        }

        "Extracting a bean returns expected value" {
            val app = Kraftverk.start { AppModule() }
            app { widget } shouldBe widget
            app { childWidget } shouldBe childWidget
        }

        "Beans provided by 'get' should not be instantiated until referenced" {
            val app = Kraftverk.manage { AppModule() }

            val w by app.get { widget }
            val c by app.get { childWidget }

            verifyThatNoBeansAreInstantiated()

            app.start()

            w shouldBe widget
            c shouldBe childWidget
        }

        "Extracting a bean does not propagate to other beans if not necessary" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            app { widget }
            verifySequence {
                widgetFactory.createWidget()
                widget.start()
            }
        }

        "Customize can be used to replace a bean and inhibit its onCreate" {
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            val replacement = mockk<Widget>(relaxed = true)
            app.customize {
                bind(widget) to { replacement }
                onCreate(widget) { }
            }
            app.start()
            verifySequence {
                widgetFactory wasNot Called
                replacement wasNot Called
            }
            app { widget } shouldBe replacement
        }

        "Extracting a bean propagates to other beans if necessary" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            app { childWidget }
            verifySequence {
                widgetFactory.createWidget()
                widget.start()
                widgetFactory.createWidget(widget)
                childWidget.start()
            }
        }

        "Extracting a bean results in one instantiation even if many invocations" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            repeat(3) { app { widget } }
            verifySequence {
                widgetFactory.createWidget()
                widget.start()
            }
        }

        "bean on create invokes 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onCreate(widget) { proceed() }
            }
            verifySequence {
                widget.start()
            }
        }

        "bean on create inhibits 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onCreate(widget) { }
            }
            verify(exactly = 1) {
                widgetFactory.createWidget()
                widget wasNot Called
            }
        }

        "bean on destroy invokes 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onDestroy(widget) { proceed() }
                onDestroy(childWidget) { proceed() }
            }
            clearMocks(widget, childWidget)
            app.stop()
            verifySequence {
                childWidget.stop()
                widget.stop()
            }
        }

        "bean on destroy inhibits 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onDestroy(widget) { }
                onDestroy(childWidget) { }
            }
            clearMocks(widget, childWidget)
            app.stop()
            verifySequence {
                childWidget wasNot Called
                widget wasNot Called
            }
        }

        "Binding a bean does a proper replace 1" {
            val replacement = mockk<Widget>(relaxed = true)
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            app.start {
                bind(widget) to { replacement }
            }
            app { widget } shouldBe replacement

            verifyThatNoBeansAreInstantiated()
        }

        "Binding a bean does a proper replace 2" {
            val replacement = mockk<Widget>(relaxed = true)
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            app.start {
                bind(childWidget) to { replacement }
            }
            app { childWidget } shouldBe replacement

            verifyThatNoBeansAreInstantiated()
        }

        class Mod0(val destroyed: MutableList<String>) : Module() {
            val b1 by bean {
                b0()
                Thread.sleep(2000)
            }
            val b0 by bean {}
            val b2 by bean { b1() }

            init {
                onDestroy(b0) { destroyed.add("b0") }
                onDestroy(b1) { destroyed.add("b1") }
                onDestroy(b2) { destroyed.add("b2") }
            }
        }

        "Destruction when creation is ongoing" {
            val destroyed = mutableListOf<String>()
            val mod0 = Kraftverk.start(lazy = true) {
                Mod0(destroyed)
            }
            thread { mod0 { b2 } } // Will take 2000 ms to instantiate
            Thread.sleep(500)
            mod0.stop()
            destroyed should containExactly("b2", "b1", "b0")
        }

        "Trying out mock" {
            val module = Kraftverk.manage { Mod0(mutableListOf()) }
            val b0 by module.mock { b0 }
            val b1 by module.mock { b1 }
            val b2 by module.spy { b2 }
            module.start()
            println(b0)
            println(b1)
            println(b2)
        }

        class Mod1 : Module() {
            val b0 by bean { 1 }
            val b1 by bean { 2 }
            val intList by bean { beans<Int>() }
        }

        "Trying out bean providers 1" {
            val module = Kraftverk.start { Mod1() }
            module { intList } should containExactly(1, 2)
        }

        "Trying out bean providers 2" {
            val module = Kraftverk.start { Mod1() }
            val configs = module.beanProviders.map { it.definition }
            configs shouldHaveSize 3
            with(configs[0]) {
                name shouldBe "b0"
                lazy shouldBe false
                this.type shouldBe Int::class
            }
            with(configs[1]) {
                name shouldBe "b1"
                lazy shouldBe false
                this.type shouldBe Int::class
            }
            with(configs[2]) {
                name shouldBe "intList"
                lazy shouldBe false
                this.type shouldBe List::class
            }
        }

        "Trying out bean processors" {
            val module = Kraftverk.manage { Mod1() }
            module.registerBeanProcessor<Int> { it + 7 }
            module.start()
            module { intList } should containExactly(8, 9)
        }

        "Getting a bean before the module has been started should fail" {
            val module = Kraftverk.manage { Mod1() }
            val ex = shouldThrow<IllegalStateException> {
                module { b0 }
            }
            ex.message shouldBe "Expected state to be 'Running' but was 'Configurable'"
        }

        "Customizing the module after it has been started should fail" {
            val module = Kraftverk.start { Mod1() }
            val ex = shouldThrow<IllegalStateException> {
                module.customize { }
            }
            ex.message shouldBe "Expected state to be 'Configurable' but was 'Running'"
        }

        "A nested construction operation like 'OnCreate' should fail if the module has been started" {
            val module = Kraftverk.start { Mod1() }
            val ex = shouldThrow<IllegalStateException> {
                module.start {
                    onCreate(b0) {
                        onCreate(b1) {} // <-- Should fail
                    }
                }
            }
            ex.message shouldBe "Expected state to be 'Configurable' but was 'Running'"
        }

        // This declaration is to ensure that we don't break binding and provider covariance
        class CovariantModule : Module() {
            val bean0: Bean<Widget> by bean {
                widget
            }
            val binding0: Binding<Widget> = bean0
            val beanProvider: BeanProvider<Widget> = bean0.provider
            val provider: Provider<Widget> = beanProvider
        }
    }

    private fun verifyThatNoBeansAreInstantiated() {
        verify {
            widgetFactory wasNot Called
        }
    }

    private fun verifyThatAllBeansAreInstantiated() {
        verifySequence {
            widgetFactory.createWidget()
            widget.start()
            widgetFactory.createWidget(widget)
            childWidget.start()
        }
    }

    interface Widget {
        fun start()
        fun stop()
    }

    interface WidgetFactory {
        fun createWidget(): Widget
        fun createWidget(parent: Widget): Widget
    }

    private inline fun <M : Module, reified T : Any> Managed<M>.mock(noinline bean: M.() -> Bean<T>):
            ReadOnlyProperty<Any?, T> {
        customize {
            bind(bean()) to { mockk() }
        }
        return get(bean)
    }

    private inline fun <M : Module, reified T : Any> Managed<M>.spy(noinline bean: M.() -> Bean<T>):
            ReadOnlyProperty<Any?, T> {
        customize {
            bind(bean()) to { spyk(proceed()) }
        }
        return get(bean)
    }

    private inline fun <reified T : Any> BeanDeclaration.beans(): List<T> =
        beanProviders.filter { it.type.isSubclassOf(T::class) }.map { it.get() as T }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> Managed<*>.registerBeanProcessor(crossinline block: (T) -> T) {
        val processor = object : BeanProcessor {
            override fun <A : Any> process(bean: BeanDefinition<A>) =
                if (bean.type.isSubclassOf(T::class)) {
                    bean.copy {
                        val t: T = bean.instance() as T
                        val a: T = block(t)
                        a as A
                    }
                } else bean
        }
        registerProcessor(processor)
    }
}
