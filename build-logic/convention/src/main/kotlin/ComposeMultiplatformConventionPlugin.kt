import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("DEPRECATION")
class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            // Add Compose dependencies to commonMain via the Kotlin extension.
            // This plugin assumes kotlin-multiplatform is already applied
            // (e.g., via chromadmx.kmp.library).
            // Note: compose.* accessors are deprecated in CMP 1.10+ but the direct
            // artifact replacement has version mismatches (e.g. material3 != plugin version).
            // Using @Suppress("DEPRECATION") until JetBrains provides a stable migration path.
            extensions.configure<KotlinMultiplatformExtension> {
                val compose = the<ComposePlugin.Dependencies>()

                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(compose.runtime)
                        implementation(compose.foundation)
                        implementation(compose.material3)
                        implementation(compose.materialIconsExtended)
                        implementation(compose.ui)
                        implementation(compose.components.resources)
                    }
                }
            }
        }
    }
}
