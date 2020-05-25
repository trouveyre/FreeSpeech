plugins {
    kotlin("multiplatform") version "1.3.72"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.3.6")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
}