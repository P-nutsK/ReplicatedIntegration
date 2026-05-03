pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = extra["modId"] as String

include("common")
include("1.20.1-common")
include("1.20.1-forge")
include("1.21.1-common")
include("1.21.1-neo")
