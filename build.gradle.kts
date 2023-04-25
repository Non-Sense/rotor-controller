plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.serialization") version "1.4.20"
}

group = "com.n0n5ense"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
    implementation("com.charleskorn.kaml:kaml:0.53.0")
    implementation("com.fazecast:jSerialComm:2.9.3")
}