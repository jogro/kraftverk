package io.kraftverk

import io.kotlintest.matchers.collections.containExactly
import io.kotlintest.should
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppTest {

    val ONCE = 1
    val NEVER = 0

    @Nested
    inner class `bean instantiation` {

        @Test
        fun `is eager by default`() {
            var instantiations = 0
            App.start {
                object : Module() {
                    val bean0 by bean {
                        instantiations++
                        42
                    }
                }
            }
            instantiations shouldBe ONCE
        }

        @Test
        fun `is lazy when default lazy is true`() {
            var instantiations = 0
            App.startLazy {
                object : Module() {
                    val bean0 by bean {
                        instantiations++
                        42
                    }
                }
            }
            instantiations shouldBe NEVER
        }

        @Test
        fun `is lazy when specified for the bean`() {
            var instantiations = 0
            App.start {
                object : Module() {
                    val bean0 by bean(lazy = true) {
                        instantiations++
                        42
                    }
                }
            }
            instantiations shouldBe NEVER
        }

        @Test
        fun `is eager when specified for the bean`() {
            var instantiations = 0
            App.start {
                object : Module() {
                    val bean0 by bean(lazy = false) {
                        instantiations++
                        42
                    }
                }
            }
            instantiations shouldBe ONCE
        }

        @Test
        fun `is eager when specified even if defaultLazy`() {
            var instantiations = 0
            App.startLazy {
                object : Module() {
                    val bean0 by bean(lazy = false) {
                        instantiations++
                        42
                    }
                }
            }
            instantiations shouldBe ONCE
        }
    }

    @Nested
    inner class `bean get` {
        @Test
        fun `Returns expected value`() {
            val app = App.start {
                object : Module() {
                    val bean0 by bean { 42 }
                }
            }
            app.get { bean0 } shouldBe 42
        }

        @Test
        fun `results in one instantiation even if many invocations`() {
            var instantiations = 0
            val app = App.start {
                object : Module() {
                    val bean0 by bean {
                        println("Active profiles: $profiles")
                        instantiations++
                        42
                    }
                }
            }
            repeat(3) { app.get { bean0 } }
            instantiations shouldBe ONCE
        }

    }

    @Nested
    inner class `bean on start` {
        @Test
        fun `is invoked properly`() {
            var invoked = 0
            App.start {
                object : Module() {
                    val bean0 by bean { 42 }

                    init {
                        onStart(bean0) { invoked++ }
                    }
                }
            }
            invoked shouldBe ONCE
        }

        @Test
        fun `is invoked once even if many gets`() {
            var invoked = 0
            val app = App.start {
                object : Module() {
                    val bean0 by bean { 42 }

                    init {
                        onStart(bean0) { invoked++ }
                    }
                }
            }
            repeat(3) { app.get { bean0 } }
            invoked shouldBe ONCE
        }

        @Test
        fun `invokes next properly`() {
            var value = 0

            open class RootModule() : Module() {
                val bean0 by bean { 42 }

                init {
                    onStart(bean0) { value++ }
                }
            }

            val app = App.start {
                object : RootModule() {

                    init {
                        onStart(bean0) { next(); value++ }
                    }
                }
            }
            app.get { bean0 }
            value shouldBe 2
        }

    }

    @Nested
    inner class `testing properties` {

        inner class MyProps : Module() {
            val prop1 by property()
            val prop2 by property()
            val prop3 by property()
            val prop4 by property("prop44")
        }

        inner class MyModule : Module() {
            val props by module { MyProps() }
            val props2 by module("props2") { MyProps() }
        }

        @Test
        fun `no profile`() {
            val app = App.start(::MyModule)
            app.profiles.isEmpty() shouldBe true
            app.get { props.prop1 } shouldBe "F056"
            app.get { props.prop2 } shouldBe "F057"
            app.get { props.prop3 } shouldBe "F058"
        }

        @Test
        fun `no profile - set value`() {
            val app = App.start(::MyModule) {
                bind(props.prop1) to { "SET" }
            }
            app.profiles.isEmpty() shouldBe true
            app.get { props.prop1 } shouldBe "SET"
            app.get { props.prop2 } shouldBe "F057"
            app.get { props.prop3 } shouldBe "F058"
        }

        @Test
        fun `using one profile`() {
            val app = App.start(::MyModule) {
                useProfiles("prof1")
            }
            app.profiles should containExactly("prof1")
            app.get { props.prop1 } shouldBe "F056"
            app.get { props.prop2 } shouldBe "F157"
            app.get { props.prop3 } shouldBe "F158"
        }

        @Test
        fun `using two profiles`() {
            val app = App.start(::MyModule) {
                useProfiles("prof1", "prof2")
            }
            app.profiles should containExactly("prof1", "prof2")
            app.get { props.prop1 } shouldBe "F056"
            app.get { props.prop2 } shouldBe "F157"
            app.get { props.prop3 } shouldBe "F258"
        }

        @Test
        fun `using two profiles - set value`() {
            val app = App.start(::MyModule) {
                useProfiles("prof1", "prof2")
                bind(props.prop1) to { "SET" }
            }
            app.profiles should containExactly("prof1", "prof2")
            app.get { props.prop1 } shouldBe "SET"
            app.get { props.prop2 } shouldBe "F157"
            app.get { props.prop3 } shouldBe "F258"
        }

        @Test
        fun `using two profiles + environment variable`() {
            val env = mapOf("PROPS_PROP1" to "E056")
            TestUtils.setEnv(env)
            try {
                val app = App.start(::MyModule) {
                    useProfiles("prof1", "prof2")
                }
                app.profiles should containExactly("prof1", "prof2")
                app.get { props.prop1 } shouldBe "E056"
                app.get { props.prop2 } shouldBe "F157"
                app.get { props.prop3 } shouldBe "F258"

            } finally {
                TestUtils.removeEnv(env)
            }
        }

        @Test
        fun `using two profiles + environment variable + System property`() {
            val env = mapOf("PROPS_PROP1" to "E056")
            System.setProperty("props.prop1", "S056")
            TestUtils.setEnv(env)
            try {
                val app = App.start(
                    module = ::MyModule
                ) { useProfiles("prof1", "prof2") }
                app.profiles should containExactly("prof1", "prof2")
                app.get { props.prop1 } shouldBe "S056"
                app.get { props.prop2 } shouldBe "F157"
                app.get { props.prop3 } shouldBe "F258"
            } finally {
                System.clearProperty("props.prop1")
                TestUtils.removeEnv(env)
            }
        }

    }

    @Nested
    inner class `including module` {

        inner class SomeTypeProperties : Module() {
            val date by property(defaultValue="2019-08-01") { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
            val age: Property<Int> by property { it.toInt() }
            val time: Property<Long> by property { it.toLong() }
        }

        inner class AuthProperties : Module() {
            val user by property()
            val password by property()
        }

        inner class DbModule : Module() {
            val url by property()
            val auth by module { AuthProperties() }
        }

        inner class MyModule : Module() {
            val db0 by module { DbModule() }
            val db1 by module(::DbModule) {
                bind(url) to { "url1" }
            }
            val db2 by module {
                DbModule().apply {
                    bind(url) to { "url2" }
                }
            }
            val db3 by module("db3") { DbModule() }
        }

        @Test
        fun name() {
            val app = App.startLazy(::SomeTypeProperties) {
                bind(age) to { 12 }
                bind(time) to { 167 }
            }
            app.get { age } shouldBe 12
            app.get { time } shouldBe 167
            app.get { date } shouldBe LocalDate.of(2019,8,1)
        }

        @Test
        fun `some nested property paths`() {
            val app = App.startLazy(::MyModule) {
                bind(db0.url) to { "url0" }
                bind(db0.auth.user) to { "user0" }
                bind(db0.auth.password) to { "password0" }
                bind(db1.auth.user) to { "user1" }
                bind(db1.auth.password) to { "password1" }
                bind(db2.auth.user) to { "user2" }
                bind(db2.auth.password) to { "password2" }
                bind(db3.url) to { "url3" }
                bind(db3.auth.user) to { "user3" }
                bind(db3.auth.password) to { "password3" }
            }
            app.get { db0.url } shouldBe "url0"
            app.get { db0.auth.user } shouldBe "user0"
            app.get { db0.auth.password } shouldBe "password0"
            app.get { db1.url } shouldBe "url1"
            app.get { db1.auth.user } shouldBe "user1"
            app.get { db1.auth.password } shouldBe "password1"
            app.get { db2.url } shouldBe "url2"
            app.get { db2.auth.user } shouldBe "user2"
            app.get { db2.auth.password } shouldBe "password2"
            app.get { db3.url } shouldBe "url3"
            app.get { db3.auth.user } shouldBe "user3"
            app.get { db3.auth.password } shouldBe "password3"
        }
    }

}
