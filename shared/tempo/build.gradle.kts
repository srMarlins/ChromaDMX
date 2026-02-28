plugins {
    id("chromadmx.kmp.library")
}

val useStubs = findProperty("chromadmx.linkkit.stubs")?.toString()?.toBoolean() ?: false

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.compilations["main"].cinterops {
            create("abletonLink") {
                defFile = if (useStubs) {
                    file("src/nativeInterop/cinterop/ableton_link_stub.def")
                } else {
                    file("src/nativeInterop/cinterop/ableton_link.def")
                }
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
