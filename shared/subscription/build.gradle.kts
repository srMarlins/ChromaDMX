plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(libs.sqldelight.coroutines)
        }
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}
