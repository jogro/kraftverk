package io.kraftverk

import io.kotlintest.TestCase
import io.kotlintest.extensions.system.withEnvironment
import io.kotlintest.extensions.system.withSystemProperties
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.*


class PropertyTest : StringSpec() {

    private val principal = "ergo"

    private val propertyObject1 = PropertyObject("051")

    private val propertyObject2 = PropertyObject("052", propertyObject1)

    private val propertyObject3 = PropertyObject("053")

    private val propertyObject4 = PropertyObject("054")

    private val propertyObject5 = PropertyObject("055")

    private val propertyObjectFactory = spyk(PropertyObjectFactory())

    private val userName = "Kuno"

    private val password = "Kuno123"

    private class UserModule(lazy: Boolean? = null) : Module() {
        val userName by stringProperty(lazy = lazy)
        val password by stringProperty(lazy = lazy, secret = true)
    }

    private inner class PropertiesModule(private val lazy: Boolean? = null) : Module() {

        val prop1 by property(lazy = this.lazy) {
            propertyObjectFactory.newValue(it)
        }

        val prop2 by property(lazy = this.lazy) {
            propertyObjectFactory.newValue(it, prop1())
        }

        val prop3 by property(lazy = this.lazy) {
            propertyObjectFactory.newValue(it)
        }

        val prop4 by property(lazy = this.lazy, defaultValue = propertyObject4.value) {
            propertyObjectFactory.newValue(it)
        }

        val prop5 by property(lazy = this.lazy, name = "xyz.prop5") {
            propertyObjectFactory.newValue(it)
        }

        val userModule by module { UserModule(lazy) }

    }

    private inner class AppModule(private val lazy: Boolean? = null) : Module() {
        val principal by stringProperty(lazy = lazy)
        val props by module { PropertiesModule(lazy) }
    }

    override fun beforeTest(testCase: TestCase) {
        clearMocks(propertyObjectFactory)
    }

    init {

        "Property instantiation is eager by default" {
            Container.start { AppModule() }
            verifyThatAllPropertiesAreInstantiated()
        }

        "Property instantiation is lazy when using App.startLazy" {
            Container.start(lazy = true) { AppModule() }
            verifyThatNoPropertiesAreInstantiated()
        }

        "Property instantiation is eager when specified for the properties" {
            Container.start { AppModule(lazy = false) }
            verifyThatAllPropertiesAreInstantiated()
        }

        "Property instantiation is lazy when specified for the properties" {
            Container.start { AppModule(lazy = true) }
            verifyThatNoPropertiesAreInstantiated()
        }

        "Getting a property returns expected value" {
            val app = Container.start { AppModule() }
            app.get { principal } shouldBe principal
            app.get { props.prop1 } shouldBe propertyObject1
            app.get { props.prop2 } shouldBe propertyObject2
            app.get { props.prop3 } shouldBe propertyObject3
            app.get { props.prop4 } shouldBe propertyObject4
            app.get { props.prop5 } shouldBe propertyObject5
            app.get { props.userModule.userName } shouldBe userName
            app.get { props.userModule.password } shouldBe password
        }

        "Getting a property does not trigger creation of other properties if not necessary" {
            val app = Container.start { AppModule(lazy = true) }
            app.get { props.prop1 }
            verifySequence {
                propertyObjectFactory.newValue(propertyObject1.value)
            }
        }

        "Getting a property propagates to other properties if necessary" {
            val app = Container.start { AppModule(lazy = true) }
            app.get { props.prop2 }
            verifySequence {
                propertyObjectFactory.newValue(propertyObject1.value)
                propertyObjectFactory.newValue(propertyObject2.value, propertyObject1)
            }
        }

        "Getting a property results in one instantiation even if many invocations" {
            val app = Container.start { AppModule(lazy = true) }
            repeat(3) { app.get { props.prop1 } }
            verifySequence {
                propertyObjectFactory.newValue(propertyObject1.value)
            }
        }

        "Binding a property does a proper replace" {
            val app = Container.start {
                AppModule().apply {
                    bind(props.prop1) to { propertyObjectFactory.newValue("Kalle", next()) }
                }
            }
            app.get { props.prop1 } shouldBe PropertyObject("Kalle", propertyObject1)
        }


        "Properties should be overridden when using profiles" {
            val app = Container.start {
                AppModule().apply {
                    useProfiles("prof1", "prof2")
                }
            }
            verifySequence {
                propertyObjectFactory.newValue(propertyObject1.value)
                propertyObjectFactory.newValue("152", propertyObject1)
                propertyObjectFactory.newValue("253")
                propertyObjectFactory.newValue(propertyObject4.value)
                propertyObjectFactory.newValue(propertyObject5.value)
            }
            app.profiles.shouldContainExactly("prof1", "prof2")
        }

        "Properties can be overridden by environment vars and system properties" {
            val po1 = PropertyObject("SET1")
            val po2 = PropertyObject("SET2", po1)
            withEnvironment("PROPS_PROP1" to po1.value) {
                withSystemProperties("props.prop2" to po2.value) {
                    val app = Container.start { AppModule() }
                    app.get { props.prop1 } shouldBe po1
                    app.get { props.prop2 } shouldBe po2
                }
            }
        }

    }

    private fun verifyThatNoPropertiesAreInstantiated() {
        verify {
            propertyObjectFactory wasNot Called
        }
    }

    private fun verifyThatAllPropertiesAreInstantiated() {
        verifySequence {
            propertyObjectFactory.newValue(propertyObject1.value)
            propertyObjectFactory.newValue(propertyObject2.value, propertyObject1)
            propertyObjectFactory.newValue(propertyObject3.value)
            propertyObjectFactory.newValue(propertyObject4.value)
            propertyObjectFactory.newValue(propertyObject5.value)
        }
    }

    private data class PropertyObject(val value: String, val parent: PropertyObject? = null)

    private class PropertyObjectFactory() {
        fun newValue(value: String) = PropertyObject(value)
        fun newValue(value: String, parent: PropertyObject) = PropertyObject(value, parent)
    }

}
