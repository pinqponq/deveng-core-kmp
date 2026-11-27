package figmasync.manifest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
}

@Serializable
data class SchemaFile(
    val schemaVersion: Int = 1,
    val components: List<Component>
)

@Serializable
data class Component(
    val componentName: String,
    val kotlinFqName: String
)

@Serializable
data class ManifestFile(
    val version: Int = 1,
    val components: List<ManifestComponent>
)

@Serializable
data class ManifestComponent(
    val componentName: String,
    val kotlinFqName: String,
    val codeConnect: ManifestCodeConnect? = null,
    val figma: ManifestFigma? = null
)

@Serializable
data class ManifestCodeConnect(
    val docFile: String? = null,
    val docRole: String? = null,
    val templateFile: String? = null,
    val publish: ManifestPublish? = null
)

@Serializable
data class ManifestPublish(val source: String? = null)

@Serializable
data class ManifestFigma(
    val fileKey: String? = null,
    val nodeId: String? = null,
    val componentUrl: String? = null
)

data class CliConfig(
    val schemaPath: Path,
    val manifestPath: Path,
    val interactive: Boolean,
    val merge: Boolean
)

fun main(args: Array<String>) {
    val config = parseArgs(args)
    runGenerateManifest(config)
}

private fun parseArgs(args: Array<String>): CliConfig {
    var schemaPath: Path = Path("figma-sync/schema/component-schema.json")
    var manifestPath: Path = Path("figma-sync/schema/components.manifest.json")
    var interactive = false
    var merge = false

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--schema" -> {
                schemaPath = Path(args.getOrNull(i + 1) ?: error("Missing value for --schema"))
                i += 2
            }

            "--manifest" -> {
                manifestPath = Path(args.getOrNull(i + 1) ?: error("Missing value for --manifest"))
                i += 2
            }

            "--interactive" -> {
                interactive = true
                i += 1
            }

            "--merge" -> {
                merge = true
                i += 1
            }

            "--help", "-h" -> {
                printHelp()
                kotlin.system.exitProcess(0)
            }

            else -> i += 1
        }
    }

    return CliConfig(schemaPath, manifestPath, interactive, merge)
}

private fun runGenerateManifest(config: CliConfig) {
    val schemaPath = config.schemaPath
    val manifestPath = config.manifestPath

    if (!schemaPath.exists()) error("Schema file not found at $schemaPath")

    val schema = json.decodeFromString<SchemaFile>(schemaPath.readText())
    val existingManifest = if (manifestPath.exists()) {
        json.decodeFromString<ManifestFile>(manifestPath.readText())
    } else {
        ManifestFile(version = 1, components = emptyList())
    }

    val existingByName = existingManifest.components.associateBy { it.componentName }

    // Generate manifest entries for schema components
    if (config.interactive) {
        println("Interactive mode enabled")
    }

    val manifestComponents = schema.components.map { component ->
        val existing = existingByName[component.componentName]
        when {
            existing == null -> {
                if (config.interactive) println("  New component: ${component.componentName}")
                createStubManifestEntry(component, config.interactive)
            }

            config.interactive && hasMissingFigmaUrl(existing) -> {
                println("  Missing URL for: ${existing.componentName}")
                System.out.flush() // Ensure output is visible
                System.err.flush() // Also flush stderr
                val updated = promptAndUpdateEntry(existing)
                val urlStillMissing = updated.figma?.componentUrl.isNullOrBlank() ||
                        updated.figma?.componentUrl == "<PASTE_FIGMA_URL_HERE>"
                if (urlStillMissing) {
                    println("    [Skipped - no URL provided]")
                } else {
                    println("    ✓ URL updated")
                }
                updated
            }

            else -> existing
        }
    }

    // Only preserve extra components if in merge mode
    val merged = if (config.merge) {
        // Merge mode: preserve extra components not in schema
        val schemaNames = schema.components.map { it.componentName }.toSet()
        val extraComponents =
            existingManifest.components.filter { it.componentName !in schemaNames }
        enforceDeterministicOrder(manifestComponents + extraComponents)
    } else {
        // Replace mode (default): only schema components
        enforceDeterministicOrder(manifestComponents)
    }

    ensureParent(manifestPath)
    val updatedManifest = ManifestFile(version = 1, components = merged)
    manifestPath.writeText(json.encodeToString(updatedManifest))

    val missingCount =
        merged.count { it.figma?.componentUrl.isNullOrBlank() || it.figma?.componentUrl == "<PASTE_FIGMA_URL_HERE>" }
    println("✓ Manifest updated at $manifestPath")
    println("  Components: ${merged.size} (missing Figma URLs: $missingCount)")
    if (config.merge) {
        println("  Mode: merge (preserved extra components)")
    }
}

private fun hasMissingFigmaUrl(entry: ManifestComponent): Boolean {
    val url = entry.figma?.componentUrl
    return url.isNullOrBlank() || url == "<PASTE_FIGMA_URL_HERE>"
}

private fun promptAndUpdateEntry(existing: ManifestComponent): ManifestComponent {
    val figmaUrl = promptForFigmaUrl(existing.componentName)
    if (figmaUrl == null) return existing // User skipped, keep existing

    val (fileKey, nodeId) = parseFigmaUrl(figmaUrl)
    return existing.copy(
        figma = ManifestFigma(
            fileKey = fileKey ?: existing.figma?.fileKey ?: "<FILE_KEY>",
            nodeId = nodeId ?: existing.figma?.nodeId ?: "<NODE_ID>",
            componentUrl = figmaUrl
        )
    )
}

private fun createStubManifestEntry(component: Component, interactive: Boolean): ManifestComponent {
    val figmaUrl = if (interactive) promptForFigmaUrl(component.componentName) else null
    val (fileKey, nodeId) = parseFigmaUrl(figmaUrl)

    return ManifestComponent(
        componentName = component.componentName,
        kotlinFqName = component.kotlinFqName,
        codeConnect = ManifestCodeConnect(
            templateFile = "figma-sync/templates/${component.componentName}.figma.template.js",
            publish = ManifestPublish(source = "template-v2")
        ),
        figma = ManifestFigma(
            fileKey = fileKey ?: "<FILE_KEY>",
            nodeId = nodeId ?: "<NODE_ID>",
            componentUrl = figmaUrl ?: "<PASTE_FIGMA_URL_HERE>"
        )
    )
}

private fun promptForFigmaUrl(componentName: String): String? {
    val prompt = "Enter Figma component URL for '$componentName' (or press Enter to skip): "

    // Try System.console() first (works better in Gradle contexts)
    val console = System.console()
    if (console != null) {
        try {
            val input = console.readLine(prompt)
            return input?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            System.err.println("  [Warning] Console read failed: ${e.message}")
        }
    }

    // Fallback to stdin if console is not available
    print(prompt)
    System.out.flush()
    return try {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(System.`in`))
        val line = reader.readLine()
        line?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        System.err.println("  [Error] Could not read input: ${e.message}")
        System.err.println("  [Hint] Try running without Gradle daemon: ./gradlew --no-daemon :figma-sync:tools:manifest-cli:generateManifest --interactive")
        System.err.println("  [Hint] Or run directly: ./gradlew :figma-sync:tools:manifest-cli:run --args='--interactive'")
        null
    }
}

// Returns Pair<fileKey, nodeId>
private fun parseFigmaUrl(url: String?): Pair<String?, String?> {
    if (url.isNullOrBlank()) return null to null
    val pattern = Regex("""figma\.com/(?:design|file)/([A-Za-z0-9]+)[^?]*\?[^#]*node-id=([^&]+)""")
    val match = pattern.find(url)
    return if (match != null) {
        match.groupValues[1] to match.groupValues[2]
    } else {
        null to null
    }
}

private fun ensureParent(path: Path) {
    Files.createDirectories(path.parent)
}

private fun <T> enforceDeterministicOrder(items: List<T>): List<T> = when (items.firstOrNull()) {
    is ManifestComponent -> items.map { it as ManifestComponent }
        .sortedBy { it.componentName } as List<T>

    else -> items
}

private fun printHelp() {
    println(
        """
        |generateManifest - Generate manifest from schema (replaces by default).
        |
        |Usage:
        |  ./gradlew :figma-sync:tools:manifest-cli:generateManifest [options]
        |
        |Options:
        |  --schema <path>     Path to component-schema.json (default: figma-sync/schema/component-schema.json)
        |  --manifest <path>   Path to components.manifest.json (default: figma-sync/schema/components.manifest.json)
        |  --interactive       Prompt for Figma URLs for components with missing/placeholder URLs
        |  --merge             Preserve manifest entries not in schema (default: replace)
        |
        |Examples:
        |  ./gradlew :figma-sync:tools:manifest-cli:generateManifest
        |  ./gradlew :figma-sync:tools:manifest-cli:generateManifest --interactive
        |  ./gradlew :figma-sync:tools:manifest-cli:generateManifest --merge
        |  ./gradlew :figma-sync:tools:manifest-cli:generateManifest --merge --interactive
        """.trimMargin()
    )
}
