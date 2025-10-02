plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0-RC3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0-RC3")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.avgoryunov.learnWordsBot.telegram.TelegramKt")
}