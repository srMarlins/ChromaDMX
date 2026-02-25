import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = the<VersionCatalogsExtension>().named("libs")

            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<KotlinMultiplatformExtension> {
                // Suppress beta warning for expect/actual classes
                targets.configureEach {
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            compilerOptions {
                                freeCompilerArgs.add("-Xexpect-actual-classes")
                            }
                        }
                    }
                }

                // Configure Android target via the new AGP 9.0 KMP library DSL
                targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                    .configureEach {
                        namespace = "com.chromadmx.${project.name}"
                        compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()
                        minSdk = libs.findVersion("android-minSdk").get().toString().toInt()

                        compilerOptions {
                            jvmTarget.set(JvmTarget.JVM_17)
                        }

                        // Enable Android host tests (unit tests) so commonTest runs on Android
                        withHostTestBuilder {}
                    }

                // iOS targets
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                )

                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                        implementation(libs.findLibrary("koin-core").get())
                    }

                    commonTest.dependencies {
                        implementation(libs.findLibrary("kotlin-test").get())
                        implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                    }

                    androidMain.dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutines-android").get())
                    }
                }
            }
        }
    }
}
