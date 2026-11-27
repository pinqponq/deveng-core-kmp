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
    mainClass.set("figmasync.manifest.ManifestCliMainKt")
}

// Configure the run task to use stdin for interactive mode
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    // Run from repo root so default relative paths work
    workingDir = rootProject.projectDir
}

abstract class ManifestTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    var workDir: String = ""

    @get:Input
    @get:Option(option = "schema", description = "Path to component-schema.json")
    var schema: String = ""

    @get:Input
    @get:Option(option = "manifest", description = "Path to components.manifest.json")
    var manifest: String = ""

    @get:Input
    @get:Option(option = "interactive", description = "Prompt for Figma URLs for new components")
    var interactive: Boolean = false

    @get:Input
    @get:Option(
        option = "merge",
        description = "Preserve manifest entries not in schema (default: replace)"
    )
    var merge: Boolean = false

    @get:Input
    @get:Optional
    var projectInteractive: String? = null

    @get:Input
    @get:Optional
    var projectMerge: String? = null

    @TaskAction
    fun run() {
        // Check both @Option and project property (workaround for boolean @Option issues)
        val interactiveFlag = interactive ||
                projectInteractive == "true" ||
                projectInteractive?.toBoolean() == true
        val mergeFlag = merge ||
                projectMerge == "true" ||
                projectMerge?.toBoolean() == true

        val argsList = mutableListOf("--schema", schema, "--manifest", manifest)
        if (interactiveFlag) {
            argsList.add("--interactive")
        }
        if (mergeFlag) argsList.add("--merge")

        execOperations.javaexec {
            classpath(this@ManifestTask.classpath)
            mainClass.set("figmasync.manifest.ManifestCliMainKt")
            workingDir(workDir)
            args(argsList)
            // Enable stdin for interactive mode
            standardInput = System.`in`
        }
    }
}

val schemaFile = rootProject.layout.projectDirectory.file("figma-sync/schema/component-schema.json")
val manifestFile =
    rootProject.layout.projectDirectory.file("figma-sync/schema/components.manifest.json")

tasks.register<ManifestTask>("generateManifest") {
    group = "figma-sync"
    description = "Generate/update manifest from schema, creating stubs for new components"
    classpath.from(sourceSets.main.get().runtimeClasspath)
    workDir = rootProject.projectDir.absolutePath
    schema = schemaFile.asFile.absolutePath
    manifest = manifestFile.asFile.absolutePath
    // Read project properties at configuration time (configuration cache safe)
    projectInteractive = rootProject.findProperty("interactive") as? String
    projectMerge = rootProject.findProperty("merge") as? String
    dependsOn(":figma-sync:tools:schema-cli:generateSchema")
}
