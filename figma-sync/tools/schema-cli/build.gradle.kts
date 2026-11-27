plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

val kotlinVersion = libs.versions.kotlin.get()

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
    implementation(libs.kotlinx.datetime)

    // Kotlin compiler embeddable for parsing Kotlin source files
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("figmasync.cli.SchemaCliMainKt")
}

abstract class GenerateSchemaTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    var workDir: String = ""

    @get:Input
    @get:Option(option = "discover-all", description = "Discover all components and drawables")
    var discoverAll: Boolean = false

    @get:Input
    @get:Optional
    @get:Option(option = "component-dir", description = "Root directory for @Composable discovery")
    var componentDir: String? = null

    @get:Input
    @get:Optional
    @get:Option(option = "drawable-dir", description = "Drawable directory to scan (repeatable)")
    var drawableDirs: MutableList<String> = mutableListOf()

    @get:Input
    @get:Optional
    @get:Option(option = "schema-out", description = "Output path for final schema JSON")
    var schemaOut: String? = null

    @get:Input
    var mappingDefault: String = ""

    @get:Input
    @get:Optional
    @get:Option(option = "mapping", description = "Type mapping file path")
    var mapping: String? = null

    @get:Input
    var overridesDefault: String = ""

    @get:Input
    @get:Optional
    @get:Option(option = "overrides", description = "Schema overrides file path")
    var overridesPath: String? = null

    @get:Input
    @get:Optional
    @get:Option(option = "input", description = "Kotlin file (.kt) or drawable resource path")
    var input: String? = null

    @get:Input
    @get:Optional
    @get:Option(
        option = "component",
        description = "Component name if multiple @Composable functions"
    )
    var component: String? = null

    @get:Input
    @get:Optional
    @get:Option(option = "figma-url", description = "Figma component URL (drawable mode)")
    var figmaUrl: String? = null

    @get:Input
    @get:Optional
    @get:Option(option = "template-out", description = "Output path for drawable template")
    var templateOut: String? = null

    @get:Input
    @get:Optional
    @get:Option(option = "mode", description = "Mode (e.g., canonicalize)")
    var mode: String? = null

    @get:Input
    @get:Optional
    @get:Option(option = "raw", description = "Raw schema JSON input file")
    var raw: String? = null

    @get:Input
    @get:Optional
    @get:Option(
        option = "out",
        description = "Output path for final schema JSON (canonicalize mode)"
    )
    var out: String? = null

    @get:Input
    @get:Optional
    var passthroughArgs: String? = null

    @TaskAction
    fun run() {
        val argsList = mutableListOf<String>()

        val mappingPath = mapping ?: mappingDefault
        val overridesArg = overridesPath ?: overridesDefault

        // Check if -Pargs was provided (for direct argument passing)
        if (passthroughArgs != null && passthroughArgs!!.isNotBlank()) {
            // Parse -Pargs string and pass directly to main function
            // Simple split by whitespace (handles most cases; paths with spaces need quoting in shell)
            val parsedArgs =
                passthroughArgs!!.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            // Add parsed args and defaults
            argsList.addAll(parsedArgs)
            argsList += listOf("--mapping", mappingPath)
            argsList += listOf("--overrides", overridesArg)
        } else {
            // Original option-based logic
            val isCanonicalize = (mode == "canonicalize") || raw != null || out != null
            if (isCanonicalize) {
                argsList += "--mode=canonicalize"
                raw?.let { argsList += listOf("--raw", it) }
                out?.let { argsList += listOf("--out", it) }
            } else if (input != null) {
                argsList += listOf("--input", input!!)
                component?.let { argsList += listOf("--component", it) }
                figmaUrl?.let { argsList += listOf("--figma-url", it) }
                templateOut?.let { argsList += listOf("--template-out", it) }
                schemaOut?.let { argsList += listOf("--schema-out", it) }
            } else {
                // discover-all is default when nothing else is set
                argsList += "--discover-all"
                componentDir?.let { argsList += listOf("--component-dir", it) }
                drawableDirs.forEach { argsList += listOf("--drawable-dir", it) }
                schemaOut?.let { argsList += listOf("--schema-out", it) }
            }

            argsList += listOf("--mapping", mappingPath)
            argsList += listOf("--overrides", overridesArg)
        }

        execOperations.javaexec {
            classpath(this@GenerateSchemaTask.classpath)
            mainClass.set("figmasync.cli.SchemaCliMainKt")
            workingDir(workDir)
            args(argsList)
        }
    }
}

// Default paths for schema files
val rawSchema =
    rootProject.layout.projectDirectory.file("figma-sync/schema/component-schema.raw.json")
val overridesFile =
    rootProject.layout.projectDirectory.file("figma-sync/schema/schema.overrides.json")
val finalSchema =
    rootProject.layout.projectDirectory.file("figma-sync/schema/component-schema.json")
val typeMappingFile =
    rootProject.layout.projectDirectory.file("figma-sync/schema/type-mapping.json")
val templatesDir = rootProject.layout.projectDirectory.dir("figma-sync/templates")

// See figma-sync/README.md Section 5.2 for full argument documentation
tasks.register<GenerateSchemaTask>("generateSchema") {
    group = "figma-sync"
    description = "Extract schema from Kotlin composable or drawable, then canonicalize"
    classpath.from(sourceSets.main.get().runtimeClasspath)
    workDir = rootProject.projectDir.absolutePath
    mappingDefault = typeMappingFile.asFile.absolutePath
    overridesDefault = overridesFile.asFile.absolutePath
    // Set passthroughArgs from project property at configuration time (configuration cache safe)
    passthroughArgs = (project.findProperty("args") as? String)?.takeIf { it.isNotBlank() }
}

// See figma-sync/README.md Section 5.3 for full argument documentation
tasks.register<GenerateSchemaTask>("generateSchemaManuel") {
    group = "figma-sync"
    description = "Canonicalize raw JSON schema (manual/AI workflow)"
    classpath.from(sourceSets.main.get().runtimeClasspath)
    workDir = rootProject.projectDir.absolutePath
    mode = "canonicalize"
    mappingDefault = typeMappingFile.asFile.absolutePath
    overridesDefault = overridesFile.asFile.absolutePath
}

// @deprecated Use generateSchemaManuel instead
tasks.register<JavaExec>("generateSchemaLegacy") {
    group = "figma-sync"
    description =
        "[DEPRECATED] Use generateSchemaManuel for raw->canonicalize. This runs old behavior."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("figmasync.cli.SchemaCliMainKt")
    args = listOf(
        "--mode=canonicalize",
        "--raw", rawSchema.asFile.absolutePath,
        "--overrides", overridesFile.asFile.absolutePath,
        "--out", finalSchema.asFile.absolutePath
    )
}
