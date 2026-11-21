import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    val localProperties = Properties().apply {
        val file = rootDir.resolve("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }
    val mapboxDownloadsToken = providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN")
        .orNull
        ?: localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN", "")

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri(rootDir.resolve("third_party/google-home-sdk")) }
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = mapboxDownloadsToken
            }
        }
    }
}

rootProject.name = "royal_shield"
include(":app")
