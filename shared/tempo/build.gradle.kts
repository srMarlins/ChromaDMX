plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.compilations["main"].cinterops {
            create("abletonLink") {
                defFile = file("src/nativeInterop/cinterop/ableton_link.def")
                includeDirs("src/nativeInterop/cinterop/headers")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
        }
    }
}
