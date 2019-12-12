package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.*

class BeanTest : StringSpec() {

    private val widget = mockk<Widget>(relaxed = true)

    private val childWidget = mockk<Widget>(relaxed = true)

    private val widgetFactory = mockk<WidgetFactory>()

    inner class AppModule(private val lazy: Boolean? = null) : Module() {

        val widget by bean(lazy = this.lazy) {
            widgetFactory.newWidget()
        }

        val childWidget by bean(lazy = this.lazy) {
            widgetFactory.newWidget(widget())
        }

        init {
            onCreate(widget) { it.start() }
            onCreate(childWidget) { it.start() }
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

        "Bean instantiation is eager by default" {
            Container.start { AppModule() }
            verifyThatAllBeansAreInstantiated()
        }

        "Bean instantiation is eager when specified for the beans" {
            Container.start(lazy = true) { AppModule(lazy = false) }
            verifyThatAllBeansAreInstantiated()
        }

        "Bean instantiation is lazy when started lazily" {
            Container.start(lazy = true) { AppModule() }
            verifyThatNoBeansAreInstantiated()
        }

        "Bean instantiation is lazy when specified for the beans" {
            Container.start { AppModule(lazy = true) }
            verifyThatNoBeansAreInstantiated()
        }

        "Getting a bean returns expected value" {
            val app = Container.start { AppModule() }
            app.get { widget } shouldBe widget
            app.get { childWidget } shouldBe childWidget
        }

        "Getting a bean does not trigger creation of other beans if not necessary" {
            val app = Container.start(lazy = true) { AppModule() }
            app.get { widget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "Getting a bean propagates to other beans if necessary" {
            val app = Container.start(lazy = true) { AppModule() }
            app.get { childWidget }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
                widgetFactory.newWidget(widget)
                childWidget.start()
            }
        }

        "Getting a bean results in one instantiation even if many invocations" {
            val app = Container.start(lazy = true) { AppModule() }
            repeat(3) { app.get { widget } }
            verifySequence {
                widgetFactory.newWidget()
                widget.start()
            }
        }

        "Bean on create invokes next properly" {
            Container.start {
                AppModule().apply {
                    onCreate(widget) { next() }
                }
            }
            verifySequence {
                widget.start()
            }
        }

        "Bean on create inhibits next properly" {
            Container.start {
                AppModule().apply {
                    onCreate(widget) { }
                }
            }
            verify(exactly = 1) {
                widgetFactory.newWidget()
                widget wasNot Called
            }
        }

        "Bean on destroy invokes next properly" {
            val app = Container.start {
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
            val app = Container.start {
                AppModule().apply {
                    bind(widget) to { widgetFactory.newWidget(next()) }
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
