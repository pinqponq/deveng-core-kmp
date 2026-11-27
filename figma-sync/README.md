# Figma ↔ Compose Sync System

## Document Purpose

This document provides a complete guide for synchronizing Jetpack Compose components with Figma using Code Connect.
After reading this document, you will be able to:

- Extract schemas from Kotlin composable functions
- Configure component manifests with Figma URLs
- Generate Code Connect templates
- Run drift audits to detect mismatches
- Publish components to Figma

> 📌 For the Turkish version of this document, see [README.tr.md](./README.tr.md)

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Core Concepts](#2-core-concepts)
3. [Quick Start](#3-quick-start)
4. [Step-by-Step Workflow](#4-step-by-step-workflow)
5. [CLI Reference](#5-cli-reference)
6. [Type Mapping Reference](#6-type-mapping-reference)
7. [Troubleshooting](#7-troubleshooting)
8. [Directory Structure](#8-directory-structure)

---

## 1. Prerequisites

Before using this system, ensure you have:

| Requirement | Description |
|-------------|-------------|
| Node.js & npm | Required for Code Connect CLI |
| Figma Access Token | Set as `FIGMA_ACCESS_TOKEN` environment variable |
| Gradle | For running schema extraction and template generation |

**Setup commands:**

```bash
# Install npm dependencies
npm install

# Set Figma token (required for drift audit and publishing)
export FIGMA_ACCESS_TOKEN="your-token-here"
```

---

## 2. Core Concepts

### 2.1 Schema

A JSON file that describes component parameters and their Figma bindings. The schema maps Kotlin function parameters to Figma properties.

### 2.2 Manifest

The `components.manifest.json` file registers components with their Figma URLs and template file paths.

### 2.3 Template

JavaScript files (`.figma.template.js`) that generate Compose code snippets displayed in Figma Dev Mode.

### 2.4 Binding Types

| Binding | Purpose |
|---------|---------|
| `TEXT_CHARACTERS` | Binds to text layer content via `#bind:paramName` marker |
| `VARIANT_AXIS` | Maps to Figma variant properties |
| `INSTANCE_SWAP` | Swaps icon instances via `#swap:paramName` marker |
| `PROP_ONLY` | Code-only parameter, no Figma layer binding |
| `NONE` | Excluded from bindings (callbacks) |

### 2.5 Layer Markers

| Marker | Purpose | Example |
|--------|---------|---------|
| `#bind:paramName` | Text content binding | Layer: `Label #bind:label` |
| `#swap:paramName` | Instance swap slot | Layer: `Icon #swap:icon` |

### 2.6 Nestable Components

Components that accept composable content (slots) are marked as `nestable: true` in the schema. This allows them to contain child components in Figma Code Connect.

The schema CLI automatically detects slot parameters:
- `Slot` type (typealias for `@Composable () -> Unit`)
- `@Composable () -> Unit` lambda parameters
- `@Composable SomeScope.() -> Unit` scoped lambda parameters

**Example:**

```kotlin
@Composable
fun Container(
    label: String,
    content: Slot  // This makes the component nestable
) { ... }
```

Generates schema with:

```json
{
  "componentName": "Container",
  "codeConnect": {
    "nestable": true
  }
}
```

---

## 3. Quick Start

### 3.1 Full Pipeline (Single Command)

```bash
./gradlew figmaSync
```

This command:
1. Discovers all `@Composable` components and `DrawableResource` files
2. Generates a merged `component-schema.json`
3. Updates `components.manifest.json` (creates placeholders or uses interactive input)
4. Creates drawable templates in `figma-sync/templates/icons/`
5. Generates Code Connect templates for all components
6. Runs drift audit against Figma

### 3.2 Discover All Components (Default)

When no arguments are provided, the CLI automatically discovers all `@Composable` functions and `DrawableResource` files in the project:

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema
```

This command:
1. Scans `deveng-core/src/commonMain/kotlin/core/presentation/component/` for Kotlin files with `@Composable` functions
2. Scans drawable directories for icon resources (`ic_*.xml`, `shared_ic_*.xml`, etc.)
3. Generates a merged `component-schema.json` with all discovered components
4. Creates template files for each discovered drawable in `figma-sync/templates/icons/`

### 3.3 Extract Schema from Single Kotlin File

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema \
  -Pargs="--input deveng-core/src/commonMain/kotlin/core/presentation/component/YourComponent.kt"
```

### 3.4 Generate Manifest

```bash
./gradlew :figma-sync:tools:manifest-cli:generateManifest
# Add -Pinteractive=true to be prompted for Figma URLs
```

Output: Updates `figma-sync/schema/components.manifest.json`, adding entries for any schema components that are missing and filling placeholders (`<PASTE_FIGMA_URL_HERE>`, `<FILE_KEY>`, `<NODE_ID>`) if URLs are not provided.

### 3.5 Generate Templates

```bash
./gradlew :figma-sync:tools:template-generator:generateTemplates
```

> Note: This task only renders templates. Ensure `figma-sync/schema/component-schema.json` is already up to date (run `generateSchema` or `generateSchemaManuel` first) before invoking this command.

### 3.6 Publish to Figma

```bash
npm run codeconnect:publish
```

---

## 4. Step-by-Step Workflow

### Step 1: Create Schema

**Option A: Discover All Components (Recommended for Full Sync)**

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema
```

When run without arguments, the CLI:
- Recursively scans the component directory for `.kt` files containing `@Composable` functions
- Discovers drawable resources matching icon patterns (`ic_*.xml`, `shared_ic_*.xml`, etc.)
- Generates a single merged schema with all components
- Creates individual template files for each drawable in `figma-sync/templates/icons/`

You can customize the search directories:

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema \
  -Pargs="--component-dir custom/path/to/components --drawable-dir path/to/drawables"
```

**Option B: Extract from Single Kotlin File**

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema \
  -Pargs="--input path/to/YourComponent.kt"
```

The extractor:
- Parses function signature and parameters
- Maps Kotlin types using `type-mapping.json`
- Filters excluded types (Modifier, Color, TextStyle, Shape, Dp)
- Generates appropriate bindings

**Example input:**

```kotlin
@Composable
fun CustomIconButton(
    modifier: Modifier = Modifier,      // Excluded
    isEnabled: Boolean = true,          // VARIANT_AXIS
    icon: DrawableResource,             // INSTANCE_SWAP
    iconDescription: String,            // PROP_ONLY
    onClick: () -> Unit                 // EXCLUDED
)
```

**Option C: Manual Authoring**

Edit `schema/component-schema.raw.json` directly, then canonicalize:

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel \
  --args="--raw figma-sync/schema/component-schema.raw.json"
```

### Step 2: Configure Manifest

Generate or update manifest entries for all discovered components (creates stub entries when missing):

```bash
./gradlew :figma-sync:tools:manifest-cli:generateManifest
# Add --interactive to be prompted for Figma URLs
```

What this does:
- Reads `figma-sync/schema/component-schema.json` and ensures every component has a manifest entry.
- Preserves any existing manifest entries (including components that are not in the latest schema).
- For new components, writes placeholders: `componentUrl: "<PASTE_FIGMA_URL_HERE>"`, `fileKey: "<FILE_KEY>"`, `nodeId: "<NODE_ID>"`.
- If `--interactive` is used, prompts for the Figma URL and auto-fills `fileKey`/`nodeId`.

After running, open `schema/components.manifest.json` and fill the placeholders with real Figma URLs before generating templates.

Add or edit your component in `schema/components.manifest.json`:

```json
{
  "componentName": "YourComponent",
  "kotlinFqName": "core.presentation.component.YourComponent",
  "codeConnect": {
    "templateFile": "figma-sync/templates/YourComponent.figma.template.js",
    "publish": { "source": "template-v2" }
  },
  "figma": {
    "fileKey": "YOUR_FILE_KEY",
    "nodeId": "NODE-ID",
    "componentUrl": "https://www.figma.com/design/YOUR_FILE_KEY/Design-System?node-id=NODE-ID"
  }
}
```

**Getting Figma URL:**
1. Open Figma file
2. Select the component
3. Right-click → "Copy link to selection"
4. Extract `fileKey` and `nodeId` from URL

### Step 3: Generate Templates

```bash
./gradlew :figma-sync:tools:template-generator:generateTemplates
```

Output: `templates/YourComponent.figma.template.js`

> Note: This task only renders templates. Ensure `figma-sync/schema/component-schema.json` is already up to date (run `generateSchema` or `generateSchemaManuel` first) before invoking this command.

### Step 4: Run Drift Audit

```bash
export FIGMA_ACCESS_TOKEN="your-token"
./gradlew :figma-sync:tools:drift-auditor:auditDrift
```

Review results in `schema/drift-report.md`.

### Step 5: Validate and Publish

```bash
# Validate templates
npm run codeconnect:parse

# Publish to Figma
npm run codeconnect:publish
```

---

## 5. CLI Reference

### 5.1 Gradle Tasks

| Task | Description |
|------|-------------|
| `figmaSync` | Full pipeline: schema → templates → audit |
| `generateSchema` | Extract schema from Kotlin or drawable |
| `generateManifest` | Create/update `components.manifest.json` from schema (stubs missing URLs) |
| `generateSchemaManuel` | Canonicalize raw JSON |
| `generateTemplates` | Generate Code Connect templates from an existing schema (does not run schema tasks) |
| `auditDrift` | Compare schema against Figma components |

**Task options (Gradle CLI):**
- Manifest: `./gradlew :figma-sync:tools:manifest-cli:generateManifest --interactive --schema <path> --manifest <path>`
- Templates: `./gradlew :figma-sync:tools:template-generator:generateTemplates --schema <path> --manifest <path> --templates <outDir>`
- Schema (discover all): `./gradlew :figma-sync:tools:schema-cli:generateSchema --discover-all --component-dir <dir> --drawable-dir <dir> --schema-out <path> --mapping <path> --overrides <path>`
- Schema (single file): `./gradlew :figma-sync:tools:schema-cli:generateSchema --input <file> --component <name> --figma-url <url> --schema-out <path> --template-out <path>`
- Schema canonicalize: `./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel --mode=canonicalize --raw <path> --out <path> --mapping <path> --overrides <path>`
- Drift: `./gradlew :figma-sync:tools:drift-auditor:auditDrift --schema <path> --manifest <path> --report-json <path> --report-md <path>`

`generateManifest` lives in the `figma-sync:tools:manifest-cli` module. Run it via:

```bash
./gradlew :figma-sync:tools:manifest-cli:generateManifest
```

### 5.2 generateSchema Arguments

**General Options:**

| Argument | Description |
|----------|-------------|
| `--help`, `-h` | Show help message with all available options |

**Discover-All Mode (default when no `--input` provided):**

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `--discover-all` | | | Explicitly trigger discover-all mode |
| `--component-dir <path>` | | `deveng-core/src/commonMain/kotlin/core/presentation/component` | Root directory for `@Composable` discovery |
| `--drawable-dir <path>` | | `deveng-core/src/commonMain/composeResources/drawable`, `sample/composeApp/src/commonMain/composeResources/drawable` | Drawable directory to scan (repeatable for multiple directories) |
| `--schema-out <path>` | | `figma-sync/schema/component-schema.json` | Output path for final schema JSON |
| `--mapping <path>` | | `figma-sync/schema/type-mapping.json` | Type mapping configuration file |
| `--overrides <path>` | | `figma-sync/schema/schema.overrides.json` | Schema overrides file for manual adjustments |

**Single-File Extraction Mode:**

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `--input <path>` | ✅ | | Kotlin file (`.kt`) or drawable resource path |
| `--component <name>` | | | Component name to extract (if file has multiple `@Composable` functions) |
| `--figma-url <url>` | | | Figma component URL (for drawable mode, prompts interactively if not provided) |
| `--schema-out <path>` | | `figma-sync/schema/component-schema.json` | Output path for schema JSON (replaces existing file) |
| `--template-out <path>` | | `figma-sync/templates/icons/<IconName>.figma.template.js` | Output path for drawable template |
| `--mapping <path>` | | `figma-sync/schema/type-mapping.json` | Type mapping configuration file |
| `--overrides <path>` | | `figma-sync/schema/schema.overrides.json` | Schema overrides file |

### 5.3 generateSchemaManuel Arguments

Used for the raw JSON to canonicalized schema workflow (manual/AI-assisted authoring):

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `--mode=canonicalize` | | | Force canonicalize-only mode (alternative to `--raw`) |
| `--raw <path>` | ✅ | | Raw schema JSON input file |
| `--out <path>` | | `figma-sync/schema/component-schema.json` | Output path for final schema JSON |
| `--overrides <path>` | | `figma-sync/schema/schema.overrides.json` | Schema overrides file |
| `--mapping <path>` | | `figma-sync/schema/type-mapping.json` | Type mapping file (for validation) |

**Example:**

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel \
  -Pargs="--raw figma-sync/schema/component-schema.raw.json --out figma-sync/schema/component-schema.json"
```

### 5.4 npm Scripts

| Script | Description |
|--------|-------------|
| `npm run codeconnect:parse` | Validate templates |
| `npm run codeconnect:publish` | Publish to Figma |

---

## 6. Type Mapping Reference

File: `schema/type-mapping.json`

| Kotlin Type | Schema Kind | Binding |
|-------------|-------------|---------|
| `String` | TEXT | TEXT_CHARACTERS |
| `Boolean` | BOOLEAN | VARIANT_AXIS |
| `DrawableResource` | INSTANCE_SWAP | INSTANCE_SWAP |
| `() -> Unit` | EXCLUDED | NONE |
| `(Boolean) -> Unit` | EXCLUDED | NONE |
| `Int`, `Float` | TEXT | PROP_ONLY |

**Excluded types:** `Modifier`, `Color`, `TextStyle`, `Shape`, `Dp`

**Adding new types:**

```json
{
  "MyCustomType": {
    "kind": "TEXT",
    "binding": "PROP_ONLY",
    "supportsLiteralDefault": true,
    "literalDefaultType": "string"
  }
}
```

---

## 7. Troubleshooting

| Issue | Solution |
|-------|----------|
| `FIGMA_ACCESS_TOKEN not set` | Run: `export FIGMA_ACCESS_TOKEN="..."` |
| Schema missing parameters | Check `type-mapping.json` for the Kotlin type |
| Drift: property not found | Verify Figma component has matching variant names |
| Template parse error | Check JS syntax; ensure `url=` comment is first line |
| Publish fails | Run `npm install` to verify dependencies |

---

## 8. Directory Structure

```
figma-sync/
├── schema/
│   ├── component-schema.json       # Canonicalized schema
│   ├── component-schema.raw.json   # Raw schema (manual workflow)
│   ├── components.manifest.json    # Component registry
│   ├── schema.overrides.json       # Override rules
│   ├── type-mapping.json           # Kotlin type mappings
│   ├── drift-report.json           # Audit results (JSON)
│   └── drift-report.md             # Audit results (Markdown)
├── templates/
│   ├── *.figma.template.js         # Component templates
│   └── icons/                      # Icon templates
└── tools/
    ├── schema-cli/                 # Schema extraction
    ├── template-generator/         # Template generation
    └── drift-auditor/              # Drift detection
```

---

## Workflow Summary

```
┌────────────────────────────────────────────────────────────┐
│  1. CREATE SCHEMA                                          │
│     generateSchema (discovers all) or --input Component.kt │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  2. CONFIGURE MANIFEST                                     │
│     Add entry to components.manifest.json                  │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  3. GENERATE TEMPLATES                                     │
│     generateTemplates                                      │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  4. AUDIT DRIFT                                            │
│     auditDrift → Review drift-report.md                    │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  5. PUBLISH                                                │
│     npm run codeconnect:publish                            │
└────────────────────────────────────────────────────────────┘
```

---

**Deveng Group - Figma Sync Team**
