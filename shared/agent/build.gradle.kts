plugins {
    id("chromadmx.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            implementation(project(":shared:engine"))
            implementation(project(":shared:networking"))
            implementation(project(":shared:tempo"))
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.koog.agents)
            implementation(libs.koog.agents.ext)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
