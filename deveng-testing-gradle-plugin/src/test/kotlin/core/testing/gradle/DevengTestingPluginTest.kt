package core.testing.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevengTestingPluginTest {

    @kotlin.test.Test
    fun apply_onMultiplatformModuleWithDesktopTarget_appliesPluginsAndAddsTestDependencies() {
        val project = createProject()
        project.pluginManager.apply(KOTLIN_MULTIPLATFORM_PLUGIN_ID)
        project.extensions.getByType(KotlinMultiplatformExtension::class.java).jvm(DESKTOP_TARGET)

        project.pluginManager.apply(DEVENG_TESTING_PLUGIN_ID)

        assertTrue(project.pluginManager.hasPlugin(KOVER_PLUGIN_ID))
        assertTrue(project.pluginManager.hasPlugin(ROBORAZZI_PLUGIN_ID))
        assertTrue(CORE_TESTING_DEPENDENCY in declaredDependencies(project, COMMON_TEST_IMPLEMENTATION))
        assertTrue(KOTLIN_TEST_DEPENDENCY in declaredDependencies(project, COMMON_TEST_IMPLEMENTATION))
        assertTrue(ROBORAZZI_DEPENDENCY in declaredDependencies(project, DESKTOP_TEST_IMPLEMENTATION))
    }

    @kotlin.test.Test
    fun apply_beforeMultiplatformPlugin_waitsForItAndStillConfiguresModule() {
        val project = createProject()

        project.pluginManager.apply(DEVENG_TESTING_PLUGIN_ID)
        project.pluginManager.apply(KOTLIN_MULTIPLATFORM_PLUGIN_ID)

        assertTrue(project.pluginManager.hasPlugin(KOVER_PLUGIN_ID))
        assertTrue(CORE_TESTING_DEPENDENCY in declaredDependencies(project, COMMON_TEST_IMPLEMENTATION))
    }

    @kotlin.test.Test
    fun apply_withoutMultiplatformPlugin_onlyPinsTestLocaleOverOtherLocales() {
        val project = createProject()
        project.pluginManager.apply(JAVA_PLUGIN_ID)
        project.tasks.withType(Test::class.java).configureEach { testTask ->
            testTask.systemProperty(USER_LANGUAGE_PROPERTY, OTHER_LANGUAGE)
            testTask.systemProperty(USER_COUNTRY_PROPERTY, OTHER_COUNTRY)
        }

        project.pluginManager.apply(DEVENG_TESTING_PLUGIN_ID)

        assertFalse(project.pluginManager.hasPlugin(KOVER_PLUGIN_ID))
        val testJvmArguments = project.tasks.withType(Test::class.java).getByName(JAVA_TEST_TASK).allJvmArgs
        assertTrue("-D$USER_LANGUAGE_PROPERTY=$EXPECTED_LANGUAGE" in testJvmArguments)
        assertTrue("-D$USER_COUNTRY_PROPERTY=$EXPECTED_COUNTRY" in testJvmArguments)
        assertFalse("-D$USER_LANGUAGE_PROPERTY=$OTHER_LANGUAGE" in testJvmArguments)
    }

    private fun createProject(): Project = ProjectBuilder.builder().build()

    private fun declaredDependencies(project: Project, configurationName: String): List<String> =
        project.configurations.getByName(configurationName).dependencies.map { dependency ->
            "${dependency.group}:${dependency.name}:${dependency.version}"
        }

    private companion object {
        const val DEVENG_TESTING_PLUGIN_ID = "global.deveng.testing"
        const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
        const val KOVER_PLUGIN_ID = "org.jetbrains.kotlinx.kover"
        const val ROBORAZZI_PLUGIN_ID = "io.github.takahirom.roborazzi"
        const val JAVA_PLUGIN_ID = "java"
        const val JAVA_TEST_TASK = "test"
        const val DESKTOP_TARGET = "desktop"
        const val COMMON_TEST_IMPLEMENTATION = "commonTestImplementation"
        const val DESKTOP_TEST_IMPLEMENTATION = "desktopTestImplementation"
        // Gradle moves these two system properties into the test JVM arguments.
        const val USER_LANGUAGE_PROPERTY = "user.language"
        const val USER_COUNTRY_PROPERTY = "user.country"
        const val EXPECTED_LANGUAGE = "en"
        const val EXPECTED_COUNTRY = "US"
        const val OTHER_LANGUAGE = "tr"
        const val OTHER_COUNTRY = "TR"
        const val CORE_TESTING_DEPENDENCY = "global.deveng:core-testing-kmp:${DevengTestingVersions.LIBRARY_VERSION}"
        const val KOTLIN_TEST_DEPENDENCY = "org.jetbrains.kotlin:kotlin-test:null"
        const val ROBORAZZI_DEPENDENCY =
            "io.github.takahirom.roborazzi:roborazzi-compose-desktop:${DevengTestingVersions.ROBORAZZI_VERSION}"
    }
}
