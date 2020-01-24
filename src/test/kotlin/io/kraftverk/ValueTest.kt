/*
 * Copyright 2019 Jonas Grönberg
 * Licensed under MIT: https://github.com/jogro/kraftverk/blob/master/LICENSE
 */

package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.extensions.system.withEnvironment
import io.kotlintest.extensions.system.withSystemProperties
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.*


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

    private class UserModule(lazy: Boolean? = null) : Module() {
        val userName by string(lazy = lazy)
        val password by string(lazy = lazy, secret = true)
    }

    private inner class ValueModule(private val lazy: Boolean? = null) : Module() {

        val val2 by value(lazy = this.lazy) {
            valueObjectFactory.newValue(it, val1())
        }

        val val1 by value(lazy = this.lazy) {
            valueObjectFactory.newValue(it)
        }

        val val3 by value(lazy = this.lazy) {
            valueObjectFactory.newValue(it)
        }

        val val4 by value(lazy = this.lazy, default = valueObject4.value) {
            valueObjectFactory.newValue(it)
        }

        val val5 by value(lazy = this.lazy, name = "xyz.val5") {
            valueObjectFactory.newValue(it)
        }

        val user by module { UserModule(lazy) }

    }

    private inner class AppModule(private val lazy: Boolean? = null) : Module() {
        val principal by string(lazy = lazy)
        val values by module { ValueModule(lazy) }
    }

    override fun beforeTest(testCase: TestCase) {
        clearMocks(valueObjectFactory)
    }

    init {

        "Value instantiation is eager by default" {
            Kraftverk.manage { AppModule() }
            verifyThatAllValuesAreInstantiated()
        }

        "Value instantiation is lazy when specified for the container" {
            Kraftverk.manage(lazy = true) { AppModule() }
            verifyThatNoValuesAreInstantiated()
        }

        "Value instantiation is eager when specified for the values" {
            Kraftverk.manage { AppModule(lazy = false) }
            verifyThatAllValuesAreInstantiated()
        }

        "Value instantiation is lazy when specified for the values" {
            Kraftverk.manage { AppModule(lazy = true) }
            verifyThatNoValuesAreInstantiated()
        }

        "Extracting a value returns expected value" {
            val app = Kraftverk.manage { AppModule() }
            app.get { principal } shouldBe principal
            app.get { values.val1 } shouldBe valueObject1
            app.get { values.val2 } shouldBe valueObject2
            app.get { values.val3 } shouldBe valueObject3
            app.get { values.val4 } shouldBe valueObject4
            app.get { values.val5 } shouldBe valueObject5
            app.get { values.user.userName } shouldBe userName
            app.get { values.user.password } shouldBe password
        }

        "Extracting a value does not propagate to other values if not necessary" {
            val app = Kraftverk.manage { AppModule(lazy = true) }
            app.get { values.val1 }
            verifySequence {
                valueObjectFactory.newValue(valueObject1.value)
            }
        }

        "Extracting a value propagates to other values if necessary" {
            val app = Kraftverk.manage { AppModule(lazy = true) }
            app.get { values.val2 }
            verifySequence {
                valueObjectFactory.newValue(valueObject1.value)
                valueObjectFactory.newValue(valueObject2.value, valueObject1)
            }
        }

        "Extracting a value results in one instantiation even if many invocations" {
            val app = Kraftverk.manage { AppModule(lazy = true) }
            repeat(3) { app.get { values.val1 } }
            verifySequence {
                valueObjectFactory.newValue(valueObject1.value)
            }
        }

        "Binding a value does a proper replace" {
            val app = Kraftverk.manage {
                AppModule().apply {
                    bind(values.val1) to { valueObjectFactory.newValue("Kalle", next()) }
                }
            }
            app.get { values.val1 } shouldBe ValueObject("Kalle", valueObject1)
        }

        "Values can be overridden by environment vars and system properties" {
            val po1 = ValueObject("SET1")
            val po2 = ValueObject("SET2", po1)
            withEnvironment("VALUES_VAL1" to po1.value) {
                withSystemProperties("values.val2" to po2.value) {
                    val app = Kraftverk.manage { AppModule() }
                    app.get { values.val1 } shouldBe po1
                    app.get { values.val2 } shouldBe po2
                }
            }
        }

        "Values should be overridden when using profiles 1" {
            withSystemProperties("kraftverk.active.profiles" to "prof2, prof1") {
                val env = Environment.standard()
                Kraftverk.manage(environment = env) { AppModule() }
                verifySequence {
                    valueObjectFactory.newValue(valueObject1.value)
                    valueObjectFactory.newValue("152", valueObject1)
                    valueObjectFactory.newValue("253")
                    valueObjectFactory.newValue(valueObject4.value)
                    valueObjectFactory.newValue(valueObject5.value)
                }
                env.profiles.shouldContainExactly("prof2", "prof1")
            }
        }

        "Values should be overridden when using profiles 2" {
            val env = Environment.standard(profiles = listOf("prof2", "prof1"))
            Kraftverk.manage(environment = env) { AppModule() }
            verifySequence {
                valueObjectFactory.newValue(valueObject1.value)
                valueObjectFactory.newValue("152", valueObject1)
                valueObjectFactory.newValue("253")
                valueObjectFactory.newValue(valueObject4.value)
                valueObjectFactory.newValue(valueObject5.value)
            }
            env.profiles.shouldContainExactly("prof2", "prof1")
        }

    }

    private fun verifyThatNoValuesAreInstantiated() {
        verify {
            valueObjectFactory wasNot Called
        }
    }

    private fun verifyThatAllValuesAreInstantiated() {
        verifySequence {
            valueObjectFactory.newValue(valueObject1.value)
            valueObjectFactory.newValue(valueObject2.value, valueObject1)
            valueObjectFactory.newValue(valueObject3.value)
            valueObjectFactory.newValue(valueObject4.value)
            valueObjectFactory.newValue(valueObject5.value)
        }
    }

    private data class ValueObject(val value: String, val parent: ValueObject? = null)

    private class ValueObjectFactory() {
        fun newValue(value: String) = ValueObject(value)
        fun newValue(value: String, parent: ValueObject) = ValueObject(value, parent)
    }

}