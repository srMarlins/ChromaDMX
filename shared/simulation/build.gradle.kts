plugins {
    id("chromadmx.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(project(":shared:networking"))
        }
        commonTest.dependencies {
            implementation(project(":shared:engine"))
            implementation(project(":shared:tempo"))
            implementation(project(":shared:vision"))
        }
    }
}
