/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.extensions.system.withEnvironment
import io.kotlintest.extensions.system.withSystemProperties
import io.kotlintest.matchers.collections.containExactly
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kraftverk.declaration.ValueDeclaration
import io.kraftverk.env.environment
import io.kraftverk.managed.get
import io.kraftverk.managed.invoke
import io.kraftverk.managed.start
import io.kraftverk.module.ChildModule
import io.kraftverk.module.Module
import io.kraftverk.module.bind
import io.kraftverk.module.import
import io.kraftverk.module.int
import io.kraftverk.module.module
import io.kraftverk.module.ref
import io.kraftverk.module.string
import io.kraftverk.module.value
import io.kraftverk.provider.get
import io.kraftverk.provider.type
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import java.time.LocalDate
import kotlin.reflect.full.isSubclassOf

class ValueTest : StringSpec() {

    private val principal = "ergo"

    private val valueObject1 = ValueObject("051")

    private val valueObject2 = ValueObject("052", valueObject1)

    private val valueObject3 = ValueObject("053")

    private val valueObject4 = ValueObject("054")

    private val valueObject5 = ValueObject("055")

    private val valueObjectFactory = spyk(ValueObjectFactory())

    private val userName = "Kuno"

    private val password = "Kuno123"

    private class UserModule : Module() {
        val userName by string()
        val password by string(secret = true)
    }

    private inner class ValueModule : Module() {

        val val2 by value {
            valueObjectFactory.createValue(it.toString(), val1())
        }

        val val1 by value {
            valueObjectFactory.createValue(it.toString())
        }

        val val3 by value {
            valueObjectFactory.createValue(it.toString())
        }

        val val4 by value(default = valueObject4.value) {
            valueObjectFactory.createValue(it.toString())
        }

        val val5 by value(name = "xyz.val5") {
            valueObjectFactory.createValue(it.toString())
        }

        val user by this.module { UserModule() }
    }

    private inner class AppModule : Module() {
        val principal by string()
        val values by this.module { ValueModule() }
    }

    override fun beforeTest(testCase: TestCase) {
        clearMocks(valueObjectFactory)
    }

    init {

        "Value instantiation is eager by default" {
            Kraftverk.start { AppModule() }
            verifyThatAllValuesAreInstantiated()
        }

        "Value instantiation is lazy when specified for the container" {
            Kraftverk.start(lazy = true) { AppModule() }
            verifyThatNoValuesAreInstantiated()
        }

        "Extracting a value returns expected value" {
            val app = Kraftverk.start { AppModule() }
            app { principal } shouldBe principal
            app { values.val1 } shouldBe valueObject1
            app { values.val2 } shouldBe valueObject2
            app { values.val3 } shouldBe valueObject3
            app { values.val4 } shouldBe valueObject4
            app { values.val5 } shouldBe valueObject5
            app { values.user.userName } shouldBe userName
            app { values.user.password } shouldBe password
        }

        "Extracting a value does not propagate to other values if not necessary" {
            val app = Kraftverk.start(lazy = true) { AppModule() }
            app { values.val1 }
            verifySequence {
                valueObjectFactory.createValue(valueObject1.value)
            }
        }

        "Extracting a value propagates to other values if necessary" {
            val app = Kraftverk.start(lazy = true) { AppModule() }
            app { values.val2 }
            verifySequence {
                valueObjectFactory.createValue(valueObject1.value)
                valueObjectFactory.createValue(valueObject2.value, valueObject1)
            }
        }

        "Extracting a value results in one instantiation even if many invocations" {
            val app = Kraftverk.start(lazy = true) { AppModule() }
            repeat(3) {
                app { values.val1 }
                Unit
            }
            verifySequence {
                valueObjectFactory.createValue(valueObject1.value)
            }
        }

        "Binding a value does a proper replace" {
            val app = Kraftverk.manage { AppModule() }
            app.start {
                bind(values.val1) to { valueObjectFactory.createValue("Kalle", proceed()) }
            }
            app { values.val1 } shouldBe ValueObject("Kalle", valueObject1)
        }

        "Values can be overridden by environment vars and system properties" {
            val po1 = ValueObject("SET1")
            val po2 = ValueObject("SET2", po1)
            withEnvironment("VALUES_VAL1" to po1.value) {
                withSystemProperties("values.val2" to po2.value) {
                    val app = Kraftverk.start { AppModule() }
                    val val1 by app.get { values.val1 }
                    val val2 by app.get { values.val2 }
                    val1 shouldBe po1
                    val2 shouldBe po2
                }
            }
        }

        "Values should be overridden when using profiles 1" {
            withSystemProperties("kraftverk.active.profiles" to "prof2, prof1") {
                val env = environment()
                Kraftverk.start(env = env) {
                    AppModule()
                }
                verifySequence {
                    valueObjectFactory.createValue(valueObject1.value)
                    valueObjectFactory.createValue("152", valueObject1)
                    valueObjectFactory.createValue("253")
                    valueObjectFactory.createValue(valueObject4.value)
                    valueObjectFactory.createValue(valueObject5.value)
                }
                env.profiles.shouldContainExactly("prof2", "prof1")
            }
        }

        "Values should be overridden when using profiles 2" {
            val env = environment("prof2", "prof1")
            Kraftverk.start(env = env) {
                AppModule()
            }
            verifySequence {
                valueObjectFactory.createValue(valueObject1.value)
                valueObjectFactory.createValue("152", valueObject1)
                valueObjectFactory.createValue("253")
                valueObjectFactory.createValue(valueObject4.value)
                valueObjectFactory.createValue(valueObject5.value)
            }
            env.profiles.shouldContainExactly("prof2", "prof1")
        }

        "Using environment set method should update properties" {
            val app = Kraftverk.start(
                env = environment {
                    set("principal", "jonas")
                },
                module = {
                    AppModule()
                }
            )
            app { principal } shouldBe "jonas"
        }

        class Mod1 : Module() {
            val v0 by int(default = 9)
            val v1 by int(default = 10)
            val intList by bean { values<Int>() }
        }

        "Trying out value providers" {
            val module = Kraftverk.start { Mod1() }
            module { intList } should containExactly(9, 10)
        }

        class YamlModule : Module() {
            val host by string("server.host")
            val ports by list<Int>("server.ports")
            val poolSize by int("app.database.pool-size", default = 99)
            val startDate by localDate("app.start-date")
            val endDate by localDate("app.end-date", default = LocalDate.of(2020, 12, 12))
        }

        "Trying out yaml module" {
            val module = Kraftverk.start { YamlModule() }
            module { host } shouldBe "acme.com"
            module { ports } should containExactly(80, 8080)
            module { poolSize } shouldBe 20
            module { startDate } shouldBe LocalDate.of(2020, 2, 2)
            module { endDate } shouldBe LocalDate.of(2020, 12, 12)
        }

        "Trying out sub modules" {
            Kraftverk.start { Mod3() }
        }
    }

    private fun verifyThatNoValuesAreInstantiated() {
        verify {
            valueObjectFactory wasNot Called
        }
    }

    private fun verifyThatAllValuesAreInstantiated() {
        verifySequence {
            valueObjectFactory.createValue(valueObject1.value)
            valueObjectFactory.createValue(valueObject2.value, valueObject1)
            valueObjectFactory.createValue(valueObject3.value)
            valueObjectFactory.createValue(valueObject4.value)
            valueObjectFactory.createValue(valueObject5.value)
        }
    }

    class Mod3 : Module() {
        val sm1 by module { Sub1() }
        val b1 by bean { 1 }

        // val sm2 by module { Sub2() } // This shouldn't compile
    }

    class Mod4 : Module()

    class Sub1 : ChildModule<Mod3>() {
        val br1 by ref { b1 }
        val mako by import { sm1 }
        val b1 by bean { mako().br1 }
    }

    class Sub2 : ChildModule<Mod4>()

    private data class ValueObject(val value: String, val parent: ValueObject? = null)

    private class ValueObjectFactory {
        fun createValue(value: String) = ValueObject(value)
        fun createValue(value: String, parent: ValueObject) = ValueObject(value, parent)
    }

    private inline fun <reified T : Any> ValueDeclaration.values(): List<T> =
        valueProviders.filter { it.type.isSubclassOf(T::class) }.map { it.get() as T }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> Module.list(name: String) = value(name) { v -> v as List<T> }

    private fun Module.localDate(name: String? = null, default: LocalDate? = null) =
        value(name, default = default) { value ->
            if (value is LocalDate) value else LocalDate.parse(value.toString())
        }
}
