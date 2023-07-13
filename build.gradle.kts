import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.2"
    kotlin("plugin.serialization") version "1.5.30"
    application
}

group = "me.kartikey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation ("ch.qos.logback:logback-classic:1.2.6")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("org.jfree:jfreechart:1.5.0")
}



tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}