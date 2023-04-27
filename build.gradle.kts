plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.serialization") version "1.4.20"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    application
}

group = "com.n0n5ense"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
    implementation("com.charleskorn.kaml:kaml:0.53.0")
    implementation("com.fazecast:jSerialComm:2.9.3")
}

application {
    mainClass.value("MainKt")
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "MainKt"
        attributes["Multi-Release"] = true
    }
}