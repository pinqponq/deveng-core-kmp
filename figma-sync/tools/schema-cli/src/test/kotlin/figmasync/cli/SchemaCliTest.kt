package figmasync.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaCliTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ========================================================================
    // Legacy Tests (Canonicalization)
    // ========================================================================

    @Test
    fun `merges overrides and orders deterministically`() {
        val raw = """
            {
              "schemaVersion": 1,
              "generatedAt": "1970-01-01T00:00:00Z",
              "git": { "commit": "unknown" },
              "components": [
                {
                  "componentName": "B",
                  "kotlinFqName": "pkg.B",
                  "codeConnect": { "nestable": false },
                  "params": [
                    { "name": "z", "kind": "BOOLEAN", "nullable": false, "default": null, "defaultSource": "unknown", "binding": { "field": "NONE" } }
                  ]
                },
                {
                  "componentName": "A",
                  "kotlinFqName": "pkg.A",
                  "codeConnect": { "nestable": false },
                  "params": [
                    { "name": "b", "kind": "BOOLEAN", "nullable": false, "default": null, "defaultSource": "unknown", "binding": { "field": "NONE" } },
                    { "name": "a", "kind": "BOOLEAN", "nullable": false, "default": null, "defaultSource": "unknown", "binding": { "field": "NONE" } }
                  ]
                }
              ]
            }
        """.trimIndent()

        val overrides = """
            {
              "version": 1,
              "overrides": [
                {
                  "component": "A",
                  "param": "a",
                  "forceBinding": { "field": "VARIANT_AXIS" },
                  "variantAxis": { "valueMap": { "On": true, "Off": false } }
                }
              ]
            }
        """.trimIndent()

        val merged = merge(raw, overrides)
        assertEquals(listOf("A", "B"), merged.components.map { it.componentName })
        assertEquals(listOf("a", "b"), merged.components.first().params.map { it.name })
        val aParam = merged.components.first().params.first()
        assertEquals("VARIANT_AXIS", aParam.binding.field)
        assertEquals(mapOf("On" to true, "Off" to false), aParam.variantAxis?.valueMap)
    }

    @Test
    fun `boolean variant axis missing valueMap fails`() {
        val raw = """
            {
              "schemaVersion": 1,
              "generatedAt": "1970-01-01T00:00:00Z",
              "git": { "commit": "unknown" },
              "components": [
                {
                  "componentName": "C",
                  "kotlinFqName": "pkg.C",
                  "codeConnect": { "nestable": false },
                  "params": [
                    { "name": "flag", "kind": "BOOLEAN", "nullable": false, "default": null, "defaultSource": "unknown", "binding": { "field": "VARIANT_AXIS" } }
                  ]
                }
              ]
            }
        """.trimIndent()

        val overrides = """{ "version":1, "overrides": [] }"""

        assertFailsWith<IllegalArgumentException> {
            merge(raw, overrides)
        }
    }

    private fun merge(raw: String, overrides: String): SchemaFile {
        val rawFile = json.decodeFromString<SchemaFile>(raw)
        val overridesFile = json.decodeFromString<OverridesFile>(overrides)
        return canonicalizeSchema(rawFile, overridesFile)
    }

    // ========================================================================
    // Kotlin Parameter Extraction Tests
    // ========================================================================

    private val testTypeMapping = TypeMappingFile(
        version = 1,
        description = "Test mapping",
        mappings = mapOf(
            "String" to TypeMapping(
                kind = "TEXT",
                binding = "TEXT_CHARACTERS",
                supportsLiteralDefault = true,
                literalDefaultType = "string"
            ),
            "Boolean" to TypeMapping(
                kind = "BOOLEAN",
                binding = "VARIANT_AXIS",
                supportsLiteralDefault = true,
                literalDefaultType = "boolean",
                variantAxis = VariantAxisConfig(
                    derivePropertyNameFromParam = true,
                    defaultValueMap = mapOf("true" to true, "false" to false)
                )
            ),
            "DrawableResource" to TypeMapping(
                kind = "INSTANCE_SWAP",
                binding = "INSTANCE_SWAP",
                supportsLiteralDefault = false,
                instanceSwap = InstanceSwapConfig(
                    deriveTargetLayerMarkerFromParam = true,
                    contract = "componentNameMatchesKotlinResource"
                )
            ),
            "() -> Unit" to TypeMapping(
                kind = "EXCLUDED",
                binding = "NONE",
                supportsLiteralDefault = false,
                callbackArity = 0,
                forceRequired = true
            ),
            "(Boolean) -> Unit" to TypeMapping(
                kind = "EXCLUDED",
                binding = "NONE",
                supportsLiteralDefault = false,
                callbackArity = 1,
                forceRequired = true
            ),
            "Int" to TypeMapping(
                kind = "TEXT",
                binding = "PROP_ONLY",
                supportsLiteralDefault = true,
                literalDefaultType = "int"
            )
        ),
        excludedTypes = listOf("Modifier", "Color", "TextStyle")
    )

    @Test
    fun `extracts composable function with basic types`() {
        val source = """
            package test.pkg
            
            import androidx.compose.runtime.Composable
            
            @Composable
            fun TestComponent(
                label: String,
                isEnabled: Boolean = true,
                onClick: () -> Unit
            ) {
                // body
            }
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size, "Should find exactly one composable")

        val comp = components.first()
        assertEquals("TestComponent", comp.componentName)
        assertEquals("test.pkg.TestComponent", comp.kotlinFqName)

        // Check params are extracted and filtered
        val paramNames = comp.params.map { it.name }
        assertEquals(3, comp.params.size, "Expected 3 params but got: $paramNames")

        // label param
        val labelParam = comp.params.find { it.name == "label" }
        assertNotNull(labelParam, "Should find 'label' param in $paramNames")
        assertEquals("TEXT", labelParam!!.kind)
        assertEquals("TEXT_CHARACTERS", labelParam.binding.field)
        // label: String has no default and is not nullable, so should be required
        assertEquals(false, labelParam.nullable, "label should not be nullable")
        // Required = !hasDefault && !nullable = !false && !false = true
        assertEquals(
            true,
            labelParam.required,
            "label has no default and is not nullable, should be required"
        )

        // isEnabled param with default
        val enabledParam = comp.params.find { it.name == "isEnabled" }
        assertNotNull(enabledParam, "Should find 'isEnabled' param")
        assertEquals("BOOLEAN", enabledParam!!.kind)
        assertEquals("VARIANT_AXIS", enabledParam.binding.field)
        assertEquals(false, enabledParam.required, "isEnabled has default, should not be required")
        assertEquals(JsonPrimitive(true), enabledParam.default)
        assertEquals("literal", enabledParam.defaultSource)
        assertEquals("isEnabled", enabledParam.variantAxis?.propertyName)

        // onClick callback
        val clickParam = comp.params.find { it.name == "onClick" }
        assertNotNull(clickParam, "Should find 'onClick' param")
        assertEquals("EXCLUDED", clickParam!!.kind)
        assertEquals("NONE", clickParam.binding.field)
        assertEquals(
            true,
            clickParam.required,
            "onClick callback should be required (forceRequired=true)"
        )
        assertEquals(0, clickParam.callbackArity)
    }

    @Test
    fun `excludes types not in mapping`() {
        val source = """
            package test.pkg
            
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Color
            
            @Composable
            fun StyledComponent(
                modifier: Modifier = Modifier,
                backgroundColor: Color? = null,
                label: String
            ) {
                // body
            }
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size)

        val comp = components.first()
        // Only label should be extracted (Modifier and Color are excluded)
        assertEquals(1, comp.params.size)
        assertEquals("label", comp.params.first().name)
    }

    @Test
    fun `extracts nullable parameters correctly`() {
        val source = """
            package test
            
            @Composable
            fun NullableComponent(
                label: String?,
                count: Int? = null
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val comp = components.first()

        val labelParam = comp.params.find { it.name == "label" }!!
        assertEquals(true, labelParam.nullable)
        assertEquals(false, labelParam.required) // nullable means optional

        val countParam = comp.params.find { it.name == "count" }!!
        assertEquals(true, countParam.nullable)
        assertEquals(false, countParam.required)
    }

    @Test
    fun `extracts DrawableResource with instance swap config`() {
        val source = """
            package test
            
            @Composable
            fun IconComponent(
                icon: DrawableResource,
                label: String
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val iconParam = components.first().params.find { it.name == "icon" }!!

        assertEquals("INSTANCE_SWAP", iconParam.kind)
        assertEquals("INSTANCE_SWAP", iconParam.binding.field)
        assertEquals("#swap:icon", iconParam.instanceSwap?.targetLayerMarker)
        assertEquals("componentNameMatchesKotlinResource", iconParam.instanceSwap?.contract)
    }

    @Test
    fun `parses callback with parameter`() {
        val source = """
            package test
            
            @Composable
            fun SwitchComponent(
                isChecked: Boolean,
                onCheckedChange: (Boolean) -> Unit
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val callbackParam = components.first().params.find { it.name == "onCheckedChange" }!!

        assertEquals("EXCLUDED", callbackParam.kind)
        assertEquals(1, callbackParam.callbackArity)
        assertEquals(true, callbackParam.required)
    }

    @Test
    fun `skips Preview functions`() {
        val source = """
            package test
            
            @Composable
            fun RealComponent(label: String) {}
            
            @Preview
            @Composable
            fun RealComponentPreview() {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size)
        assertEquals("RealComponent", components.first().componentName)
    }

    @Test
    fun `finds multiple composables in one file`() {
        val source = """
            package test
            
            @Composable
            fun ComponentA(label: String) {}
            
            @Composable
            fun ComponentB(count: Int) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(2, components.size)
        assertEquals(setOf("ComponentA", "ComponentB"), components.map { it.componentName }.toSet())
    }

    // ========================================================================
    // Nestable / Slot Detection Tests
    // ========================================================================

    @Test
    fun `component with Slot parameter is nestable`() {
        val source = """
            package test
            
            @Composable
            fun Container(
                label: String,
                content: Slot
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size)
        assertTrue(
            components.first().codeConnect.nestable,
            "Component with Slot param should be nestable"
        )
    }

    @Test
    fun `component with @Composable lambda is nestable`() {
        val source = """
            package test
            
            @Composable
            fun Container(
                label: String,
                content: @Composable () -> Unit
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size)
        assertTrue(
            components.first().codeConnect.nestable,
            "Component with @Composable lambda should be nestable"
        )
    }

    @Test
    fun `component with scoped @Composable lambda is nestable`() {
        val source = """
            package test
            
            @Composable
            fun Container(
                header: @Composable ColumnScope.() -> Unit
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size)
        assertTrue(
            components.first().codeConnect.nestable,
            "Component with scoped @Composable lambda should be nestable"
        )
    }

    @Test
    fun `component without slot is not nestable`() {
        val source = """
            package test
            
            @Composable
            fun Button(
                label: String,
                onClick: () -> Unit
            ) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        assertEquals(1, components.size)
        assertFalse(
            components.first().codeConnect.nestable,
            "Component without slot should not be nestable"
        )
    }

    @Test
    fun `hasComposableSlotParameter detects Slot type`() {
        assertTrue(hasComposableSlotParameter("content: Slot"))
        assertTrue(hasComposableSlotParameter("content: Slot?"))
        assertTrue(hasComposableSlotParameter("label: String, content: Slot"))
    }

    @Test
    fun `hasComposableSlotParameter detects @Composable lambda`() {
        assertTrue(hasComposableSlotParameter("content: @Composable () -> Unit"))
        assertTrue(hasComposableSlotParameter("header: @Composable ColumnScope.() -> Unit"))
        assertTrue(hasComposableSlotParameter("label: String, content: @Composable () -> Unit"))
    }

    @Test
    fun `hasComposableSlotParameter returns false for regular callbacks`() {
        assertFalse(hasComposableSlotParameter("onClick: () -> Unit"))
        assertFalse(hasComposableSlotParameter("onValueChange: (Boolean) -> Unit"))
        assertFalse(hasComposableSlotParameter("label: String, onClick: () -> Unit"))
    }

    // ========================================================================
    // Schema Merge Tests
    // ========================================================================

    @Test
    fun `merge replaces existing component by name`() {
        val existing = SchemaFile(
            schemaVersion = 1,
            generatedAt = "old",
            git = GitMetadata("old-commit"),
            components = listOf(
                Component(
                    componentName = "MyComponent",
                    kotlinFqName = "old.MyComponent",
                    codeConnect = CodeConnect(nestable = false),
                    params = listOf(
                        Param(
                            name = "oldParam",
                            kind = "TEXT",
                            nullable = false,
                            required = true,
                            defaultSource = "unknown",
                            binding = Binding("TEXT_CHARACTERS")
                        )
                    )
                ),
                Component(
                    componentName = "OtherComponent",
                    kotlinFqName = "other.OtherComponent",
                    codeConnect = CodeConnect(nestable = true),
                    params = emptyList()
                )
            )
        )

        val newComponent = Component(
            componentName = "MyComponent",
            kotlinFqName = "new.MyComponent",
            codeConnect = CodeConnect(nestable = true),
            params = listOf(
                Param(
                    name = "newParam",
                    kind = "BOOLEAN",
                    nullable = false,
                    required = true,
                    defaultSource = "literal",
                    binding = Binding("VARIANT_AXIS")
                )
            )
        )

        val merged = mergeComponent(existing, newComponent)

        assertEquals(2, merged.components.size)

        val myComp = merged.components.find { it.componentName == "MyComponent" }!!
        assertEquals("new.MyComponent", myComp.kotlinFqName)
        assertEquals(true, myComp.codeConnect.nestable)
        assertEquals(1, myComp.params.size)
        assertEquals("newParam", myComp.params.first().name)

        // Other component unchanged
        val otherComp = merged.components.find { it.componentName == "OtherComponent" }!!
        assertEquals("other.OtherComponent", otherComp.kotlinFqName)
    }

    @Test
    fun `merge appends new component`() {
        val existing = SchemaFile(
            schemaVersion = 1,
            generatedAt = "old",
            git = GitMetadata("old-commit"),
            components = listOf(
                Component(
                    componentName = "ExistingComponent",
                    kotlinFqName = "existing.ExistingComponent",
                    codeConnect = CodeConnect(nestable = false),
                    params = emptyList()
                )
            )
        )

        val newComponent = Component(
            componentName = "NewComponent",
            kotlinFqName = "new.NewComponent",
            codeConnect = CodeConnect(nestable = true),
            params = emptyList()
        )

        val merged = mergeComponent(existing, newComponent)

        assertEquals(2, merged.components.size)
        assertTrue(merged.components.any { it.componentName == "ExistingComponent" })
        assertTrue(merged.components.any { it.componentName == "NewComponent" })
    }

    // ========================================================================
    // Default Value Parsing Tests
    // ========================================================================

    @Test
    fun `parses string default value`() {
        val source = """
            package test
            @Composable
            fun Comp(label: String = "default text") {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val param = components.first().params.first()

        assertEquals(JsonPrimitive("default text"), param.default)
        assertEquals("literal", param.defaultSource)
    }

    @Test
    fun `parses boolean true default`() {
        val source = """
            package test
            @Composable
            fun Comp(enabled: Boolean = true) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val param = components.first().params.first()

        assertEquals(JsonPrimitive(true), param.default)
    }

    @Test
    fun `parses boolean false default`() {
        val source = """
            package test
            @Composable
            fun Comp(enabled: Boolean = false) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val param = components.first().params.first()

        assertEquals(JsonPrimitive(false), param.default)
    }

    @Test
    fun `parses int default value`() {
        val source = """
            package test
            @Composable
            fun Comp(count: Int = 42) {}
        """.trimIndent()

        val components = parseKotlinComposables(source, testTypeMapping)
        val param = components.first().params.first()

        assertEquals(JsonPrimitive(42), param.default)
    }

    // ========================================================================
    // Argument Parsing Tests
    // ========================================================================

    @Test
    fun `parseArgs returns Help for --help flag`() {
        val command = parseArgs(arrayOf("--help"))
        assertEquals(CliCommand.Help, command)
    }

    @Test
    fun `parseArgs returns Extract for --input flag`() {
        val command = parseArgs(arrayOf("--input", "path/to/file.kt"))
        assertTrue(command is CliCommand.Extract)
        assertEquals("path/to/file.kt", (command as CliCommand.Extract).inputPath)
    }

    @Test
    fun `parseArgs returns Canonicalize for --raw flag`() {
        val command = parseArgs(arrayOf("--raw", "path/to/raw.json"))
        assertTrue(command is CliCommand.Canonicalize)
        assertEquals("path/to/raw.json", (command as CliCommand.Canonicalize).rawPath)
    }

    @Test
    fun `parseArgs returns Canonicalize for --mode=canonicalize`() {
        val command = parseArgs(arrayOf("--mode=canonicalize", "--raw", "raw.json"))
        assertTrue(command is CliCommand.Canonicalize)
    }

    @Test
    fun `parseArgs handles equals-style arguments`() {
        val command = parseArgs(arrayOf("--input=path/to/file.kt", "--component=MyComp"))
        assertTrue(command is CliCommand.Extract)
        val extract = command as CliCommand.Extract
        assertEquals("path/to/file.kt", extract.inputPath)
        assertEquals("MyComp", extract.componentName)
    }

    @Test
    fun `parseArgs defaults to DiscoverAll when empty`() {
        val command = parseArgs(emptyArray())
        assertTrue(command is CliCommand.DiscoverAll)
        val discover = command as CliCommand.DiscoverAll
        assertEquals(
            "deveng-core/src/commonMain/kotlin/core/presentation/component",
            discover.componentDir
        )
        assertEquals(
            listOf(
                "deveng-core/src/commonMain/composeResources/drawable",
                "sample/composeApp/src/commonMain/composeResources/drawable"
            ),
            discover.drawableDirs
        )
        assertEquals("figma-sync/schema/component-schema.json", discover.schemaOut)
        assertEquals("figma-sync/schema/type-mapping.json", discover.mappingPath)
        assertEquals("figma-sync/schema/schema.overrides.json", discover.overridesPath)
    }

    @Test
    fun `parseArgs honors component and drawable overrides`() {
        val command = parseArgs(
            arrayOf(
                "--discover-all",
                "--component-dir", "/tmp/components",
                "--drawable-dir", "/tmp/drawable1",
                "--drawable-dir=/tmp/drawable2",
                "--schema-out", "custom.json"
            )
        )
        assertTrue(command is CliCommand.DiscoverAll)
        val discover = command as CliCommand.DiscoverAll
        assertEquals("/tmp/components", discover.componentDir)
        assertEquals(listOf("/tmp/drawable1", "/tmp/drawable2"), discover.drawableDirs)
        assertEquals("custom.json", discover.schemaOut)
    }
}

// ========================================================================
// Drawable Template Generation Tests (in separate file for clarity)
// ========================================================================

class DrawableTemplateTest {

    @Test
    fun `derives resource name from filename`() {
        // Test various icon naming patterns
        val testCases = listOf(
            "ic_cyclone.xml" to "ic_cyclone",
            "shared_ic_arrow_next.xml" to "shared_ic_arrow_next",
            "ic_dark_mode.png" to "ic_dark_mode",
            "icon_test.svg" to "icon_test"
        )

        for ((fileName, expected) in testCases) {
            val resourceName = fileName.substringBeforeLast(".")
            assertEquals(expected, resourceName, "Failed for $fileName")
        }
    }

    @Test
    fun `generates PascalCase icon name for template file`() {
        val testCases = listOf(
            "ic_cyclone" to "IcCyclone",
            "shared_ic_arrow_next" to "SharedIcArrowNext",
            "ic_dark_mode" to "IcDarkMode"
        )

        for ((resourceName, expected) in testCases) {
            val iconName = resourceName
                .split("_")
                .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            assertEquals(expected, iconName, "Failed for $resourceName")
        }
    }

    @Test
    fun `template includes figma url when provided`() {
        val template = generateTestDrawableTemplate("ic_test", "https://figma.com/test")
        assertTrue(template.contains("// url=https://figma.com/test"))
        assertTrue(!template.contains("PASTE FIGMA ICON COMPONENT URL HERE"))
    }

    @Test
    fun `template includes placeholder when no url provided`() {
        val template = generateTestDrawableTemplate("ic_test", null)
        assertTrue(template.contains("// url=<PASTE FIGMA ICON COMPONENT URL HERE>"))
    }

    @Test
    fun `template has correct structure`() {
        val template = generateTestDrawableTemplate("ic_cyclone", null)

        assertTrue(template.contains("const figma = require('figma')"))
        assertTrue(template.contains("export default {"))
        assertTrue(template.contains("example: figma.code`Res.drawable.ic_cyclone`"))
        assertTrue(template.contains("nestable: true"))
        assertTrue(template.contains("drawable: \"Res.drawable.ic_cyclone\""))
    }

    // Helper that mirrors the actual implementation logic
    private fun generateTestDrawableTemplate(resourceName: String, figmaUrl: String?): String {
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
}

// ========================================================================
// Discovery Helpers
// ========================================================================

class DiscoveryHelpersTest {
    @Test
    fun `discoverKotlinComponents finds kotlin files`() {
        val tempDir = createTempDirectory()
        val ktFile = tempDir.resolve("Sample.kt")
        ktFile.writeText(
            """
            package test
            @Composable
            fun Sample() {}
            """.trimIndent()
        )
        tempDir.resolve("ignore.txt").writeText("noop")

        val found = discoverKotlinComponents(tempDir)
        assertEquals(listOf(ktFile), found)
    }

    @Test
    fun `discoverDrawableResources finds icons`() {
        val tempDir = createTempDirectory()
        val iconFile = tempDir.resolve("ic_sample.xml")
        iconFile.writeText("<vector />")
        tempDir.resolve("note.txt").writeText("noop")

        val found = discoverDrawableResources(tempDir)
        assertEquals(listOf(iconFile), found)
    }
}
