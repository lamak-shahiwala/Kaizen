pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.android") version "2.1.0" // Should match your libs.version.toml
        id("com.android.application") version "8.6.1"
        id("com.google.gms.google-services") version "4.4.2" // Or your preferred version
        // --- Update this line! ---
        //id("androidx.compose.compiler") version "1.7.0" // Use Compose Compiler 1.7.0 for Kotlin 2.1.0
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Persistence"
include(":app")
