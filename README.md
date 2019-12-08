Kraftverk
===========================

Kraftverk is a depency injection framework for Kotlin, that involves no reflection or code generation.

Sample code
---------
Inspired by the "coffee example" from the [Dagger 2 Users guide](https://dagger.dev/users-guide.html)

Given a module:
```kotlin
class DripCoffeeModule : Module() {

    val heater by bean<Heater>(lazy = true) {
        ElectricHeater()
    }

    val pump by bean<Pump> {
        Thermosiphon(
            { heater() }
        )
    }

    val coffeeMaker by bean {
        CoffeeMaker(
            { heater() },
            pump()
        )
    }

}
```
Start an App instance:
```kotlin
fun main() {
    val app = App.start{ DripCoffeeModule() }
    val coffeeMaker = app.get { coffeeMaker }
    coffeeMaker.brew()
}
```



