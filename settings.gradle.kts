rootProject.name = "deveng-core-kmp"

pluginManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
        // JOGL for the desktop WebView (KCEF) that compose-multiplatform-media-player depends on.
        exclusiveContent {
            forRepository {
                maven("https://jogamp.org/deployment/maven")
            }
            filter {
                includeGroupAndSubgroups("org.jogamp")
            }
        }
    }
}
plugins {
    //https://github.com/JetBrains/compose-hot-reload?tab=readme-ov-file#set-up-automatic-provisioning-of-the-jetbrains-runtime-jbr-via-gradle
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.10.0")
}

include(":sample:composeApp")
include(":deveng-core")
include(":deveng-billing")
include(":deveng-pinqloq")
include(":deveng-widget")
include(":deveng-core-testing")
include(":deveng-testing-gradle-plugin")

include(":figma-sync")
include(":figma-sync:tools:schema-cli")
include(":figma-sync:tools:template-generator")
include(":figma-sync:tools:drift-auditor")
include(":figma-sync:tools:manifest-cli")

