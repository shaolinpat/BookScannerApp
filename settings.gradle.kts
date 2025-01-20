pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // Required for AndroidX and CameraX
        mavenCentral()
    }
}


rootProject.name = "BookScannerApp"
include(":app")
