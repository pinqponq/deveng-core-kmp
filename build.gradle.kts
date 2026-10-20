plugins {
    alias(libs.plugins.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.vanniktech.mavenPublish).apply(false)
}

// See figma-sync/README.md Section 3.1 for full pipeline documentation
tasks.register("figmaSync") {
    group = "figma-sync"
    description = "Run schema discovery, template generation, and drift audit"
    dependsOn(
        ":figma-sync:tools:schema-cli:generateSchema",
        ":figma-sync:tools:manifest-cli:generateManifest",
        ":figma-sync:tools:template-generator:generateTemplates",
        ":figma-sync:tools:drift-auditor:auditDrift"
    )
}
