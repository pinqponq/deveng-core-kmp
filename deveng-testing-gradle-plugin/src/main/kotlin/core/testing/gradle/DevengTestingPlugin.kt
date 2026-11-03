package core.testing.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DevengTestingPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
            project.pluginManager.apply(KOVER_PLUGIN_ID)
            project.pluginManager.apply(ROBORAZZI_PLUGIN_ID)
            addTestDependencies(project)
        }

        pinTestLocale(project)
    }

    private fun addTestDependencies(project: Project) {
        val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlinExtension.sourceSets.matching { sourceSet -> sourceSet.name == COMMON_TEST_SOURCE_SET }.configureEach { commonTestSourceSet ->
            commonTestSourceSet.dependencies {
                implementation(KOTLIN_TEST_MODULE)
                implementation("$CORE_TESTING_MODULE:${DevengTestingVersions.LIBRARY_VERSION}")
            }
        }

        kotlinExtension.sourceSets.matching { sourceSet -> sourceSet.name == DESKTOP_TEST_SOURCE_SET }.configureEach { desktopTestSourceSet ->
            desktopTestSourceSet.dependencies {
                implementation("$ROBORAZZI_COMPOSE_DESKTOP_MODULE:${DevengTestingVersions.ROBORAZZI_VERSION}")
            }
        }
    }

    // Screenshot baselines and string lookups depend on the JVM locale.
    private fun pinTestLocale(project: Project) {
        project.tasks.withType(Test::class.java).configureEach { testTask ->
            testTask.systemProperty(USER_LANGUAGE_PROPERTY, TEST_LANGUAGE)
            testTask.systemProperty(USER_COUNTRY_PROPERTY, TEST_COUNTRY)
        }
    }

    private companion object {
        const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
        const val KOVER_PLUGIN_ID = "org.jetbrains.kotlinx.kover"
        const val ROBORAZZI_PLUGIN_ID = "io.github.takahirom.roborazzi"
        const val COMMON_TEST_SOURCE_SET = "commonTest"
        const val DESKTOP_TEST_SOURCE_SET = "desktopTest"
        const val KOTLIN_TEST_MODULE = "org.jetbrains.kotlin:kotlin-test"
        const val CORE_TESTING_MODULE = "global.deveng:core-testing-kmp"
        const val ROBORAZZI_COMPOSE_DESKTOP_MODULE = "io.github.takahirom.roborazzi:roborazzi-compose-desktop"
        const val USER_LANGUAGE_PROPERTY = "user.language"
        const val USER_COUNTRY_PROPERTY = "user.country"
        const val TEST_LANGUAGE = "en"
        const val TEST_COUNTRY = "US"
    }
}
