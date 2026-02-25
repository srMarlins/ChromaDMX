import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.api.artifacts.VersionCatalogsExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = the<VersionCatalogsExtension>().named("libs")

            pluginManager.apply("com.android.application")
            // AGP 9.0 has built-in Kotlin support — no separate kotlin-android plugin needed
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()

                defaultConfig {
                    minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
                    targetSdk = libs.findVersion("android-targetSdk").get().toString().toInt()
                }

                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                }

                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
