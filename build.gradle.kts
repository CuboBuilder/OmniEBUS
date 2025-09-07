import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "io.lucin"
version = "1.0"

application {
    mainClass.set("io.lucin.MainKt")
}

repositories {
    mavenCentral()
    // Only use Zelaux’s repo (no JitPack, avoids commit hashes)
    maven {
        url = uri("https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
    }
}

dependencies {
    val mindustryVersion = "v146"

    // Mindustry Core
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")

    // Arc + required submodules
    implementation("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    implementation("com.github.Anuken.Arc:flabel:$mindustryVersion")
    implementation("com.github.Anuken.Arc:freetype:$mindustryVersion")
    implementation("com.github.Anuken.Arc:g3d:$mindustryVersion")
    implementation("com.github.Anuken.Arc:fx:$mindustryVersion")
    implementation("com.github.Anuken.Arc:arcnet:$mindustryVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    archiveFileName.set("uwu.jar")

    // Fat jar with dependencies
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
