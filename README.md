Kraftverk
===========================

Kraftverk is a depency injection framework for Kotlin, that involves no reflection or code generation.

Sample code
---------
Inspired by the "coffee example" from the [Dagger 2 Users guide](https://dagger.dev/users-guide.html)

Given a module:
```kotlin
class DripCoffeeModule : Module() {

    val heater by singleton<Heater>(lazy = true) {
        ElectricHeater()
    }

    val pump by singleton<Pump> {
        Thermosiphon(
            heater()
        )
    }

    val coffeeMaker by singleton {
        CoffeeMaker(
            { heater() },
            pump()
        )
    }

}
```
You can start an App instance like this:
```kotlin
fun main() {
    val app = App.start { DripCoffeeModule() }
    val coffeeMaker = app.getBean { coffeeMaker }
    coffeeMaker.brew()
}
```
 


