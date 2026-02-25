plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "chromadmx.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "chromadmx.compose"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("androidApplication") {
            id = "chromadmx.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
