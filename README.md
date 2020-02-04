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
class ServiceA(val b: ServiceB)
class ServiceB()

class AppModule : Module() {
    val serviceA by bean { // Bean<ServiceA>
        ServiceA(
            serviceB() // Inject
        )
    }
    val serviceB by bean {
        ServiceB()
    }
}
```

### Create a managed instance:
```kotlin
fun main() {
    val app = Kraftverk.manage { AppModule() }
    val serviceA by app { serviceA }
    print(serviceA)
}
```
