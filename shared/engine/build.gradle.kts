plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(project(":shared:tempo"))
            implementation(libs.kotlinx.atomicfu)
        }
    }
}

android {
    namespace = "com.chromadmx.engine"
}
