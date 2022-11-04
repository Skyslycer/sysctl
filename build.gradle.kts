import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "de.skyslycer"
version = "1.0.0"

application {
    mainClass.set("de.skyslycer.sysctl.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("dev.kord:kord-core:0.8.0-M16")
    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}