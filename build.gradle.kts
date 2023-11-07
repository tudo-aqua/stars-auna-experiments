plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.diffplug.spotless") version "6.21.0"
}

group = "tools.aqua"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

var starsVersion = "0.2"

dependencies {
    testImplementation(kotlin("test"))
    implementation("tools.aqua:stars-core:$starsVersion")
    implementation("tools.aqua:stars-logic-kcmftbl:$starsVersion")
    implementation("tools.aqua:stars-data-av:$starsVersion")
    implementation("tools.aqua:stars-importer-carla:$starsVersion")
}

spotless {
    kotlin {
        licenseHeaderFile(rootProject.file("contrib/license-header.template.kt")).also {
            it.updateYearWithLatest(true)
        }
        ktfmt()
    }
    kotlinGradle {
        licenseHeaderFile(
            rootProject.file("contrib/license-header.template.kt"),
            "(import |@file|plugins |dependencyResolutionManagement|rootProject.name)")
            .also { it.updateYearWithLatest(true) }
        ktfmt()
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin { jvmToolchain(17) }

application { mainClass.set("tools.aqua.stars.auna.experiments.RunExperimentsKt") }