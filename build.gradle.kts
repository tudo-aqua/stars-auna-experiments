/*
 * Copyright 2023-2024 The STARS AuNa Experiments Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  kotlin("jvm") version "2.0.0-RC3"
  application
  id("com.diffplug.spotless") version "6.25.0"
  kotlin("plugin.serialization") version "1.9.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "tools.aqua"

version = "1.0"

repositories { mavenCentral() }

dependencies {
  testImplementation(kotlin("test"))
  implementation("tools.aqua:stars-core:0.3")
  implementation("tools.aqua:stars-logic-kcmftbl:0.3")
  implementation("tools.aqua:stars-data-av:0.3")
  implementation("tools.aqua:stars-importer-carla:0.3")
  implementation("de.sciss:kdtree:0.1.1")
  implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.5.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
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

detekt { config.setFrom(files("contrib/detekt-rules.yml")) }

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }

application { mainClass.set("tools.aqua.stars.auna.experiments.RunExperimentsKt") }
