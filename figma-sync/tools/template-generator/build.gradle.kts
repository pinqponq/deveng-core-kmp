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
    mainClass.set("figmasync.generator.TemplateGeneratorKt")
}

abstract class GenerateTemplatesTask @Inject constructor(
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
    @get:Option(option = "templates", description = "Output directory for templates")
    var templates: String = ""

    @TaskAction
    fun run() {
        execOperations.javaexec {
            classpath(this@GenerateTemplatesTask.classpath)
            mainClass.set("figmasync.generator.TemplateGeneratorKt")
            args(schema, manifest, templates)
        }
    }
}

val schemaPath = rootProject.layout.projectDirectory.file("figma-sync/schema/component-schema.json")
val templatesDir = rootProject.layout.projectDirectory.dir("figma-sync/templates")
val manifestPath =
    rootProject.layout.projectDirectory.file("figma-sync/schema/components.manifest.json")

tasks.register<GenerateTemplatesTask>("generateTemplates") {
    group = "figma-sync"
    description = "Generate Code Connect templates from component schema"
    classpath.from(sourceSets.main.get().runtimeClasspath)
    schema = schemaPath.asFile.absolutePath
    manifest = manifestPath.asFile.absolutePath
    templates = templatesDir.asFile.absolutePath
    inputs.file(schemaPath)
    inputs.file(manifestPath)
    outputs.dir(templatesDir)
    dependsOn(":figma-sync:tools:schema-cli:generateSchema")
    dependsOn(":figma-sync:tools:manifest-cli:generateManifest")
}
