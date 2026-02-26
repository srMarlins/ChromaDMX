plugins {
    id("chromadmx.kmp.library")
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("ChromaDmxDatabase") {
            packageName.set("com.chromadmx.core.db")
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
