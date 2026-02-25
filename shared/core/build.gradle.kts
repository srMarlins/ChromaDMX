plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
