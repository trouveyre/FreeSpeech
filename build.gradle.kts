plugins {
    kotlin("jvm") version "1.3.72"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
    implementation("no.tornado:tornadofx:1.7.20")
}

val mainClass = "freeSpeech.Launcher"

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