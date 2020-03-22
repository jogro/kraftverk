# Kraftverk

Kraftverk is a dependency injection toolkit written in pure Kotlin. It has one slogan:

"Dependency resolution should be done at compile time". 

With Kraftverk application startup is really fast. It employs no classpath scanning or 
time consuming reflective inspection of constructor or method signatures to wire your components.
The wiring has already been resolved at compile time. The days when you are having "a bad container day" might 
be over.

### Maven

Add dependency
```xml
<dependency>
    <groupId>io.kraftverk</groupId>
    <artifactId>kraftverk</artifactId>
    <version>0.9.3</version>
</dependency>
```
If Maven can't find the dependency add this repository to the POM file.
```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <name>jcenter</name>
        <url>https://jcenter.bintray.com</url>
    </repository>
</repositories>
```

The project is under heavy development and more documentation is to come. Until then have a look at the 
sample files below.

[AppModule0](https://github.com/jogro/kotlin-javalin-realworld-example-app/blob/master/src/main/kotlin/io/realworld/app/AppModule0.kt)


[AppModule1](https://github.com/jogro/kotlin-javalin-realworld-example-app/blob/master/src/main/kotlin/io/realworld/app/AppModule1.kt)

[AppModule2](https://github.com/jogro/kotlin-javalin-realworld-example-app/blob/master/src/main/kotlin/io/realworld/app/AppModule2.kt)



