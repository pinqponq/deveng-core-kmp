import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kover)
}

group = "global.deveng"
version = generateVersionName()

kotlin {
    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":deveng-core"))
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)
            implementation(compose.runtime)
        }

        val desktopMain by getting {
            dependencies {
                api(kotlin("test-junit"))
                @OptIn(ExperimentalComposeLibrary::class)
                api(compose.uiTest)
                api(compose.components.resources)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
            }
        }
    }
}

// Same locale the global.deveng.testing plugin pins; that plugin is built in this build, so this
// module cannot apply it.
val testLanguage = "en"
val testCountry = "US"

tasks.withType<Test>().configureEach {
    systemProperty("user.language", testLanguage)
    systemProperty("user.country", testCountry)
}

android {
    namespace = "global.deveng.testing"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(
        group.toString(),
        "core-testing-kmp",
        version.toString()
    )

    pom {
        name = "Deveng Core Testing KMP"
        description = "Test environment, fakes and Compose test helpers for projects built on deveng-core-kmp."
        inceptionYear = "2026"
        url = "https://github.com/Deveng-Group/deveng-core-kmp/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "furkanturkn"
                name = "Furkan Türkan"
                url = "https://github.com/furkanturkn/"
            }
        }
        scm {
            url = "https://github.com/Deveng-Group/deveng-core-kmp/"
            connection = "scm:git:git://github.com/Deveng-Group/deveng-core-kmp.git"
            developerConnection =
                "scm:git:ssh://git@github.com/Deveng-Group/deveng-core-kmp.git"
        }
    }
}

fun generateVersionName(): String {
    val versionMajor = libs.versions.app.version.major.get()
    val appVersionCode = libs.versions.app.version.code.get()
    val minorVersion = libs.versions.app.minor.version.get()

    return StringBuilder().apply {
        append(versionMajor)
        append(".")
        append(appVersionCode)
        append(".")
        append(minorVersion)
    }.toString()
}
