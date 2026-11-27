plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("figmasync.auditor.DriftAuditorKt")
}

abstract class AuditDriftTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    @get:Option(option = "schema", description = "Path to component-schema.json")
    var schema: String = ""

    @get:Input
    @get:Option(option = "manifest", description = "Path to components.manifest.json")
    var manifest: String = ""

    @get:Input
    @get:Option(option = "report-json", description = "Output path for drift-report.json")
    var reportJsonPath: String = ""

    @get:Input
    @get:Option(option = "report-md", description = "Output path for drift-report.md")
    var reportMdPath: String = ""

    @TaskAction
    fun run() {
        execOperations.javaexec {
            classpath(this@AuditDriftTask.classpath)
            mainClass.set("figmasync.auditor.DriftAuditorKt")
            args(schema, manifest, reportJsonPath, reportMdPath)
        }
    }
}

val schemaPath = rootProject.layout.projectDirectory.file("figma-sync/schema/component-schema.json")
val manifestPath =
    rootProject.layout.projectDirectory.file("figma-sync/schema/components.manifest.json")
val reportJson = rootProject.layout.projectDirectory.file("figma-sync/schema/drift-report.json")
val reportMd = rootProject.layout.projectDirectory.file("figma-sync/schema/drift-report.md")

tasks.register<AuditDriftTask>("auditDrift") {
    group = "figma-sync"
    description = "Audit drift between schema and Figma components"
    classpath.from(sourceSets.main.get().runtimeClasspath)
    schema = schemaPath.asFile.absolutePath
    manifest = manifestPath.asFile.absolutePath
    reportJsonPath = reportJson.asFile.absolutePath
    reportMdPath = reportMd.asFile.absolutePath
    dependsOn(":figma-sync:tools:schema-cli:generateSchema")
}
