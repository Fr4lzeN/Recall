pluginManagement {
    includeBuild("build-logic")
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Recall"

include(":app")

// Core modules
include(":core:common")
include(":core:database")
include(":core:media")
include(":core:ml")
include(":core:vector")
include(":core:worker")
include(":core:designsystem")

// Feature modules
include(":feature:search")
include(":feature:albums")
include(":feature:timeline")
include(":feature:detail")
include(":feature:settings")
include(":feature:onboarding")
