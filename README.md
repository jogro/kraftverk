# Kraftverk

Kraftverk is a minimalistic dependency injection toolkit written in pure Kotlin. 

## Quickstart

### Add dependency

#### Maven

```xml
<dependency>
    <groupId>io.kraftverk</groupId>
    <artifactId>kraftverk</artifactId>
    <version>0.8.10</version>
</dependency>
```

#### Gradle

```groovy
compile "io.kraftverk:kraftverk:0.8.10"
```

### Define a module
```kotlin
class Service(val repo: Repository)
class Repository

class AppModule : Module() {

    val service by bean {
        Service(
            repository()
        )
    }

    val repository by bean {
        Repository()
    }

}
```

### Start a managed instance:
```kotlin
fun main() {
    val app = start { AppModule() }
    val service = app { service }
    println(service)
}
```
