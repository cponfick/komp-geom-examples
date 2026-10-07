plugins {
    application
}

group = "io.github.cponfick.examples"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cponfick:komp-geom-jvm:0.5.0")
}

application {
    mainClass = "io.github.cponfick.examples.pool.SwingPool"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
