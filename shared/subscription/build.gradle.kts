plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
        }
    }
}
