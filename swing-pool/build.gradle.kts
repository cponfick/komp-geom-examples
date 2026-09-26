plugins {
    application
}

group = "io.github.cponfick.examples"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cponfick:komp-geom-jvm:0.4.0-rc7")
}

application {
    mainClass = "io.github.cponfick.examples.pool.SwingPool"
}

//tasks.jar {
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    from(configurations.runtimeClasspath.get().map { dependency ->
//        if (dependency.isDirectory) dependency else zipTree(dependency)
//    })
//    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
//    manifest {
//        attributes["Main-Class"] = application.mainClass.get()
//    }
//}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
