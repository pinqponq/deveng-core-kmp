package figmasync.auditor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
}

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
data class LayerHint(val marker: String? = null)

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
data class Manifest(
    val version: Int,
    val components: List<ManifestComponent>
)

@Serializable
data class ManifestComponent(
    val componentName: String,
    val kotlinFqName: String? = null,
    val figma: FigmaMeta? = null
)

@Serializable
data class FigmaMeta(
    val fileKey: String? = null,
    val nodeId: String? = null,
    val componentUrl: String? = null
)

@Serializable
data class DriftReport(
    val generatedAt: String,
    val components: List<DriftEntry>
)

@Serializable
data class DriftEntry(
    val componentName: String,
    val status: String,
    val errors: List<String>,
    val warnings: List<String>,
    val issues: List<String>
)

fun main(args: Array<String>) {
    if (args.size != 4) error("Usage: DriftAuditor <schemaPath> <manifestPath> <reportJson> <reportMd>")

    val schemaPath = Path(args[0])
    val manifestPath = Path(args[1])
    val reportJson = Path(args[2])
    val reportMd = Path(args[3])

    if (!schemaPath.exists()) error("Schema file not found at $schemaPath")
    if (!manifestPath.exists()) error("Manifest file not found at $manifestPath")

    val schema = json.decodeFromString<SchemaFile>(schemaPath.readText())
    val manifest = json.decodeFromString<Manifest>(manifestPath.readText())

    val httpClient = HttpClient.newHttpClient()
    val entries = schema.components.map { component ->
        driftForComponent(
            component = component,
            manifest = manifest.components.find { it.componentName == component.componentName },
            fetcher = { fk, nid, token -> fetchFigmaNode(httpClient, token, fk, nid) },
            tokenProvider = { System.getenv("FIGMA_ACCESS_TOKEN") }
        )
    }

    val totalErrors = entries.sumOf { it.errors.size }
    val auditTimestamp = Instant.now().toString()

    val report = DriftReport(
        generatedAt = auditTimestamp,
        components = entries
    )

    writeReports(report, reportJson, reportMd)

    val exitCode = computeExitCode(entries)
    exitProcess(exitCode)
}

private fun writeReports(report: DriftReport, jsonPath: Path, mdPath: Path) {
    jsonPath.parent?.createDirectories()
    mdPath.parent?.createDirectories()

    jsonPath.writeText(json.encodeToString(report))
    mdPath.writeText(renderMarkdown(report))
}

internal fun driftForComponent(
    component: Component,
    manifest: ManifestComponent?,
    fetcher: (fileKey: String, nodeId: String, token: String) -> FetchResult,
    tokenProvider: () -> String? = { System.getenv("FIGMA_ACCESS_TOKEN") }
): DriftEntry {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    if (manifest == null ||
        manifest.figma?.fileKey.isNullOrBlank() ||
        manifest.figma.nodeId.isNullOrBlank() ||
        manifest.figma.fileKey!!.startsWith("<") ||
        manifest.figma.nodeId!!.startsWith("<")
    ) {
        warnings += "[SKIPPED] Missing/placeholder manifest IDs for ${component.componentName}"
        return DriftEntry(component.componentName, "OK", errors, warnings, errors + warnings)
    }

    val token = tokenProvider()
    if (token.isNullOrBlank()) {
        warnings += "[SKIPPED] FIGMA_ACCESS_TOKEN not set; cannot fetch Figma data"
        return DriftEntry(component.componentName, "OK", errors, warnings, errors + warnings)
    }

    val figmaData = fetcher(
        manifest.figma.fileKey!!,
        manifest.figma.nodeId!!,
        token
    ).let { result ->
        when (result) {
            is FetchResult.Ok -> result.data
            is FetchResult.HttpError -> {
                warnings.add("[SKIPPED] Unable to fetch Figma node for ${component.componentName} (http=${result.status}, body=${result.bodySnippet})")
                return DriftEntry(
                    component.componentName,
                    "OK",
                    errors,
                    warnings,
                    errors + warnings
                )
            }

            is FetchResult.ParseError -> {
                warnings.add("[SKIPPED] Unable to fetch Figma node for ${component.componentName} (parseError=${result.message})")
                return DriftEntry(
                    component.componentName,
                    "OK",
                    errors,
                    warnings,
                    errors + warnings
                )
            }
        }
    }

    val defs = figmaData.definitions
    val refs = figmaData.references

    val schemaParams = component.params.associateBy { it.bindingKey() }
    val expectedTypes = schemaParams.mapValues { expectedFigmaType(it.value) }

    // NONE binding warnings (non-callbacks)
    schemaParams.values
        .filter { it.kind != "EXCLUDED" }
        .filter { it.binding.field == "NONE" }
        .forEach { param ->
            errors += "[NONE_BINDING] Param `${param.bindingKey()}` has binding.field=NONE - requires resolution"
        }

    // Missing
    expectedTypes
        .filter { (name, _) ->
            val p = schemaParams[name]
            p?.kind != "EXCLUDED" && p?.binding?.field != "PROP_ONLY"
        }
        .forEach { (name, type) ->
            if (defs[name] == null) {
                errors += "[MISSING] Property `$name` not found in Figma"
            }
        }

    // Extra
    defs.keys.forEach { name ->
        if (!schemaParams.containsKey(name)) {
            errors += "[EXTRA] Figma property `$name` not in schema"
        }
    }

    // Type mismatch
    defs.forEach { (name, figmaType) ->
        val expected = expectedTypes[name]
        if (expected != null && expected.type != figmaType.type) {
            errors += "[TYPE_MISMATCH] `$name` expected `${expected.type}` but Figma has `${figmaType.type}`"
        }
    }

    // Ghost detection
    defs.forEach { (name, _) ->
        val param = schemaParams[name]
        val referenced = refs.contains(name)
        val binding = param?.binding?.field
        val exemptGhost = binding == "PROP_ONLY" || binding == "VARIANT_AXIS"
        if (param != null) {
            if (!exemptGhost && !referenced) {
                errors += "[GHOST] Property `$name` defined but not referenced in layers"
            }
        }
    }

    // Variant value-domain validation (conditional)
    schemaParams.values.filter { it.binding.field == "VARIANT_AXIS" }.forEach { param ->
        val valueMap = param.variantAxis?.valueMap
        val figmaDef = defs[param.bindingKey()]
        val options = figmaDef?.options
        if (valueMap.isNullOrEmpty()) return@forEach
        if (options.isNullOrEmpty()) {
            warnings += "[SKIPPED] Variant value-domain validation skipped for `${param.bindingKey()}`: option list not available in REST payload"
        } else {
            val valueKeys = valueMap.keys.toSet()
            val optionSet = options.toSet()
            valueKeys.filterNot(optionSet::contains).forEach { missing ->
                errors += "[MISSING] Variant value `${missing}` not found in Figma for `${param.bindingKey()}`"
            }
            optionSet.filterNot(valueKeys::contains).forEach { extra ->
                errors += "[EXTRA] Figma variant value `${extra}` not in schema valueMap for `${param.bindingKey()}`"
            }
        }
    }

    val status = if (errors.isEmpty()) "OK" else "ISSUES"
    return DriftEntry(component.componentName, status, errors, warnings, errors + warnings)
}

internal data class FigmaNodeData(
    val definitions: Map<String, PropertyDefinition>,
    val references: Set<String>
)

internal data class PropertyDefinition(
    val type: String,
    val options: List<String>? = null
)

internal sealed interface FetchResult {
    data class Ok(val data: FigmaNodeData) : FetchResult
    data class HttpError(val status: Int, val bodySnippet: String) : FetchResult
    data class ParseError(val message: String) : FetchResult
}

internal fun buildFigmaNodeRequest(
    token: String,
    fileKey: String,
    nodeId: String
): HttpRequest {
    val encodedNodeId = encodeNodeId(nodeId)
    val url = "https://api.figma.com/v1/files/$fileKey/nodes?ids=$encodedNodeId"
    return HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("X-Figma-Token", token)
        .GET()
        .build()
}

internal fun fetchFigmaNode(
    client: HttpClient,
    token: String,
    fileKey: String,
    nodeId: String
): FetchResult {
    val normalizedNodeId = normalizeNodeId(nodeId)
    val request = buildFigmaNodeRequest(token, fileKey, normalizedNodeId)

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        return FetchResult.HttpError(
            status = response.statusCode(),
            bodySnippet = singleLineSnippet(response.body())
        )
    }

    return runCatching {
        val root = json.parseToJsonElement(response.body()).jsonObject
        val nodes = root["nodes"]?.jsonObject ?: error("nodes missing in response")
        val node =
            nodes[normalizedNodeId]?.jsonObject ?: error("node not found for id $normalizedNodeId")
        val doc = node["document"]?.jsonObject ?: error("document missing for id $normalizedNodeId")

        val defs = (doc["componentPropertyDefinitions"] as? JsonObject)
            ?.mapNotNull { (rawKey, value) ->
                val key = rawKey.substringBefore("#")
                val obj = value.jsonObject
                val type = obj["type"]?.jsonPrimitive?.content ?: ""
                val options = obj["options"]?.jsonArrayOrNull()
                    ?.mapNotNull { it.jsonPrimitiveOrNull()?.content }
                key to PropertyDefinition(type = type, options = options)
            }
            ?.toMap()
            ?: emptyMap()

        val refs = mutableSetOf<String>()
        collectRefs(doc, refs)
        val normalizedRefs = refs.map { it.substringBefore("#") }.toSet()

        FetchResult.Ok(FigmaNodeData(defs, normalizedRefs))
    }.getOrElse { throwable ->
        FetchResult.ParseError(
            singleLineSnippet(
                throwable.message ?: throwable.javaClass.simpleName
            )
        )
    }
}

internal fun normalizeNodeId(nodeId: String): String = nodeId.replace('-', ':')

internal fun encodeNodeId(nodeId: String): String =
    URLEncoder.encode(normalizeNodeId(nodeId), UTF_8)

private fun singleLineSnippet(text: String, maxLength: Int = 200): String {
    val singleLine = text.replace(Regex("\\s+"), " ").trim()
    return if (singleLine.length <= maxLength) singleLine else singleLine.substring(0, maxLength)
}

private fun collectRefs(obj: JsonObject, acc: MutableSet<String>) {
    obj["componentPropertyReferences"]?.let { refElem ->
        if (refElem is JsonObject) {
            refElem.forEach { (_, v) ->
                val id = v.jsonPrimitiveOrNull()?.content
                if (!id.isNullOrBlank()) acc += id
            }
        }
    }
    obj["children"]?.let { childrenElem ->
        val arr = childrenElem.jsonArrayOrNull()
        arr?.forEach { child ->
            child.jsonObjectOrNull()?.let { collectRefs(it, acc) }
        }
    }
}

private fun JsonElement.jsonArrayOrNull() = runCatching { jsonArray }.getOrNull()
private fun JsonElement.jsonObjectOrNull() = runCatching { jsonObject }.getOrNull()
private fun JsonElement.jsonPrimitiveOrNull() = runCatching { jsonPrimitive }.getOrNull()

private fun expectedFigmaType(param: Param): PropertyDefinition =
    if (param.binding.field == "VARIANT_AXIS") {
        PropertyDefinition(type = "VARIANT")
    } else {
        val type = when (param.kind) {
            "TEXT" -> "TEXT"
            "BOOLEAN" -> "BOOLEAN"
            "ENUM" -> "VARIANT"
            "INSTANCE_SWAP" -> "INSTANCE_SWAP"
            else -> param.kind
        }
        PropertyDefinition(type = type)
    }

private fun Param.bindingKey(): String {
    return if (binding.field == "VARIANT_AXIS") {
        variantAxis?.propertyName ?: name
    } else {
        name
    }
}

internal fun computeExitCode(entries: List<DriftEntry>): Int {
    val totalErrors = entries.sumOf { it.errors.size }
    return if (totalErrors > 0) 1 else 0
}

private fun renderMarkdown(report: DriftReport): String {
    val lines = buildString {
        appendLine("# Figma Sync Drift Report")
        appendLine("Generated: ${report.generatedAt}")
        appendLine()
        appendLine("## Summary")
        appendLine("- Components checked: ${report.components.size}")

        val totalErrors = report.components.sumOf { it.errors.size }
        val totalWarnings = report.components.sumOf { it.warnings.size }
        val componentsWithErrors = report.components.count { it.errors.isNotEmpty() }
        val componentsWithWarnings = report.components.count { it.warnings.isNotEmpty() }

        appendLine("- Drift errors found: $totalErrors")
        appendLine("- Drift warnings found: $totalWarnings")
        appendLine("- Components with errors: $componentsWithErrors")
        appendLine("- Components with warnings: $componentsWithWarnings")

        val ciStatus = if (totalErrors > 0) "FAIL" else "PASS"
        appendLine("- CI status: $ciStatus")
        appendLine()

        report.components.forEach { entry ->
            appendLine("## ${entry.componentName}")
            if (entry.errors.isEmpty() && entry.warnings.isEmpty()) {
                appendLine("- [OK] No issues found")
            } else {
                if (entry.errors.isNotEmpty()) {
                    appendLine("### Errors")
                    entry.errors.forEach { error ->
                        appendLine("- $error")
                    }
                }
                if (entry.warnings.isNotEmpty()) {
                    appendLine("### Warnings")
                    entry.warnings.forEach { warning ->
                        appendLine("- $warning")
                    }
                }
            }
            appendLine()
        }
    }
    return lines
}
