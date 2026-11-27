package figmasync.generator

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val json = Json {
    ignoreUnknownKeys = true
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

// Manifest types for reading Figma URLs
@Serializable
data class ManifestFile(
    val version: Int? = null,
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
    val templateFile: String? = null
)

@Serializable
data class ManifestFigma(
    val fileKey: String? = null,
    val nodeId: String? = null,
    val componentUrl: String? = null
)

fun main(args: Array<String>) {
    if (args.size != 3) {
        error("Usage: TemplateGenerator <schemaPath> <manifestPath> <templatesDir>")
    }

    val schemaPath = Path(args[0])
    val manifestPath = Path(args[1])
    val templatesDir = Path(args[2])

    if (!schemaPath.exists()) error("Schema file not found at $schemaPath")
    if (!manifestPath.exists()) error("Manifest file not found at $manifestPath")
    templatesDir.createDirectories()

    val schema = json.decodeFromString<SchemaFile>(schemaPath.readText())

    val figmaUrls: Map<String, String> = run {
        val manifest = json.decodeFromString<ManifestFile>(manifestPath.readText())
        manifest.components.associate { component ->
            component.componentName to (component.figma?.componentUrl ?: "")
        }
    }

    schema.components.forEach { component ->
        val figmaUrl = figmaUrls[component.componentName]
        val content = renderTemplate(component, figmaUrl)
        val outPath = templatesDir.resolve("${component.componentName}.figma.template.js")
        outPath.writeText(content)
    }
}

/**
 * Renders a dynamic Code Connect Template V2 file.
 *
 * Template V2 format:
 * - Line 1: `// url=<figma-node-url>`
 * - export default function template(figma) { ... }
 * - export const metadata = { nestable: boolean }
 *
 * This generates dynamic templates that read Figma properties at runtime
 * using figma.selectedInstance.getBoolean(), getString(), getInstanceSwap(), etc.
 *
 * @param component The component schema
 * @param figmaUrl Optional Figma component URL from manifest
 */
internal fun renderTemplate(component: Component, figmaUrl: String? = null): String {
    val url = figmaUrl?.takeIf { it.isNotBlank() }
        ?: error(
            "Component '${component.componentName}' is missing figma.componentUrl in manifest. " +
                    "Ensure manifest is provided and contains valid Figma URLs for all components."
        )

    // Separate params by their binding type for code generation
    val dynamicParams =
        component.params.filter { it.binding.field != "NONE" && it.kind != "EXCLUDED" }
    val callbackParams =
        component.params.filter { it.kind == "EXCLUDED" && it.binding.field == "NONE" && it.required }

    return buildString {
        // Line 1: Figma URL (required by Code Connect)
        appendLine("// url=$url")
        appendLine()

        // Export default function
        appendLine("export default function template(figma) {")
        appendLine("  const i = figma.selectedInstance;")
        appendLine()

        // Generate variable declarations for dynamic params
        val paramVars = mutableListOf<ParamVar>()
        dynamicParams.forEach { param ->
            val varDecl = generateParamVariable(param)
            if (varDecl != null) {
                appendLine(varDecl.declaration)
                paramVars.add(varDecl)
            }
        }

        if (paramVars.isNotEmpty()) {
            appendLine()
        }

        // Generate return statement with template literal
        appendLine("  return `")
        appendLine("${component.componentName}(")

        val allParams = buildList {
            addAll(paramVars.map { pv ->
                if (pv.needsQuotes) {
                    // Wrap in quotes for String types: label = "${labelValue}"
                    "    ${pv.paramName} = \"\${${pv.varName}}\""
                } else {
                    "    ${pv.paramName} = \${${pv.varName}}"
                }
            })
            addAll(callbackParams.map { param ->
                val stubBody = generateCallbackStub(param)
                "    ${param.name} = $stubBody"
            })
            // Add PROP_ONLY params with their literal values
            component.params.filter { it.binding.field == "PROP_ONLY" }.forEach { param ->
                val literal =
                    param.default?.toKotlinLiteral() ?: if (param.nullable) "null" else "\"\""
                add("    ${param.name} = $literal")
            }
        }

        if (allParams.isNotEmpty()) {
            appendLine(allParams.joinToString(",\n"))
        }

        appendLine(")")
        appendLine("  `.trim();")
        appendLine("}")
        appendLine()

        // Export metadata
        appendLine("export const metadata = { nestable: ${component.codeConnect.nestable} };")
    }
}

/**
 * Represents a generated variable declaration for a dynamic param.
 *
 * @param paramName The Kotlin parameter name
 * @param varName The JavaScript variable name
 * @param declaration The JavaScript variable declaration code
 * @param needsQuotes Whether the value should be wrapped in quotes in Kotlin (for String types)
 */
private data class ParamVar(
    val paramName: String,
    val varName: String,
    val declaration: String,
    val needsQuotes: Boolean = false
)

/**
 * Generates a JavaScript variable declaration for reading a Figma property dynamically.
 *
 * @param param The parameter schema
 * @return ParamVar with the variable name and declaration code, or null if param should be skipped
 */
private fun generateParamVariable(param: Param): ParamVar? {
    val varName = "${param.name}Value"

    return when (param.binding.field) {
        "VARIANT_AXIS" -> {
            val propertyName = param.variantAxis?.propertyName ?: param.name
            val valueMap = param.variantAxis?.valueMap

            when (param.kind) {
                "BOOLEAN" -> {
                    // For boolean variant axis, use getBoolean with value mapping
                    val mappingObj = if (valueMap != null) {
                        valueMap.entries.joinToString(", ", "{ ", " }") { (k, v) ->
                            "\"$k\": $v"
                        }
                    } else {
                        "{ \"true\": true, \"false\": false }"
                    }
                    val declaration =
                        "  const $varName = i.getBoolean(\"$propertyName\", $mappingObj);"
                    ParamVar(param.name, varName, declaration)
                }

                "ENUM" -> {
                    // For enum variant axis, use getEnum with template mapping
                    val templateMap = param.enum?.templateMap
                    val mappingObj = if (templateMap != null) {
                        templateMap.entries.joinToString(", ", "{ ", " }") { (k, v) ->
                            "\"$k\": \"$v\""
                        }
                    } else {
                        // Default: map domain values to themselves
                        val domain = param.enum?.domain ?: emptyList()
                        domain.joinToString(", ", "{ ", " }") { "\"$it\": \"$it\"" }
                    }
                    val declaration =
                        "  const $varName = i.getEnum(\"$propertyName\", $mappingObj);"
                    ParamVar(param.name, varName, declaration)
                }

                else -> null
            }
        }

        "TEXT_CHARACTERS" -> {
            // For text binding, use getString with the property name
            val propertyName = param.layerHint?.marker?.removePrefix("#bind:") ?: param.name
            val declaration = "  const $varName = i.getString(\"$propertyName\") || \"...\";"
            ParamVar(param.name, varName, declaration, needsQuotes = true)
        }

        "INSTANCE_SWAP" -> {
            // For instance swap, generate code to get the swapped instance and extract drawable
            val propertyName =
                param.instanceSwap?.targetLayerMarker?.removePrefix("#swap:") ?: param.name
            val declaration = buildString {
                appendLine("  const ${param.name}Swap = i.getInstanceSwap(\"$propertyName\");")
                appendLine("  let $varName = \"Res.drawable.ic_placeholder\";")
                appendLine("  if (${param.name}Swap && ${param.name}Swap.hasCodeConnect && ${param.name}Swap.hasCodeConnect()) {")
                appendLine("    const result = ${param.name}Swap.executeTemplate();")
                appendLine("    const drawable = result?.metadata?.props?.drawable;")
                appendLine("    if (drawable) { $varName = drawable; }")
                append("  }")
            }
            ParamVar(param.name, varName, declaration)
        }

        "PROP_ONLY" -> {
            // PROP_ONLY params don't read from Figma, handled separately in renderTemplate
            null
        }

        else -> null
    }
}

/**
 * Generates a Kotlin callback stub for a callback parameter.
 */
private fun generateCallbackStub(param: Param): String {
    val arity = param.callbackArity ?: 0
    return when {
        arity <= 0 -> "{ }"
        else -> {
            val args = List(arity) { "_" }.joinToString(", ")
            "{ $args -> }"
        }
    }
}

private fun JsonElement.toKotlinLiteral(): String {
    val primitive = this as? kotlinx.serialization.json.JsonPrimitive ?: return toString()
    if (primitive.isString) return "\"${primitive.content}\""
    return primitive.content
}
