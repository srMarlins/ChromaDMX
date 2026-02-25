plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            implementation(libs.kotlinx.atomicfu)
        }
    }
}

android {
    namespace = "com.chromadmx.networking"
}
