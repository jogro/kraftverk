# Kraftverk

Kraftverk is a minimalistic dependency injection toolkit written in pure Kotlin. Its primary
goal is to ease the use of manual dependency injection.

Some key points:
* There are no @Annotations
* There is no reflection
* There is no other magic; just code.

## Quickstart

### Add dependency

#### Maven

```xml
<dependency>
    <groupId>io.kraftverk</groupId>
    <artifactId>kraftverk</artifactId>
    <version>0.8.8</version>
</dependency>
```

#### Gradle

```groovy
compile "io.kraftverk:kraftverk:0.8.8"
```

### Define a module
```kotlin
import io.kraftverk.Javalin

fun main() {
    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.result("Hello World") }
}
```

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
Create a managed instance:
```kotlin
fun main() {
    val app = Kraftverk.manage { AppModule() }
    val coffeeMaker = app.get { coffeeMaker }
    coffeeMaker.brew()
}
```



