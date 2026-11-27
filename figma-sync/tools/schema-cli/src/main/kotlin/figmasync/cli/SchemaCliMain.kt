package figmasync.cli

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

// ============================================================================
// JSON Configuration
// ============================================================================

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
}

// ============================================================================
// Schema Data Types
// ============================================================================

@Serializable
data class SchemaFile(
    val schemaVersion: Int,
    val generatedAt: String,
    val git: GitMetadata,
    val components: List<Component>
)

@Serializable
data class GitMetadata(val commit: String)

@Serializable
data class Component(
    val componentName: String,
    val kotlinFqName: String,
    val codeConnect: CodeConnect,
    val params: List<Param>
)

@Serializable
data class CodeConnect(val nestable: Boolean)

@Serializable
data class Param(
    val name: String,
    val kind: String,
    val nullable: Boolean,
    val required: Boolean = false,
    val default: JsonElement? = null,
    val defaultSource: String,
    val binding: Binding,
    val layerHint: LayerHint? = null,
    val variantAxis: VariantAxis? = null,
    val instanceSwap: InstanceSwap? = null,
    val enum: EnumConfig? = null,
    val callbackArity: Int? = null
)

@Serializable
data class Binding(val field: String)

@Serializable
data class LayerHint(val marker: String)

@Serializable
data class VariantAxis(
    val propertyName: String? = null,
    val valueMap: Map<String, Boolean>? = null
)

@Serializable
data class InstanceSwap(
    val targetLayerMarker: String? = null,
    val contract: String? = null
)

@Serializable
data class EnumConfig(
    val domain: List<String>,
    val templateMap: Map<String, String>? = null
)

@Serializable
data class Override(
    val component: String,
    val param: String,
    val forceKind: String? = null,
    val forceBinding: Binding? = null,
    val layerHint: LayerHint? = null,
    val instanceSwap: InstanceSwap? = null,
    val variantAxis: VariantAxis? = null,
    val enum: EnumConfig? = null,
    val exclude: Boolean? = null
)

@Serializable
data class OverridesFile(
    val version: Int,
    val overrides: List<Override>
)

// ============================================================================
// Type Mapping Data Types
// ============================================================================

@Serializable
data class TypeMappingFile(
    val version: Int,
    val description: String,
    val mappings: Map<String, TypeMapping>,
    val excludedTypes: List<String>,
    val notes: Map<String, String>? = null
)

@Serializable
data class TypeMapping(
    val kind: String,
    val binding: String,
    val supportsLiteralDefault: Boolean,
    val literalDefaultType: String? = null,
    val variantAxis: VariantAxisConfig? = null,
    val instanceSwap: InstanceSwapConfig? = null,
    val callbackArity: Int? = null,
    val forceRequired: Boolean? = null
)

@Serializable
data class VariantAxisConfig(
    val derivePropertyNameFromParam: Boolean? = null,
    val defaultValueMap: Map<String, Boolean>? = null
)

@Serializable
data class InstanceSwapConfig(
    val deriveTargetLayerMarkerFromParam: Boolean? = null,
    val contract: String? = null
)

// ============================================================================
// CLI Argument Parsing
// ============================================================================

sealed class CliCommand {
    data class Extract(
        val inputPath: String,
        val componentName: String?,
        val figmaUrl: String?,
        val schemaOut: String?,
        val templateOut: String?,
        val mappingPath: String,
        val overridesPath: String
    ) : CliCommand()

    data class Canonicalize(
        val rawPath: String,
        val overridesPath: String,
        val outputPath: String,
        val mappingPath: String
    ) : CliCommand()

    data class DiscoverAll(
        val componentDir: String,
        val drawableDirs: List<String>,
        val schemaOut: String,
        val mappingPath: String,
        val overridesPath: String
    ) : CliCommand()

    object Help : CliCommand()
}

fun parseArgs(args: Array<String>): CliCommand {
    if (args.contains("--help") || args.contains("-h")) {
        return CliCommand.Help
    }

    val argMap = mutableMapOf<String, String?>()
    val drawableDirs = mutableListOf<String>()
    var componentDirOverride: String? = null
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg.startsWith("--drawable-dir=") -> {
                val value = arg.substringAfter("=", "")
                if (value.isNotBlank()) drawableDirs.add(value)
                i++
            }

            arg == "--drawable-dir" -> {
                val value = args.getOrNull(i + 1)
                if (value != null && !value.startsWith("--")) {
                    drawableDirs.add(value)
                    i += 2
                } else {
                    i++
                }
            }

            arg.startsWith("--component-dir=") -> {
                val value = arg.substringAfter("=", "")
                if (value.isNotBlank()) componentDirOverride = value
                i++
            }

            arg == "--component-dir" -> {
                val value = args.getOrNull(i + 1)
                if (value != null && !value.startsWith("--")) {
                    componentDirOverride = value
                    i += 2
                } else {
                    i++
                }
            }

            arg.startsWith("--") && arg.contains("=") -> {
                val (key, value) = arg.substring(2).split("=", limit = 2)
                argMap[key] = value
                i++
            }

            arg.startsWith("--") -> {
                val key = arg.substring(2)
                val value = args.getOrNull(i + 1)?.takeIf { !it.startsWith("--") }
                argMap[key] = value
                i += if (value != null) 2 else 1
            }

            else -> i++
        }
    }

    // Default paths (relative to typical project root)
    val defaultMappingPath = "figma-sync/schema/type-mapping.json"
    val defaultOverridesPath = "figma-sync/schema/schema.overrides.json"
    val defaultSchemaOut = "figma-sync/schema/component-schema.json"
    val defaultComponentDir = "deveng-core/src/commonMain/kotlin/core/presentation/component"
    val defaultDrawableDirs = listOf(
        "deveng-core/src/commonMain/composeResources/drawable",
        "sample/composeApp/src/commonMain/composeResources/drawable"
    )

    val mode = argMap["mode"]
    val discoverAll =
        args.isEmpty() || argMap.containsKey("discover-all") || argMap.containsKey("discover")

    return when {
        mode == "canonicalize" || argMap.containsKey("raw") -> {
            val rawPath = argMap["raw"] ?: error("--raw is required for canonicalize mode")
            CliCommand.Canonicalize(
                rawPath = rawPath,
                overridesPath = argMap["overrides"] ?: defaultOverridesPath,
                outputPath = argMap["out"] ?: defaultSchemaOut,
                mappingPath = argMap["mapping"] ?: defaultMappingPath
            )
        }

        argMap.containsKey("input") -> {
            CliCommand.Extract(
                inputPath = argMap["input"]!!,
                componentName = argMap["component"],
                figmaUrl = argMap["figma-url"],
                schemaOut = argMap["schema-out"],
                templateOut = argMap["template-out"],
                mappingPath = argMap["mapping"] ?: defaultMappingPath,
                overridesPath = argMap["overrides"] ?: defaultOverridesPath
            )
        }

        discoverAll -> {
            val componentDir = componentDirOverride ?: defaultComponentDir
            val drawableDirList =
                if (drawableDirs.isNotEmpty()) drawableDirs else defaultDrawableDirs
            CliCommand.DiscoverAll(
                componentDir = componentDir,
                drawableDirs = drawableDirList,
                schemaOut = argMap["schema-out"] ?: defaultSchemaOut,
                mappingPath = argMap["mapping"] ?: defaultMappingPath,
                overridesPath = argMap["overrides"] ?: defaultOverridesPath
            )
        }

        else -> CliCommand.Help
    }
}

fun printHelp() {
    println(
        """
        |Schema CLI - Figma Code Connect Schema Generator
        |
        |USAGE:
        |  generateSchema (extract + canonicalize):
        |    --input <path>         Kotlin file (.kt) or drawable resource path [required]
        |    --component <name>     Component name if multiple @Composable exist
        |    --figma-url <url>      Figma component URL (for drawable mode)
        |    --schema-out <path>    Output path for final schema JSON
        |    --template-out <path>  Output path for drawable template
        |    --mapping <path>       Type mapping file (default: figma-sync/schema/type-mapping.json)
        |    --overrides <path>     Overrides file (default: figma-sync/schema/schema.overrides.json)
        |
        |  generateSchema (discover-all mode, default when no args):
        |    --discover-all         Discover all components and drawables automatically
        |    --component-dir <dir>  Root dir for @Composable discovery (default: deveng-core/src/commonMain/kotlin/core/presentation/component)
        |    --drawable-dir <dir>   Drawable roots (repeatable, default: deveng-core/src/commonMain/composeResources/drawable and sample/composeApp/src/commonMain/composeResources/drawable)
        |    --schema-out <path>    Output path for final schema JSON
        |    --mapping <path>       Type mapping file
        |    --overrides <path>     Overrides file
        |
        |  generateSchemaManuel (raw -> canonicalize only):
        |    --mode=canonicalize    Force canonicalize-only mode
        |    --raw <path>           Raw schema JSON input file [required]
        |    --overrides <path>     Overrides file path
        |    --out <path>           Output final schema JSON
        |    --mapping <path>       Type mapping file path
        |
        |EXAMPLES:
        |  Kotlin composable:
        |    ./gradlew :figma-sync:tools:schema-cli:generateSchema \
        |      --args="--input deveng-core/src/commonMain/kotlin/core/presentation/component/CustomIconButton.kt"
        |
        |  Drawable icon:
        |    ./gradlew :figma-sync:tools:schema-cli:generateSchema \
        |      --args="--input sample/composeApp/src/commonMain/composeResources/drawable/ic_cyclone.xml \
        |              --figma-url https://www.figma.com/..."
        |
        |  Manual raw JSON:
        |    ./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel \
        |      --args="--raw figma-sync/schema/component-schema.raw.json \
        |              --out figma-sync/schema/component-schema.json"
    """.trimMargin()
    )
}

// ============================================================================
// Main Entry Point
// ============================================================================

fun main(args: Array<String>) {
    // Legacy mode: if exactly 3 args and all are paths (old behavior)
    if (args.size == 3 && args.all { !it.startsWith("--") }) {
        runLegacyCanonicalize(args[0], args[1], args[2])
        return
    }

    when (val command = parseArgs(args)) {
        is CliCommand.Help -> printHelp()
        is CliCommand.Extract -> runExtract(command)
        is CliCommand.Canonicalize -> runCanonicalize(command)
        is CliCommand.DiscoverAll -> runDiscoverAll(command)
    }
}

// ============================================================================
// Legacy Canonicalize (backward compatibility)
// ============================================================================

private fun runLegacyCanonicalize(rawPath: String, overridesPath: String, outputPath: String) {
    val raw = Path(rawPath)
    val overrides = Path(overridesPath)
    val output = Path(outputPath)

    if (!raw.exists()) error("Raw schema not found at $raw")
    if (!overrides.exists()) error("Overrides file not found at $overrides")

    val schema = canonicalizeSchema(
        rawSchema = json.decodeFromString<SchemaFile>(raw.readText()),
        overrides = json.decodeFromString<OverridesFile>(overrides.readText())
    )

    ensureParent(output)
    output.writeText(json.encodeToString(schema))
    println("✓ Generated schema at $output")
}

// ============================================================================
// Canonicalize Command (raw -> final)
// ============================================================================

private fun runCanonicalize(command: CliCommand.Canonicalize) {
    val rawPath = Path(command.rawPath)
    val overridesPath = Path(command.overridesPath)
    val outputPath = Path(command.outputPath)

    if (!rawPath.exists()) error("Raw schema not found at $rawPath")
    if (!overridesPath.exists()) error("Overrides file not found at $overridesPath")

    val schema = canonicalizeSchema(
        rawSchema = json.decodeFromString<SchemaFile>(rawPath.readText()),
        overrides = json.decodeFromString<OverridesFile>(overridesPath.readText())
    )

    ensureParent(outputPath)
    outputPath.writeText(json.encodeToString(schema))
    println("✓ Canonicalized schema written to $outputPath")
}

// ============================================================================
// Extract Command (Kotlin/Drawable -> raw -> final)
// ============================================================================

private fun runExtract(command: CliCommand.Extract) {
    val inputPath = Path(command.inputPath)
    if (!inputPath.exists()) error("Input file not found at $inputPath")

    val mappingPath = Path(command.mappingPath)
    if (!mappingPath.exists()) error("Type mapping file not found at $mappingPath")

    val typeMapping = json.decodeFromString<TypeMappingFile>(mappingPath.readText())

    when {
        isDrawableResource(command.inputPath) -> {
            runDrawableExtract(command, typeMapping)
        }

        command.inputPath.endsWith(".kt") -> {
            runKotlinExtract(command, typeMapping)
        }

        else -> {
            error("Unrecognized input type. Expected .kt file or drawable resource (ic_*.xml/png/svg)")
        }
    }
}

// ============================================================================
// Drawable Extraction
// ============================================================================

private fun isDrawableResource(path: String): Boolean {
    val fileName = Path(path).fileName.toString().lowercase()
    val isDrawablePath = path.contains("/drawable") || path.contains("\\drawable")
    val isIconPattern = fileName.startsWith("ic_") || fileName.startsWith("shared_ic_")
    val isResourceFile = fileName.endsWith(".xml") || fileName.endsWith(".png") ||
            fileName.endsWith(".svg") || fileName.endsWith(".webp")
    return (isDrawablePath || isIconPattern) && isResourceFile
}

private fun runDrawableExtract(command: CliCommand.Extract, typeMapping: TypeMappingFile) {
    val inputPath = Path(command.inputPath)
    val fileName = inputPath.fileName.toString()
    val resourceName = fileName.substringBeforeLast(".")

    // Generate icon name in PascalCase for template file
    val iconName = resourceName
        .split("_")
        .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    // Determine Figma URL
    val figmaUrl = command.figmaUrl ?: promptForFigmaUrl()

    // Generate template content
    val templateContent = generateDrawableTemplate(resourceName, figmaUrl)

    // Determine output path
    val outputPath = when {
        command.templateOut != null -> Path(command.templateOut)
        else -> {
            // Use repo convention: figma-sync/templates/icons/<IconName>.figma.template.js
            val templatesDir = Path("figma-sync/templates/icons")
            templatesDir.resolve("$iconName.figma.template.js")
        }
    }

    ensureParent(outputPath)
    outputPath.writeText(templateContent)
    println("✓ Generated drawable template at $outputPath")
    println("  Resource: Res.drawable.$resourceName")
}

private fun promptForFigmaUrl(): String? {
    return if (System.console() != null) {
        print("Enter Figma component URL (or press Enter to skip): ")
        System.console()?.readLine()?.takeIf { it.isNotBlank() }
    } else {
        null
    }
}

private fun generateDrawableTemplate(resourceName: String, figmaUrl: String?): String {
    val urlLine = if (figmaUrl != null) {
        "// url=$figmaUrl"
    } else {
        "// url=<PASTE FIGMA ICON COMPONENT URL HERE>"
    }

    return """
        |$urlLine
        |const figma = require('figma')
        |
        |export default {
        |  example: figma.code`Res.drawable.$resourceName`,
        |  metadata: {
        |    // show inline when nested
        |    nestable: true,
        |    // this is what parents will consume
        |    props: {
        |      drawable: "Res.drawable.$resourceName",
        |    },
        |  },
        |}
    """.trimMargin()
}

// ============================================================================
// Kotlin Composable Extraction
// ============================================================================

private fun runKotlinExtract(command: CliCommand.Extract, typeMapping: TypeMappingFile) {
    val inputPath = Path(command.inputPath)
    val kotlinSource = inputPath.readText()

    // Parse Kotlin source to extract composable functions
    val composables = parseKotlinComposables(kotlinSource, typeMapping)

    if (composables.isEmpty()) {
        error("No public @Composable functions found in ${command.inputPath}")
    }

    // Select the composable to extract
    val selectedComposable = when {
        composables.size == 1 -> composables.first()
        command.componentName != null -> {
            composables.find { it.componentName == command.componentName }
                ?: error("Component '${command.componentName}' not found. Available: ${composables.map { it.componentName }}")
        }

        System.console() != null -> {
            println("Multiple @Composable functions found:")
            composables.forEachIndexed { index, comp ->
                println("  ${index + 1}. ${comp.componentName}")
            }
            print("Select component (1-${composables.size}): ")
            val selection = System.console()?.readLine()?.toIntOrNull()
            if (selection == null || selection < 1 || selection > composables.size) {
                error("Invalid selection")
            }
            composables[selection - 1]
        }

        else -> {
            error("Multiple @Composable functions found: ${composables.map { it.componentName }}. Use --component to select one.")
        }
    }

    // Load overrides if available
    val overridesPath = Path(command.overridesPath)
    val overrides = if (overridesPath.exists()) {
        json.decodeFromString<OverridesFile>(overridesPath.readText())
    } else {
        OverridesFile(version = 1, overrides = emptyList())
    }

    // Determine output path
    val schemaOutPath = Path(command.schemaOut ?: "figma-sync/schema/component-schema.json")

    // Create fresh schema (replaces existing file)
    val rawSchema = SchemaFile(
        schemaVersion = 1,
        generatedAt = "1970-01-01T00:00:00Z",
        git = GitMetadata(commit = "unknown"),
        components = listOf(selectedComposable)
    )

    // Canonicalize
    val finalSchema = canonicalizeSchema(rawSchema, overrides)

    // Write output
    ensureParent(schemaOutPath)
    schemaOutPath.writeText(json.encodeToString(finalSchema))
    println("✓ Generated schema for ${selectedComposable.componentName} at $schemaOutPath")
}

// ============================================================================
// Discover-All Command
// ============================================================================

internal fun discoverKotlinComponents(componentDir: Path): List<Path> {
    if (!componentDir.exists()) return emptyList()
    val results = mutableListOf<Path>()
    Files.walk(componentDir).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
            .forEach { results.add(it) }
    }
    return results.sortedBy { it.toString() }
}

internal fun discoverDrawableResources(drawableDir: Path): List<Path> {
    if (!drawableDir.exists()) return emptyList()
    val results = mutableListOf<Path>()
    Files.walk(drawableDir).use { stream ->
        stream.filter { Files.isRegularFile(it) && isDrawableResource(it.toString()) }
            .forEach { results.add(it) }
    }
    return results.sortedBy { it.toString() }
}

private fun runDiscoverAll(command: CliCommand.DiscoverAll) {
    val componentDir = Path(command.componentDir)
    val drawableDirs = command.drawableDirs.map { Path(it) }

    val mappingPath = Path(command.mappingPath)
    if (!mappingPath.exists()) error("Type mapping file not found at $mappingPath")
    val typeMapping = json.decodeFromString<TypeMappingFile>(mappingPath.readText())

    val overridesPath = Path(command.overridesPath)
    val overrides = if (overridesPath.exists()) {
        json.decodeFromString<OverridesFile>(overridesPath.readText())
    } else {
        OverridesFile(version = 1, overrides = emptyList())
    }

    val kotlinFiles = discoverKotlinComponents(componentDir)
    val components = mutableListOf<Component>()
    kotlinFiles.forEach { file ->
        val source = file.readText()
        components.addAll(parseKotlinComposables(source, typeMapping))
    }

    if (components.isEmpty()) {
        error("No @Composable components found under $componentDir")
    }

    val rawSchema = SchemaFile(
        schemaVersion = 1,
        generatedAt = "1970-01-01T00:00:00Z",
        git = GitMetadata(commit = "unknown"),
        components = components
    )

    val finalSchema = canonicalizeSchema(rawSchema, overrides)

    val schemaOutPath = Path(command.schemaOut)
    ensureParent(schemaOutPath)
    schemaOutPath.writeText(json.encodeToString(finalSchema))
    println("✓ Generated schema for ${components.size} components at $schemaOutPath")

    val drawablePaths =
        drawableDirs.flatMap { discoverDrawableResources(it) }.distinctBy { it.toString() }
    if (drawablePaths.isNotEmpty()) {
        drawablePaths.forEach { drawablePath ->
            val fileName = drawablePath.fileName.toString()
            val resourceName = fileName.substringBeforeLast(".")
            val iconName = resourceName
                .split("_")
                .joinToString("") { part -> part.replaceFirstChar { c -> c.uppercaseChar() } }
            val outputPath =
                Path("figma-sync/templates/icons").resolve("$iconName.figma.template.js")
            val templateContent = generateDrawableTemplate(resourceName, figmaUrl = null)
            ensureParent(outputPath)
            outputPath.writeText(templateContent)
            println("✓ Generated drawable template at $outputPath for $resourceName")
        }
    } else {
        println("No drawable resources found in ${drawableDirs.joinToString()}")
    }
}

// ============================================================================
// Kotlin Parser (using regex-based approach for simplicity, with validation)
// Note: For production use, consider kotlin-compiler-embeddable PSI parsing
// ============================================================================

internal fun parseKotlinComposables(source: String, typeMapping: TypeMappingFile): List<Component> {
    val components = mutableListOf<Component>()

    // Pattern to match @Composable function declarations
    // This handles multi-line function signatures
    val composablePattern = Regex(
        """@Composable\s+(?:(?:public|internal|private)\s+)?fun\s+(\w+)\s*\(([\s\S]*?)\)\s*(?::\s*\w+)?\s*\{""",
        RegexOption.MULTILINE
    )

    // Extract package name for kotlinFqName
    val packagePattern = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    val packageName = packagePattern.find(source)?.groupValues?.get(1) ?: ""

    composablePattern.findAll(source).forEach { match ->
        val functionName = match.groupValues[1]
        val paramsBlock = match.groupValues[2]

        // Skip private functions and Preview functions
        val functionDecl = match.value
        if (functionDecl.contains("private ") || functionName.endsWith("Preview")) {
            return@forEach
        }

        val params = parseParameters(paramsBlock, typeMapping)

        // Check if any parameter is a slot/content type (makes component nestable)
        val hasSlotParameter = hasComposableSlotParameter(paramsBlock)

        val component = Component(
            componentName = functionName,
            kotlinFqName = if (packageName.isNotEmpty()) "$packageName.$functionName" else functionName,
            codeConnect = CodeConnect(nestable = hasSlotParameter),
            params = params
        )

        components.add(component)
    }

    return components
}

/**
 * Detects if the parameter block contains a composable slot/content parameter.
 * Slot parameters are:
 * - Type `Slot` (typealias for @Composable () -> Unit)
 * - Type `@Composable () -> Unit` or variations with receiver (e.g., `@Composable RowScope.() -> Unit`)
 * - Common slot parameter names: content, slot, icon (when composable)
 */
internal fun hasComposableSlotParameter(paramsBlock: String): Boolean {
    // Normalize whitespace for easier matching
    val normalized = paramsBlock.replace(Regex("\\s+"), " ")

    // Pattern 1: Slot type (typealias)
    if (Regex("""\bSlot\b""").containsMatchIn(normalized)) {
        return true
    }

    // Pattern 2: @Composable lambda types like @Composable () -> Unit, @Composable RowScope.() -> Unit
    if (Regex("""@Composable\s*(\w+\.)?\(\)\s*->\s*Unit""").containsMatchIn(normalized)) {
        return true
    }

    // Pattern 3: @Composable annotated parameter (general case)
    // e.g., content: @Composable () -> Unit, header: @Composable ColumnScope.() -> Unit
    if (Regex(""":\s*@Composable\s+""").containsMatchIn(normalized)) {
        return true
    }

    return false
}

internal fun parseParameters(paramsBlock: String, typeMapping: TypeMappingFile): List<Param> {
    val params = mutableListOf<Param>()

    // Normalize whitespace
    val normalized = paramsBlock.replace(Regex("\\s+"), " ").trim()
    if (normalized.isEmpty()) return params

    // Split by comma, handling nested generics and lambdas
    val paramStrings = splitParameters(normalized)

    for (paramStr in paramStrings) {
        val param = parseParameter(paramStr.trim(), typeMapping)
        if (param != null) {
            params.add(param)
        }
    }

    return params
}

private fun splitParameters(input: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var current = StringBuilder()

    for (char in input) {
        when (char) {
            '(', '<', '{' -> {
                depth++
                current.append(char)
            }

            ')', '>', '}' -> {
                depth--
                current.append(char)
            }

            ',' -> {
                if (depth == 0) {
                    result.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(char)
                }
            }

            else -> current.append(char)
        }
    }

    if (current.isNotBlank()) {
        result.add(current.toString())
    }

    return result
}

internal fun parseParameter(paramStr: String, typeMapping: TypeMappingFile): Param? {
    // Pattern: name: Type = default  OR  name: Type
    val paramPattern = Regex("""(\w+)\s*:\s*([^=]+?)(?:\s*=\s*(.+))?$""")
    val match = paramPattern.find(paramStr.trim()) ?: return null

    val name = match.groupValues[1].trim()
    var typeStr = match.groupValues[2].trim()
    // Note: If the optional group doesn't match, groupValues still contains an empty string
    val defaultValueStr = match.groupValues.getOrNull(3)?.trim()?.ifEmpty { null }

    // Check for nullable
    val isNullable = typeStr.endsWith("?")
    if (isNullable) {
        typeStr = typeStr.dropLast(1)
    }

    // Normalize lambda types
    val normalizedType = normalizeLambdaType(typeStr)

    // Check if type should be excluded
    if (typeMapping.excludedTypes.contains(typeStr) ||
        typeMapping.excludedTypes.contains(normalizedType)
    ) {
        return null
    }

    // Find type mapping
    val mapping = typeMapping.mappings[typeStr]
        ?: typeMapping.mappings[normalizedType]
        ?: return null // Type not in mapping, skip

    // Determine if required (no default value and not nullable)
    val hasDefault = defaultValueStr != null
    val required = mapping.forceRequired == true || (!hasDefault && !isNullable)

    // Parse default value if supported
    val (defaultValue, defaultSource) = when {
        !mapping.supportsLiteralDefault -> null to "unknown"
        defaultValueStr == null -> null to "unknown"
        defaultValueStr == "null" -> JsonNull to "literal"
        else -> parseLiteralDefault(defaultValueStr, mapping.literalDefaultType)
    }

    // Build param
    return Param(
        name = name,
        kind = mapping.kind,
        nullable = isNullable,
        required = required,
        default = defaultValue,
        defaultSource = defaultSource,
        binding = Binding(field = mapping.binding),
        layerHint = null,
        variantAxis = mapping.variantAxis?.let { config ->
            VariantAxis(
                propertyName = if (config.derivePropertyNameFromParam == true) name else null,
                valueMap = config.defaultValueMap
            )
        },
        instanceSwap = mapping.instanceSwap?.let { config ->
            InstanceSwap(
                targetLayerMarker = if (config.deriveTargetLayerMarkerFromParam == true) "#swap:$name" else null,
                contract = config.contract
            )
        },
        callbackArity = mapping.callbackArity
    )
}

private fun normalizeLambdaType(type: String): String {
    // Handle lambda types like (Boolean) -> Unit, () -> Unit
    val lambdaPattern = Regex("""\(\s*(.*?)\s*\)\s*->\s*(\w+)""")
    val match = lambdaPattern.find(type) ?: return type

    val params = match.groupValues[1].trim()
    val returnType = match.groupValues[2].trim()

    return if (params.isEmpty()) {
        "() -> $returnType"
    } else {
        "($params) -> $returnType"
    }
}

private fun parseLiteralDefault(
    valueStr: String,
    expectedType: String?
): Pair<JsonElement?, String> {
    return try {
        val element: JsonElement = when (expectedType) {
            "string" -> {
                // Remove quotes if present
                val content = valueStr.removeSurrounding("\"")
                JsonPrimitive(content)
            }

            "boolean" -> {
                JsonPrimitive(valueStr.toBooleanStrictOrNull() ?: return null to "unknown")
            }

            "int" -> {
                JsonPrimitive(valueStr.toIntOrNull() ?: return null to "unknown")
            }

            "float" -> {
                val floatVal = valueStr.removeSuffix("f").removeSuffix("F").toFloatOrNull()
                    ?: return null to "unknown"
                JsonPrimitive(floatVal)
            }

            else -> return null to "unknown"
        }
        element to "literal"
    } catch (_: Exception) {
        null to "unknown"
    }
}

// ============================================================================
// Schema Merging
// ============================================================================

internal fun mergeComponent(existing: SchemaFile, newComponent: Component): SchemaFile {
    val existingComponents = existing.components.toMutableList()
    val existingIndex =
        existingComponents.indexOfFirst { it.componentName == newComponent.componentName }

    if (existingIndex >= 0) {
        // Replace existing component
        existingComponents[existingIndex] = newComponent
    } else {
        // Append new component
        existingComponents.add(newComponent)
    }

    return existing.copy(components = existingComponents)
}

// ============================================================================
// Canonicalization (shared by both modes)
// ============================================================================

internal fun canonicalizeSchema(rawSchema: SchemaFile, overrides: OverridesFile): SchemaFile {
    val mergedComponents = rawSchema.components.map { component ->
        val componentOverrides =
            overrides.overrides.filter { it.component == component.componentName }
        val updatedParams = component.params.mapNotNull { param ->
            val paramOverrides = componentOverrides.filter { it.param == param.name }
            applyOverrides(param, paramOverrides)
        }

        component.copy(
            params = enforceDeterministicOrder(updatedParams)
        )
    }

    val orderedComponents = enforceDeterministicOrder(mergedComponents)

    val finalSchema = rawSchema.copy(
        generatedAt = Instant.now().toString(),
        git = GitMetadata(commit = resolveGitCommit(rawSchema.git.commit)),
        components = orderedComponents
    )

    validateBooleanVariantAxes(finalSchema)
    validatePropOnlyDefaults(finalSchema)
    validateRequiredCallbacks(finalSchema)

    return finalSchema
}

internal fun applyOverrides(param: Param, overrides: List<Override>): Param? {
    if (overrides.isEmpty()) return param

    var current = param
    overrides.forEach { o ->
        if (o.exclude == true) return null
        current = current.copy(
            kind = o.forceKind ?: current.kind,
            binding = o.forceBinding ?: current.binding,
            layerHint = o.layerHint ?: current.layerHint,
            instanceSwap = o.instanceSwap ?: current.instanceSwap,
            variantAxis = o.variantAxis ?: current.variantAxis,
            enum = o.enum ?: current.enum
        )
    }
    return current
}

internal fun validateBooleanVariantAxes(schema: SchemaFile) {
    schema.components.forEach { component ->
        component.params.forEach { param ->
            if (param.kind == "BOOLEAN" && param.binding.field == "VARIANT_AXIS") {
                val valueMap = param.variantAxis?.valueMap
                require(!valueMap.isNullOrEmpty()) {
                    "BOOLEAN param '${param.name}' has VARIANT_AXIS binding but missing variantAxis.valueMap"
                }
            }
        }
    }
}

internal fun validatePropOnlyDefaults(schema: SchemaFile) {
    schema.components.forEach { component ->
        component.params.forEach { param ->
            if (param.binding.field == "PROP_ONLY" && param.required && param.default == null) {
                error("PROP_ONLY param '${param.name}' is required but missing default")
            }
        }
    }
}

internal fun validateRequiredCallbacks(schema: SchemaFile) {
    schema.components.forEach { component ->
        component.params.forEach { param ->
            if (param.kind == "EXCLUDED" &&
                param.binding.field == "NONE" &&
                param.required
            ) {
                val arity = param.callbackArity
                require(arity != null && arity >= 0) {
                    "Required callback param '${param.name}' must define non-null callbackArity"
                }
            }
        }
    }
}

private fun resolveGitCommit(fallback: String): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "HEAD")
            .directory(null as File?)
            .start()
        val exit = process.waitFor()
        if (exit == 0) process.inputStream.bufferedReader().readText().trim()
            .ifBlank { fallback } else fallback
    } catch (_: Exception) {
        fallback
    }
}

private fun ensureParent(path: Path) {
    Files.createDirectories(path.parent)
}

internal fun <T> enforceDeterministicOrder(items: List<T>): List<T> = when (items.firstOrNull()) {
    is Component -> items.map { it as Component }.sortedBy { it.componentName } as List<T>
    is Param -> items.map { it as Param }.sortedBy { it.name } as List<T>
    else -> items
}
