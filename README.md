# Kraftverk

Kraftverk is a minimalistic dependency injection toolkit written in pure Kotlin. 

## Quickstart

### Add dependency

#### Maven

```xml
<dependency>
    <groupId>io.kraftverk</groupId>
    <artifactId>kraftverk</artifactId>
    <version>0.8.9</version>
</dependency>
```

#### Gradle

```groovy
compile "io.kraftverk:kraftverk:0.8.9"
```

### Define a module
```kotlin
class MyService(val b: MyRepository)
class MyRepository()

class AppModule : Module() {

    val myService by bean {
        MyService(
            myRepository() // Inject
        )
    }

    val myRepository by bean {
        MyRepository()
    }

}
```

### Create a managed instance:
```kotlin
fun main() {
    val app = Kraftverk.manage { AppModule() }
    val myService = app.get { myService }
    println(myService)
}
```
