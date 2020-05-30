plugins {
    kotlin("jvm") version "1.3.72"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
}