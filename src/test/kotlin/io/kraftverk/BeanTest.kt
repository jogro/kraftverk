/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.matchers.collections.containExactly
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kraftverk.binding.Bean
import io.kraftverk.managed.Managed
import io.kraftverk.managed.operations.customize
import io.kraftverk.managed.operations.get
import io.kraftverk.managed.operations.invoke
import io.kraftverk.managed.operations.start
import io.kraftverk.managed.operations.stop
import io.kraftverk.module.Module
import io.kraftverk.module.operations.bean
import io.kraftverk.module.operations.bind
import io.kraftverk.module.operations.onCreate
import io.kraftverk.module.operations.onDestroy
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

class BeanTest : StringSpec() {

    private val widget = mockk<Widget>(relaxed = true)

    private val childWidget = mockk<Widget>(relaxed = true)

    private val widgetFactory = mockk<WidgetFactory>()

    inner class AppModule(private val lazy: Boolean? = null) : Module() {

        val childWidget by bean(lazy = this.lazy) {
            widgetFactory.newWidget(widget())
        }

        val widget by bean(lazy = this.lazy) {
            widgetFactory.newWidget()
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
        every { widgetFactory.newWidget() } returns widget
        every { widgetFactory.newWidget(widget) } returns childWidget
    }

    init {

        "bean instantiation is eager by default" {
            val app = Kraftverk.manage {
                AppModule()
            }
            app.start()
            verifyThatAllBeansAreInstantiated()
        }

        "bean instantiation is eager when specified for the dsl" {
            val app = Kraftverk.manage(lazy = true) {
                AppModule(lazy = false)
            }
            app.start()
            verifyThatAllBeansAreInstantiated()
        }

        "bean instantiation is lazy when managed lazily" {
            val app = Kraftverk.manage(lazy = true) {
                AppModule()
            }
            app.start()
            verifyThatNoBeansAreInstantiated()
        }

        "bean instantiation is lazy when specified for the dsl" {
            val app = Kraftverk.manage {
                AppModule(lazy = true)
            }
            app.start()
            verifyThatNoBeansAreInstantiated()
        }

        "Extracting a bean returns expected value" {
            val app = Kraftverk.manage { AppModule() }

            val w by app.get { widget }
            val c by app.get { childWidget }

            app.start()

            w shouldBe widget
            c shouldBe childWidget
        }

        "Extracting a bean does not propagate to other dsl if not necessary" {
            val app = Kraftverk.manage(lazy = true) {
                AppModule()
            }
            app.start()
            app { widget }

            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "Extracting a bean propagates to other dsl if necessary" {
            val app = Kraftverk.manage(lazy = true) {
                AppModule()
            }
            app.start()
            app { childWidget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
                widgetFactory.newWidget(widget)
                childWidget.start()
            }
        }

        "Extracting a bean results in one instantiation even if many invocations" {
            val app = Kraftverk.manage(lazy = true) {
                AppModule()
            }
            app.start()
            repeat(3) { app { widget } }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "bean on create invokes next properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onCreate(widget) { next() }
            }
            verifySequence {
                widget.start()
            }
        }

        "bean on create inhibits next properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onCreate(widget) { }
            }
            verify(exactly = 1) {
                widgetFactory.newWidget()
                widget wasNot Called
            }
        }

        "bean on destroy invokes next properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                onDestroy(widget) { next() }
                onDestroy(childWidget) { next() }
            }
            clearMocks(widget, childWidget)
            app.stop()
            verifySequence {
                childWidget.stop()
                widget.stop()
            }
        }

        "Binding a bean does a proper replace" {
            val replacement = mockk<Widget>(relaxed = true)
            every { widgetFactory.newWidget(widget) } returns replacement
            every { widgetFactory.newWidget(replacement) } returns childWidget
            val app = Kraftverk.manage {
                AppModule().apply {
                    bind(widget) to { widgetFactory.newWidget(next()) }
                }
            }
            app.start()
            val widget by app.get { widget }
            widget shouldBe replacement
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
            val mod0 = Kraftverk.manage(lazy = true) {
                Mod0(destroyed)
            }
            mod0.start()
            thread { mod0 { b2 } } // Will take 2000 ms to instantiate
            Thread.sleep(500)
            mod0.stop()
            destroyed should containExactly("b2", "b1", "b0")
        }

        "Trying out managed use case" {
            val module = Kraftverk.manage { Mod0(mutableListOf()) }
            val b0 by module.mock { b0 }
            val b1 by module.mock { b1 }
            val b2 by module.spy { b2 }
            module.start()
            println(b0)
            println(b1)
            println(b2)
        }
    }

    private fun verifyThatNoBeansAreInstantiated() {
        verify {
            widgetFactory wasNot Called
        }
    }

    private fun verifyThatAllBeansAreInstantiated() {
        verifySequence {
            widgetFactory.newWidget()
            widget.start()
            widgetFactory.newWidget(widget)
            childWidget.start()
        }
    }

    interface Widget {
        fun start()
        fun stop()
    }

    interface WidgetFactory {
        fun newWidget(): Widget
        fun newWidget(parent: Widget): Widget
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
            bind(bean()) to { spyk(next()) }
        }
        return get(bean)
    }
}
