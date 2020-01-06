Kraftverk
===========================

Kraftverk is a minimalistic depency injection framework for Kotlin.

Sample code
---------
Inspired by the "coffee example" from the [Dagger 2 Users guide](https://dagger.dev/users-guide.html)

Given a module:
```kotlin
class AppModule : Module() {

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
Start a Container instance:
```kotlin
fun main() {
    val app = Kraftverk.manage { AppModule() }
    val coffeeMaker = app.get { coffeeMaker }
    coffeeMaker.brew()
}
```



