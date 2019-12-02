package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.*


class AppBeanTest : StringSpec() {

    val widget = mockk<Widget>(relaxed = true)

    val childWidget = mockk<Widget>(relaxed = true)

    val widgetFactory = mockk<WidgetFactory>()

    inner class TestModule(private val lazy: Boolean? = null) : Module() {

        val widget by bean(lazy = this.lazy) {
            widgetFactory.newWidget()
        }

        val childWidget by bean(lazy = this.lazy) {
            widgetFactory.newWidget(widget())
        }

        init {
            onStart(widget) { it.start() }
            onStart(childWidget) { it.start() }
            onStop(widget) { it.stop() }
            onStop(childWidget) { it.stop() }
        }
    }

    override fun beforeTest(testCase: TestCase) {
        clearMocks(widgetFactory, widget, childWidget)
        every { widgetFactory.newWidget() } returns widget
        every { widgetFactory.newWidget(widget) } returns childWidget
    }

    init {

        "Bean instantiation is eager by default" {
            App.start { TestModule() }
            verifyThatAllBeansAreInstantiated()
        }

        "Bean instantiation is eager when specified for the beans" {
            App.startLazy { TestModule(lazy = false) }
            verifyThatAllBeansAreInstantiated()
        }

        "Bean instantiation is lazy when started lazily" {
            App.startLazy { TestModule() }
            verifyThatNoBeansAreInstantiated()
        }

        "Bean instantiation is lazy when specified for the beans" {
            App.start { TestModule(lazy = true) }
            verifyThatNoBeansAreInstantiated()
        }

        "Getting a bean returns expected value" {
            val app = App.start { TestModule() }
            app.get { widget } shouldBe widget
            app.get { childWidget } shouldBe childWidget
        }

        "Getting a bean does not trigger creation of other beans if not necessary" {
            val app = App.startLazy { TestModule() }
            app.get { widget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "Getting a bean propagates to other beans if necessary" {
            val app = App.startLazy { TestModule() }
            app.get { childWidget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
                widgetFactory.newWidget(widget)
                childWidget.start()
            }
        }

        "Getting a bean results in one instantiation even if many invocations" {
            val app = App.startLazy { TestModule() }
            repeat(3) { app.get { widget } }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "Bean on start invokes next properly" {
            App.start {
                TestModule().apply {
                    onStart(widget) { next() }
                }
            }
            verifySequence {
                widget.start()
            }
        }

        "Bean on start inhibits next properly" {
            App.start {
                TestModule().apply {
                    onStart(widget) { }
                }
            }
            verify(exactly = 1) {
                widgetFactory.newWidget()
                widget wasNot Called
            }
        }

        "Bean on stop invokes next properly" {
            val app = App.start {
                TestModule().apply {
                    onStop(widget) { next() }
                    onStop(childWidget) { next() }
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
            val app = App.start {
                TestModule().apply {
                    bind(widget) to {
                        widgetFactory.newWidget(next())
                    }
                }
            }
            app.get { widget } shouldBe replacement
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
