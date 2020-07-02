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
import io.kraftverk.common.BeanDefinition
import io.kraftverk.common.BeanProcessor
import io.kraftverk.declaration.BeanDeclaration
import io.kraftverk.managed.Managed
import io.kraftverk.managed.addProcessor
import io.kraftverk.managed.beanProviders
import io.kraftverk.managed.configure
import io.kraftverk.managed.get
import io.kraftverk.managed.invoke
import io.kraftverk.managed.start
import io.kraftverk.managed.stop
import io.kraftverk.module.Module
import io.kraftverk.module.bean
import io.kraftverk.module.bind
import io.kraftverk.module.configure
import io.kraftverk.module.sink
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

        val config by sink<StringBuffer>()

        init {
            configure(gadget) { w ->
                lifecycle {
                    onCreate { w.start() }
                    onDestroy { w.stop() }
                }
            }
            configure(childGadget) { cw ->
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
            app { gadget } shouldBe gadget
            app { childGadget } shouldBe childGadget
        }

        "Beans provided by 'get' should not be instantiated until referenced" {
            val app = Kraftverk.manage { AppModule() }

            val w by app.get { gadget }
            val c by app.get { childGadget }

            verifyThatNoBeansAreInstantiated()

            app.start()

            w shouldBe gadget
            c shouldBe childGadget
        }

        "Extracting a bean does not propagate to other beans if not necessary" {
            val app = Kraftverk.start(lazy = true) {
                AppModule()
            }
            app { gadget }
            verifySequence {
                gadgetFactory.createGadget()
                gadget.start()
            }
        }

        "Customize can be used to replace a bean and inhibit its onCreate" {
            val app = Kraftverk.manage { AppModule() }
            val replacement = mockk<Gadget>(relaxed = true)
            app.configure {
                bind(gadget) to { replacement }
                configure(gadget) {
                    lifecycle {
                        onCreate { }
                    }
                }
            }
            app.start(lazy = true)
            verifySequence {
                gadgetFactory wasNot Called
                replacement wasNot Called
            }
            app { gadget } shouldBe replacement
        }

        "Extracting a bean propagates to other beans if necessary" {
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

        "Extracting a bean results in one instantiation even if many invocations" {
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

        "bean on create invokes 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                configure(gadget) {
                    lifecycle {
                        onCreate { proceed() }
                    }
                }
            }
            verifySequence {
                gadget.start()
            }
        }

        "bean on create inhibits 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                // onCreate(gadget) { }
                configure(gadget) {
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

        "bean on destroy invokes 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                configure(gadget) {
                    lifecycle {
                        onDestroy { proceed() }
                    }
                }
                configure(childGadget) {
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

        "bean on destroy inhibits 'proceed' properly" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                configure(gadget) {
                    lifecycle {
                        onDestroy { }
                    }
                }
                configure(childGadget) {
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

        "Binding a bean does a proper replace 1" {
            val replacement = mockk<Gadget>(relaxed = true)
            val app = Kraftverk.manage { AppModule() }
            app.start(lazy = true) {
                bind(gadget) to { replacement }
            }
            app { gadget } shouldBe replacement

            verifyThatNoBeansAreInstantiated()
        }

        "Binding a bean does a proper replace 2" {
            val replacement = mockk<Gadget>(relaxed = true)
            val app = Kraftverk.manage { AppModule() }
            app.start(lazy = true) {
                bind(childGadget) to { replacement }
            }
            app { childGadget } shouldBe replacement

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
                configure(b0) {
                    lifecycle {
                        onDestroy { destroyed.add("b0") }
                    }
                }
                configure(b1) {
                    lifecycle {
                        onDestroy { destroyed.add("b1") }
                    }
                }
                configure(b2) {
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
                lazy shouldBe null
                this.type shouldBe Int::class
            }
            with(configs[1]) {
                name shouldBe "b1"
                lazy shouldBe null
                this.type shouldBe Int::class
            }
            with(configs[2]) {
                name shouldBe "intList"
                lazy shouldBe null
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
                module.configure { }
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
            val bean0: Bean<Gadget> by bean { gadget }
            val binding0: Binding<Gadget> = bean0
            // val beanProvider: BeanProvider<Gadget, Gadget> = bean0.provider
            // val provider: Provider<Gadget> = beanProvider
        }
    }

    private fun verifyThatNoBeansAreInstantiated() {
        verify {
            gadgetFactory wasNot Called
        }
    }

    private fun verifyThatAllBeansAreInstantiated() {
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

    private inline fun <M : Module, reified T : Any> Managed<M>.mock(noinline bean: M.() -> Bean<T>):
            ReadOnlyProperty<Any?, T> {
        configure {
            val c: Bean<T> = bean()
            bind(c) to { mockk() }
        }
        return get(bean)
    }

    private inline fun <M : Module, reified T : Any> Managed<M>.spy(noinline bean: M.() -> Bean<T>):
            ReadOnlyProperty<Any?, T> {
        configure {
            val c: Bean<T> = bean()
            bind(c) to { spyk(proceed()) }
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
        addProcessor(processor)
    }
}
