plugins {
    kotlin("jvm") version "1.3.72"
    application
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.3.6")
//    implementation("no.tornado:tornadofx:1.7.20")
}

val mainClass = "freeSpeech.LauncherKt"

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    register<Jar>("fatJar") {
        destinationDirectory.set(projectDir.resolve("out/artifacts"))
        archiveVersion.set(project.version.toString())
        manifest {
            attributes["Main-Class"] = mainClass
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().output)
        dependsOn(configurations.runtimeClasspath)
        from({
            sourceSets.main.get().runtimeClasspath.files.filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }

    register("launcher") {
        // TODO
    }
}

application {
    mainClassName = mainClass
}