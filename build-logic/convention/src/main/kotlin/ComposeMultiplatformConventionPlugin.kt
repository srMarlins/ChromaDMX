import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            // Add Compose dependencies to commonMain via the Kotlin extension.
            // This plugin assumes kotlin-multiplatform is already applied
            // (e.g., via chromadmx.kmp.library).
            extensions.configure<KotlinMultiplatformExtension> {
                val compose = the<ComposeExtension>().dependencies

                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(compose.runtime)
                        implementation(compose.foundation)
                        implementation(compose.material3)
                        implementation(compose.ui)
                    }
                }
            }
        }
    }
}
