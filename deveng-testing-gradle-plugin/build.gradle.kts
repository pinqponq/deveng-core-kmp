import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "global.deveng"
version = generateVersionName()

kotlin {
    jvmToolchain(17)
}

val generatedVersionSourceDirectory = layout.buildDirectory.dir("generated/devengTestingVersions/kotlin")

val generateDevengTestingVersions by tasks.registering {
    val libraryVersion = version.toString()
    val roborazziVersion = libs.versions.roborazzi.get()
    val outputDirectory = generatedVersionSourceDirectory
    inputs.property("libraryVersion", libraryVersion)
    inputs.property("roborazziVersion", roborazziVersion)
    outputs.dir(outputDirectory)
    doLast {
        val versionFile = outputDirectory.get().file("core/testing/gradle/DevengTestingVersions.kt").asFile
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            |package core.testing.gradle
            |
            |internal object DevengTestingVersions {
            |    const val LIBRARY_VERSION = "$libraryVersion"
            |    const val ROBORAZZI_VERSION = "$roborazziVersion"
            |}
            |""".trimMargin()
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateDevengTestingVersions)
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.roborazzi.gradle.plugin)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlin.gradle.plugin)
    testImplementation(gradleKotlinDsl())
}

gradlePlugin {
    plugins {
        create("devengTesting") {
            id = "global.deveng.testing"
            implementationClass = "core.testing.gradle.DevengTestingPlugin"
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(
        group.toString(),
        "testing-gradle-plugin",
        version.toString()
    )

    pom {
        name = "Deveng Testing Gradle Plugin"
        description = "Turns on the deveng test setup (core-testing, Kover, Roborazzi, pinned test locale) for a Kotlin Multiplatform module."
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
