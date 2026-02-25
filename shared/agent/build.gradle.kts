plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            implementation(project(":shared:engine"))
            implementation(project(":shared:networking"))
            implementation(project(":shared:tempo"))
            implementation(libs.kotlinx.atomicfu)
        }
    }
}

android {
    namespace = "com.chromadmx.agent"
}
