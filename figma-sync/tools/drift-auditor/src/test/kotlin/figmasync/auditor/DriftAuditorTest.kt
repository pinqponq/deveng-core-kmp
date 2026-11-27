package figmasync.auditor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DriftAuditorTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `detects missing extra type mismatch and ghost`() {
        val schema = json.decodeFromString<SchemaFile>(
            this::class.java.getResource("/schema.json")!!.readText()
        )
        val manifest = json.decodeFromString<Manifest>(
            this::class.java.getResource("/manifest.json")!!.readText()
        )
        val figmaData = parseFixture()

        val entry = driftForComponent(
            component = schema.components.first(),
            manifest = manifest.components.first(),
            fetcher = { _, _, _ -> FetchResult.Ok(figmaData) },
            tokenProvider = { "test-token" }
        )

        val errors = entry.errors.sorted()
        val warnings = entry.warnings.sorted()

        assertEquals(
            listOf(
                "[EXTRA] Figma property `extraProp` not in schema",
                "[MISSING] Property `isChecked` not found in Figma",
                "[MISSING] Property `isLabelAtStart` not found in Figma",
                "[TYPE_MISMATCH] `label` expected `TEXT` but Figma has `BOOLEAN`"
            ).sorted(),
            errors
        )

        assertEquals(
            listOf(
                "[SKIPPED] Variant value-domain validation skipped for `isChecked`: option list not available in REST payload",
                "[SKIPPED] Variant value-domain validation skipped for `isLabelAtStart`: option list not available in REST payload"
            ).sorted(),
            warnings
        )

        // label is referenced via value "label#0:0" so no ghost should be reported
        assertEquals(false, errors.any { it.contains("GHOST") })

        // Status should be ISSUES because errors exist
        assertEquals("ISSUES", entry.status)

        // Verify issues field is errors + warnings (backward compatibility)
        assertEquals(entry.errors + entry.warnings, entry.issues)
    }

    @Test
    fun `boolean variant axis expects VARIANT type`() {
        val schema = json.decodeFromString<SchemaFile>(
            this::class.java.getResource("/schema_variant.json")!!.readText()
        )
        val manifest = json.decodeFromString<Manifest>(
            this::class.java.getResource("/manifest.json")!!.readText()
        )
        val figmaData = parseVariantFixture()

        val entry = driftForComponent(
            component = schema.components.first(),
            manifest = manifest.components.first().copy(componentName = "VariantSwitch"),
            fetcher = { _, _, _ -> FetchResult.Ok(figmaData) },
            tokenProvider = { "test-token" }
        )

        assertEquals(true, entry.errors.none { it.contains("TYPE_MISMATCH") })
    }

    private fun parseFixture(): FigmaNodeData {
        val fixture = this::class.java.getResource("/figma-node.json")!!.readText()
        val root = json.parseToJsonElement(fixture).jsonObject
        val defs = root["componentPropertyDefinitions"]!!.jsonObject
            .map { (rawKey, value) ->
                val obj = value.jsonObject
                val type = obj["type"]!!.jsonPrimitive.content
                val options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }
                rawKey.substringBefore("#") to PropertyDefinition(type = type, options = options)
            }
            .toMap()
        val refs = root["componentPropertyReferences"]!!
            .jsonObject
            .values
            .map { it.jsonPrimitive.content }
            .map { it.substringBefore("#") }
            .toSet()
        return FigmaNodeData(defs, refs)
    }

    private fun parseVariantFixture(): FigmaNodeData {
        val fixture = this::class.java.getResource("/figma-node-variant.json")!!.readText()
        val root = json.parseToJsonElement(fixture).jsonObject
        val defs = root["componentPropertyDefinitions"]!!.jsonObject
            .map { (rawKey, value) ->
                val obj = value.jsonObject
                val type = obj["type"]!!.jsonPrimitive.content
                val options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }
                rawKey.substringBefore("#") to PropertyDefinition(type = type, options = options)
            }
            .toMap()
        val refs = root["componentPropertyReferences"]!!
            .jsonObject
            .values
            .map { it.jsonPrimitive.content }
            .map { it.substringBefore("#") }
            .toSet()
        return FigmaNodeData(defs, refs)
    }

    @Test
    fun `node id is normalized and encoded for requests`() {
        assertEquals("148%3A87", encodeNodeId("148-87"))
        assertEquals("148%3A87", encodeNodeId("148:87"))
    }

    @Test
    fun `http error surfaces skip reason`() {
        val schema = json.decodeFromString<SchemaFile>(
            this::class.java.getResource("/schema.json")!!.readText()
        )
        val manifest = json.decodeFromString<Manifest>(
            this::class.java.getResource("/manifest.json")!!.readText()
        )

        val entry = driftForComponent(
            component = schema.components.first(),
            manifest = manifest.components.first(),
            fetcher = { _, _, _ -> FetchResult.HttpError(403, "Forbidden") },
            tokenProvider = { "test-token" }
        )

        assertEquals("OK", entry.status)
        assertEquals(0, entry.errors.size)
        val warning = entry.warnings.single()
        assertTrue(warning.contains("Unable to fetch Figma node"))
        assertTrue(warning.contains("http=403"))
        assertTrue(warning.contains("Forbidden"))
    }

    @Test
    fun `warnings only results in OK status`() {
        val schema = json.decodeFromString<SchemaFile>(
            this::class.java.getResource("/schema.json")!!.readText()
        )
        val manifest = json.decodeFromString<Manifest>(
            this::class.java.getResource("/manifest.json")!!.readText()
        )

        // Test with missing token - should produce warning
        val entryNoToken = driftForComponent(
            component = schema.components.first(),
            manifest = manifest.components.first(),
            fetcher = { _, _, _ -> error("should not be called") },
            tokenProvider = { null }
        )

        assertEquals("OK", entryNoToken.status)
        assertEquals(0, entryNoToken.errors.size)
        assertEquals(1, entryNoToken.warnings.size)
        assertTrue(entryNoToken.warnings.first().contains("[SKIPPED]"))
    }

    @Test
    fun `builds figma request with token header`() {
        val request = buildFigmaNodeRequest(
            token = "fake-token",
            fileKey = "fileKey123",
            nodeId = "148-87"
        )

        assertEquals("fake-token", request.headers().firstValue("X-Figma-Token").orElse(null))
        assertTrue(request.headers().firstValue("Authorization").isEmpty)
        assertTrue(request.uri().toString().contains("ids=148%3A87"))
    }

    @Test
    fun `issues field equals errors plus warnings`() {
        val schema = json.decodeFromString<SchemaFile>(
            this::class.java.getResource("/schema.json")!!.readText()
        )
        val manifest = json.decodeFromString<Manifest>(
            this::class.java.getResource("/manifest.json")!!.readText()
        )
        val figmaData = parseFixture()

        val entry = driftForComponent(
            component = schema.components.first(),
            manifest = manifest.components.first(),
            fetcher = { _, _, _ -> FetchResult.Ok(figmaData) },
            tokenProvider = { "test-token" }
        )

        assertEquals(entry.errors + entry.warnings, entry.issues)
        assertEquals(entry.errors.size + entry.warnings.size, entry.issues.size)
    }

    @Test
    fun `computeExitCode returns 0 when no errors`() {
        val entries = listOf(
            DriftEntry(
                "Component1",
                "OK",
                emptyList(),
                listOf("[SKIPPED] warning"),
                listOf("[SKIPPED] warning")
            ),
            DriftEntry("Component2", "OK", emptyList(), emptyList(), emptyList())
        )
        assertEquals(0, computeExitCode(entries))
    }

    @Test
    fun `computeExitCode returns 1 when errors exist`() {
        val entries = listOf(
            DriftEntry(
                "Component1",
                "ISSUES",
                listOf("[MISSING] error"),
                emptyList(),
                listOf("[MISSING] error")
            ),
            DriftEntry(
                "Component2",
                "OK",
                emptyList(),
                listOf("[SKIPPED] warning"),
                listOf("[SKIPPED] warning")
            )
        )
        assertEquals(1, computeExitCode(entries))
    }

    @Test
    fun `computeExitCode ignores warnings`() {
        val entries = listOf(
            DriftEntry(
                "Component1",
                "OK",
                emptyList(),
                listOf("[SKIPPED] warning1", "[SKIPPED] warning2"),
                listOf("[SKIPPED] warning1", "[SKIPPED] warning2")
            )
        )
        assertEquals(0, computeExitCode(entries))
    }

    @Test
    fun `report generatedAt is audit timestamp not schema timestamp`() {
        val schema = json.decodeFromString<SchemaFile>(
            this::class.java.getResource("/schema.json")!!.readText()
        )
        val schemaTimestamp = schema.generatedAt

        // Simulate what main() does: use Instant.now() instead of schema.generatedAt
        val auditTimestamp = Instant.now().toString()
        val report = DriftReport(
            generatedAt = auditTimestamp,
            components = emptyList()
        )

        // Verify it's different from schema timestamp
        assertNotEquals(schemaTimestamp, report.generatedAt)

        // Verify it's a valid ISO-8601 timestamp (can be parsed)
        val parsed = Instant.parse(report.generatedAt)
        assertTrue(
            parsed.isAfter(Instant.parse("2025-01-01T00:00:00Z")) || parsed.equals(
                Instant.parse(
                    "2025-01-01T00:00:00Z"
                )
            )
        )
    }

    @Test
    fun `issues field is serialized in JSON`() {
        val entry = DriftEntry(
            componentName = "TestComponent",
            status = "ISSUES",
            errors = listOf("[MISSING] error1"),
            warnings = listOf("[SKIPPED] warning1"),
            issues = listOf("[MISSING] error1", "[SKIPPED] warning1")
        )

        val jsonString = json.encodeToString(entry)
        assertTrue(jsonString.contains("\"issues\""))
        assertTrue(jsonString.contains("error1"))
        assertTrue(jsonString.contains("warning1"))

        // Verify round-trip serialization
        val decoded = json.decodeFromString<DriftEntry>(jsonString)
        assertEquals(entry.issues, decoded.issues)
        assertEquals(entry.errors + entry.warnings, decoded.issues)
    }
}
