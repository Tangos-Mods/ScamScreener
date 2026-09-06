pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val minecraftVersions = providers.gradleProperty("minecraft.versions")
    .get()
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)

stonecutter {
    create(rootProject) {
        versions(*minecraftVersions.toTypedArray())
        vcsVersion = minecraftVersions.last()
    }
}

rootProject.name = "ScamScreener"
