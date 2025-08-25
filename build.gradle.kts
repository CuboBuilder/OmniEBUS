import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    java
}

group = "io.lucin"
version = "1.0"

application {
    mainClass.set("io.lucin.MainKt")
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_1_8
}

val mindustryVersion = "v150.1"
val jabelVersion = "93fde537c7"

dependencies {
    implementation("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    implementation("com.github.Anuken.Arc:arcnet:$mindustryVersion")
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")

    annotationProcessor("com.github.Anuken:jabel:$jabelVersion")

    testImplementation(kotlin("test"))
}

// force arc version to match Mindustry
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.github.Anuken.Arc") {
            useVersion(mindustryVersion)
        }
    }
}

// Java 8 backwards compatibility
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--release", "8"))
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

// build fat jar
tasks.jar {
    archiveFileName.set("uwu.jar")

    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
