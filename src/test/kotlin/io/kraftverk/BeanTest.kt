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
import io.kraftverk.binding.Binding
import io.kraftverk.binding.Component
import io.kraftverk.binding.provider
import io.kraftverk.common.ComponentDefinition
import io.kraftverk.common.ComponentProcessor
import io.kraftverk.declaration.ComponentDeclaration
import io.kraftverk.managed.Managed
import io.kraftverk.managed.componentProviders
import io.kraftverk.managed.config
import io.kraftverk.managed.get
import io.kraftverk.managed.invoke
import io.kraftverk.managed.registerProcessor
import io.kraftverk.managed.start
import io.kraftverk.managed.stop
import io.kraftverk.module.Module
import io.kraftverk.module.bean
import io.kraftverk.module.bind
import io.kraftverk.module.shape
import io.kraftverk.provider.ComponentProvider
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

    private val gadget = mockk<Gadget>(relaxed = true)

    private val childGadget = mockk<Gadget>(relaxed = true)

    private val gadgetFactory = mockk<GadgetFactory>()

    inner class AppModule(private val lazy: Boolean? = null) : Module() {

        val childGadget by bean(lazy = this.lazy) {
            gadgetFactory.createGadget(gadget())
        }

        val gadget by bean(lazy = this.lazy) {
            gadgetFactory.createGadget()
        }

        init {
            shape(gadget) { w ->
                lifecycle {
                    onCreate { w.start() }
                    onDestroy { w.stop() }
                }
            }
            shape(childGadget) { cw ->
                lifecycle {
                    onCreate { cw.start() }
                    onDestroy { cw.stop() }
                }
            }
            /*
            onCreate(childGadget) { it.start() }
            onCreate(gadget) { it.start() }
            onDestroy(gadget) { it.stop() }
            onDestroy(childGadget) { it.stop() }
            */
        }
    }

    override fun beforeTest(testCase: TestCase) {
        clearAllMocks()
        every { gadgetFactory.createGadget() } returns gadget
        every { gadgetFactory.createGadget(gadget) } returns childGadget
    }

    init {

        "component instantiation is eager by default" {
            Kraftverk.start { AppModule() }
            verifyThatAllComponentsAreInstantiated()
        }

        "component instantiation is eager when specified for the component" {
            Kraftverk.start(lazy = true) {
                AppModule(lazy = false)
            }
            verifyThatAllComponentsAreInstantiated()
        }

        "component instantiation is lazy when managed lazily" {
            Kraftverk.start(lazy = true) {
                AppModule()
            }
            verifyThatNoComponentsAreInstantiated()
        }

        "component instantiation is lazy when specified for the component" {
            Kraftverk.start { AppModule(lazy = true) }
            verifyThatNoComponentsAreInstantiated()
        }

        "Extracting a component returns expected value" {
            val app = Kraftverk.start { AppModule() }
            app { gadget } shouldBe gadget
            app { childGadget } shouldBe childGadget
        }

        "Components provided by 'get' should not be instantiated until referenced" {
            val app = Kraftverk.manage { AppModule() }

            val w by app.get { gadget }
            val c by app.get { childGadget }

            verifyThatNoComponentsAreInstantiated()

            app.start()

            w shouldBe gadget
            c shouldBe childGadget
        }

        "Extracting a component does not propagate to other components if not necessary" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            app { gadget }
            verifySequence {
                gadgetFactory.createGadget()
                gadget.start()
            }
        }

        "Customize can be used to replace a component and inhibit its onCreate" {
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            val replacement = mockk<Gadget>(relaxed = true)
            app.config {
                bind(gadget) to { replacement }
                shape(gadget) {
                    lifecycle {
                        onCreate { }
                    }
                }
            }
            app.start()
            verifySequence {
                gadgetFactory wasNot Called
                replacement wasNot Called
            }
            app { gadget } shouldBe replacement
        }

        "Extracting a component propagates to other components if necessary" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            app { childGadget }
            verifySequence {
                gadgetFactory.createGadget()
                gadget.start()
                gadgetFactory.createGadget(gadget)
                childGadget.start()
            }
        }

        "Extracting a component results in one instantiation even if many invocations" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            repeat(3) {
                app { gadget }
                Unit
            }
            verifySequence {
                gadgetFactory.createGadget()
                gadget.start()
            }
        }

        "component on create invokes 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                shape(gadget) {
                    lifecycle {
                        onCreate { proceed() }
                    }
                }
            }
            verifySequence {
                gadget.start()
            }
        }

        "component on create inhibits 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                // onCreate(gadget) { }
                shape(gadget) {
                    lifecycle {
                        onCreate { }
                    }
                }
            }
            verify(exactly = 1) {
                gadgetFactory.createGadget()
                gadget wasNot Called
            }
        }

        "component on destroy invokes 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                shape(gadget) {
                    lifecycle {
                        onDestroy { proceed() }
                    }
                }
                shape(childGadget) {
                    lifecycle {
                        onDestroy { proceed() }
                    }
                }
                // onDestroy(gadget) { proceed() }
                // onDestroy(childGadget) { proceed() }
            }
            clearMocks(gadget, childGadget)
            app.stop()
            verifySequence {
                childGadget.stop()
                gadget.stop()
            }
        }

        "component on destroy inhibits 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                shape(gadget) {
                    lifecycle {
                        onDestroy { }
                    }
                }
                shape(childGadget) {
                    lifecycle {
                        onDestroy { }
                    }
                }
                // onDestroy(gadget) { }
                // onDestroy(childGadget) { }
            }
            clearMocks(gadget, childGadget)
            app.stop()
            verifySequence {
                childGadget wasNot Called
                gadget wasNot Called
            }
        }

        "Binding a component does a proper replace 1" {
            val replacement = mockk<Gadget>(relaxed = true)
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            app.start {
                bind(gadget) to { replacement }
            }
            app { gadget } shouldBe replacement

            verifyThatNoComponentsAreInstantiated()
        }

        "Binding a component does a proper replace 2" {
            val replacement = mockk<Gadget>(relaxed = true)
            val app = Kraftverk.manage(lazy = true) { AppModule() }
            app.start {
                bind(childGadget) to { replacement }
            }
            app { childGadget } shouldBe replacement

            verifyThatNoComponentsAreInstantiated()
        }

        class Mod0(val destroyed: MutableList<String>) : Module() {

            val b1 by bean {
                b0()
                Thread.sleep(2000)
            }
            val b0 by bean {}
            val b2 by bean { b1() }

            init {
                shape(b0) {
                    lifecycle {
                        onDestroy { destroyed.add("b0") }
                    }
                }
                shape(b1) {
                    lifecycle {
                        onDestroy { destroyed.add("b1") }
                    }
                }
                shape(b2) {
                    lifecycle {
                        onDestroy { destroyed.add("b2") }
                    }
                }
                // onDestroy(b0) { destroyed.add("b0") }
                // onDestroy(b1) { destroyed.add("b1") }
                // onDestroy(b2) { destroyed.add("b2") }
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
            val intList by bean { components<Int>() }
        }

        "Trying out component providers 1" {
            val module = Kraftverk.start { Mod1() }
            module { intList } should containExactly(1, 2)
        }

        "Trying out component providers 2" {
            val module = Kraftverk.start { Mod1() }
            val configs = module.componentProviders.map { it.definition }
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

        "Trying out component processors" {
            val module = Kraftverk.manage { Mod1() }
            module.registerComponentProcessor<Int> { it + 7 }
            module.start()
            module { intList } should containExactly(8, 9)
        }

        "Getting a component before the module has been started should fail" {
            val module = Kraftverk.manage { Mod1() }
            val ex = shouldThrow<IllegalStateException> {
                module { b0 }
            }
            ex.message shouldBe "Expected state to be 'Running' but was 'Configurable'"
        }

        "Customizing the module after it has been started should fail" {
            val module = Kraftverk.start { Mod1() }
            val ex = shouldThrow<IllegalStateException> {
                module.config { }
            }
            ex.message shouldBe "Expected state to be 'Configurable' but was 'Running'"
        }

        /*
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
        */

        // This declaration is to ensure that we don't break binding and provider covariance
        class CovariantModule : Module() {
            val component0: Component<Gadget, Gadget> by bean { gadget }
            val binding0: Binding<Gadget> = component0
            val componentProvider: ComponentProvider<Gadget, Gadget> = component0.provider
            val provider: Provider<Gadget> = componentProvider
        }
    }

    private fun verifyThatNoComponentsAreInstantiated() {
        verify {
            gadgetFactory wasNot Called
        }
    }

    private fun verifyThatAllComponentsAreInstantiated() {
        verifySequence {
            gadgetFactory.createGadget()
            gadget.start()
            gadgetFactory.createGadget(gadget)
            childGadget.start()
        }
    }

    interface Gadget {
        fun start()
        fun stop()
    }

    interface GadgetFactory {
        fun createGadget(): Gadget
        fun createGadget(parent: Gadget): Gadget
    }

    private inline fun <M : Module, reified T : Any> Managed<M>.mock(noinline component: M.() -> Component<T, *>):
            ReadOnlyProperty<Any?, T> {
        config {
            bind(component()) to { mockk() }
        }
        return get(component)
    }

    private inline fun <M : Module, reified T : Any> Managed<M>.spy(noinline component: M.() -> Component<T, *>):
            ReadOnlyProperty<Any?, T> {
        config {
            bind(component()) to { spyk(proceed()) }
        }
        return get(component)
    }

    private inline fun <reified T : Any> ComponentDeclaration.components(): List<T> =
        componentProviders.filter { it.type.isSubclassOf(T::class) }.map { it.get() as T }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> Managed<*>.registerComponentProcessor(crossinline block: (T) -> T) {
        val processor = object : ComponentProcessor {
            override fun <A : Any, B : Any> process(component: ComponentDefinition<A, B>) =
                if (component.type.isSubclassOf(T::class)) {
                    component.copy {
                        val t: T = component.instance() as T
                        val a: T = block(t)
                        a as A
                    }
                } else component
        }
        registerProcessor(processor)
    }
}
