plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
        }
    }
}

android {
    namespace = "com.chromadmx.tempo"
}
