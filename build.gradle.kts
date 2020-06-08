plugins {
    kotlin("jvm") version "1.3.72"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
}

val mainClass = "freeSpeech.javafx.FreeSpeech"

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            includeRuntime = true
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    withType<Jar>().configureEach {
        destinationDirectory.set(projectDir.resolve("artifact"))
        manifest {
            attributes["Main-Class"] = mainClass
        }
    }
}

application {
    mainClassName = mainClass
}
