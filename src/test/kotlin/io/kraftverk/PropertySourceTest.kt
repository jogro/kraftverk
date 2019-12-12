package io.kraftverk

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrowUnit
import io.kotlintest.specs.WordSpec
import io.kotlintest.tables.row
import io.kraftverk.internal.PropertySource
import kotlinx.coroutines.launch

class PropertySourceTest : WordSpec() {

    init {

        val propertySource = PropertySource()

        "PropertySource" should {
            "accept a single case in-sensitive alphanumeric property name that might contain dashes" {
                val rows = arrayOf(
                    row("person1"),
                    row("person-1"),
                    row("pers---on-1"),
                    row("Person1"),
                    row("Person-1"),
                    row("PERSON1 "),
                    row(" PERSON1")
                )
                forall(
                    *rows
                ) { setterKey ->
                    forall(
                        *rows
                    ) { getterKey ->
                        propertySource.clear()
                        propertySource[setterKey] = "ABC"
                        propertySource[getterKey] shouldBe "ABC"
                    }
                }
            }
            "accept nested property names" {
                val rows = arrayOf(
                    row("crowd1.person1.name1"),
                    row("CROWD1_PERSON1_NAME1")
                )
                forall(
                    *rows
                ) { setterName ->
                    forall(
                        *rows
                    ) { getterName ->
                        propertySource.clear()
                        propertySource[setterName] = "ABC"
                        propertySource[getterName] shouldBe "ABC"
                    }
                }

            }
            "throw exception when an invalid property name is encountered" {
                forall(
                    row("name&"),
                    row("nam e"),
                    row("\$xyz"),
                    row("{name}"),
                    row("person..name"),
                    row("person...name"),
                    row("person.name."),
                    row(".person.name"),
                    row("PERSON__NAME"),
                    row("PERSON___NAME"),
                    row("PERSON_NAME_"),
                    row("_PERSON_NAME"),
                    row("."),
                    row("_")

                ) { name ->
                    shouldThrowUnit<PropertyNameException> {
                        propertySource[name] = "ABC"
                    }
                }
            }
        }

    }
}