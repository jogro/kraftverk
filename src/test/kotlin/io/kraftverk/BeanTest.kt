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
import io.mockk.*
import kotlin.concurrent.thread

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
            Kraftverk.manage { AppModule() }
            verifyThatAllBeansAreInstantiated()
        }

        "bean instantiation is eager when specified for the beans" {
            Kraftverk.manage(lazy = true) { AppModule(lazy = false) }
            verifyThatAllBeansAreInstantiated()
        }

        "bean instantiation is lazy when managed lazily" {
            Kraftverk.manage(lazy = true) { AppModule() }
            verifyThatNoBeansAreInstantiated()
        }

        "bean instantiation is lazy when specified for the beans" {
            Kraftverk.manage { AppModule(lazy = true) }
            verifyThatNoBeansAreInstantiated()
        }

        "Extracting a bean returns expected value" {
            val app = Kraftverk.manage { AppModule() }
            app.get { widget } shouldBe widget
            app.get { childWidget } shouldBe childWidget
        }

        "Extracting a bean does not propagate to other beans if not necessary" {
            val app: Managed<AppModule> = Kraftverk.manage(lazy = true) { AppModule() }
            app.get { widget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "Extracting a bean propagates to other beans if necessary" {
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            app.get { childWidget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
                widgetFactory.newWidget(widget)
                childWidget.start()
            }
        }

        "Extracting a bean results in one instantiation even if many invocations" {
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            repeat(3) { app.get { widget } }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "bean on create invokes next properly" {
            Kraftverk.manage {
                AppModule().apply {
                    onCreate(widget) { next() }
                }
            }
            verifySequence {
                widget.start()
            }
        }

        "bean on create inhibits next properly" {
            Kraftverk.manage {
                AppModule().apply {
                    onCreate(widget) { }
                }
            }
            verify(exactly = 1) {
                widgetFactory.newWidget()
                widget wasNot Called
            }
        }

        "bean on destroy invokes next properly" {
            val app = Kraftverk.manage {
                AppModule().apply {
                    onDestroy(widget) { next() }
                    onDestroy(childWidget) { next() }
                }
            }
            clearMocks(widget, childWidget)
            app.destroy()
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
            val widget by app { widget }
            widget shouldBe replacement
        }

        "Refreshing the module stops and starts the bindings by default" {
            val app = Kraftverk.manage { AppModule() }
            clearAllMocks()
            every { widgetFactory.newWidget() } returns widget
            every { widgetFactory.newWidget(widget) } returns childWidget
            app.refresh()
            verifySequence {
                childWidget.stop()
                widget.stop()
                widgetFactory.newWidget()
                widget.start()
                widgetFactory.newWidget(widget)
                childWidget.start()
            }
        }

        "Refreshing the module affects nothing when refreshable is false" {
            val app = Kraftverk.manage(refreshable = false) { AppModule() }
            clearAllMocks()
            every { widgetFactory.newWidget() } returns widget
            every { widgetFactory.newWidget(widget) } returns childWidget
            app.refresh()
            verify {
                childWidget wasNot Called
                widget wasNot Called
                widgetFactory wasNot Called
            }
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
            val mod0 = Kraftverk.manage(lazy = true) { Mod0(destroyed) }
            thread { mod0.get { b2 } } // Will take 2000 ms to instantiate
            Thread.sleep(500)
            mod0.destroy()
            destroyed should containExactly("b2", "b1", "b0")
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

}
