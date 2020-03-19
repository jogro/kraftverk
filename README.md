# Kraftverk

Kraftverk is a configuration and dependency injection toolkit written in pure Kotlin.

Example component:
```kotlin
class JdbcModule : Module() {

    val url by string()
    val username by string()
    val password by string(secret = true)

    val dataSource by bean { HikariDataSource() }

    init {
        customize(dataSource) { ds ->
            ds.jdbcUrl = url()
            ds.username = username()
            ds.password = password()
        }
    }
}
```

  

## Quickstart
### Maven
Add dependency
```xml
<dependency>
    <groupId>io.kraftverk</groupId>
    <artifactId>kraftverk</artifactId>
    <version>0.9.3</version>
</dependency>
```
Add repository
```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <name>jcenter</name>
        <url>https://jcenter.bintray.com</url>
    </repository>
</repositories>
```
### Gradle
Add dependency
```groovy
compile "io.kraftverk:kraftverk:0.9.3"
```
Add repository
```groovy
repositories {
    //[...]
    jcenter()
}
```
### Given some classes
```kotlin
class Service(val repo: Repository)
class Repository
```
### Define a module
```kotlin
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
    val app = Kraftverk.start { AppModule() }
    val service = app { service }
    println(service)
}
```
