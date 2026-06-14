// Top-level build file where you can add configuration options common to all sub-projects/modules.

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
        google()
        mavenCentral()
    }
}

rootProject.name = "A206730_CAOYUNHAN_CikguIzwan_Project2"
include(":app")