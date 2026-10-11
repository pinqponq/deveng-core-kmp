import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlin.serialization)
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
            implementation(project(":deveng-core"))
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.russhwolf.settings)
            api(libs.ktor.client.core)
        }

        androidMain.dependencies {
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.facebook.core)
            implementation(libs.install.referrer)
        }

        iosMain.dependencies {
            implementation(libs.gitlive.firebase.analytics)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.russhwolf.settings.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

android {
    namespace = "global.deveng.analytics"
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
        "analytics-kmp",
        version.toString()
    )

    pom {
        name = "Deveng Analytics KMP"
        description = "Conversion analytics (pinqloq, Firebase, Meta) and install attribution for Kotlin Multiplatform apps."
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
