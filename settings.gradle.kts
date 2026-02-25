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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
    }
}

rootProject.name = "ChromaDMX"

include(":shared")
include(":shared:core")
include(":shared:networking")
include(":shared:tempo")
include(":shared:vision")
include(":shared:simulation")
include(":shared:engine")
include(":shared:agent")
include(":android:app")
