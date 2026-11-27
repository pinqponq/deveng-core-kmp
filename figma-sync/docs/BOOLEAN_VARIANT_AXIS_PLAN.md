# Boolean Variant Axis Plan (Phase 1)

This document defines the canonical, enforced behavior for representing Boolean parameters as variant axes in Figma, and how that is consumed by:

- **schema-cli** (validation + canonical schema)
- **template-generator** (Template V2 output)
- **drift-auditor** (schema ↔ Figma REST drift checks)

This is a Phase-1 policy: it focuses on property existence/type + basic reference presence. It does not claim deep semantic binding validation.

## Problem Statement

Sometimes a Kotlin boolean parameter is not a simple on/off visibility toggle. It can represent a structural UI difference that the design system models as a variant axis in Figma (e.g., enabled/disabled states built as variants).

We must support this cleanly without "magic guessing".

## Canonical Representation

### When a Boolean is a Variant Axis

A boolean param MAY be represented as a variant axis only when the design is structurally modeled as variants (or must remain variants for DS reasons).

In schema:
- `kind = "BOOLEAN"`
- `binding.field = "VARIANT_AXIS"`
- `variantAxis.propertyName` is optional and defaults to `name` when missing
- `variantAxis.valueMap` MUST be present and map Figma option keys → Kotlin boolean values

**Example:**

Figma axis options: `"true"`, `"false"` (recommended)

Schema:
```json
{
  "name": "isEnabled",
  "kind": "BOOLEAN",
  "nullable": false,
  "required": false,
  "default": true,
  "defaultSource": "literal",
  "binding": {
    "field": "VARIANT_AXIS"
  },
  "variantAxis": {
    "propertyName": "isEnabled",
    "valueMap": {
      "true": true,
      "false": false
    }
  }
}
```

### When a Boolean is NOT a Variant Axis

If the boolean is not a structural variant axis, in Phase-1 it should be modeled as:
- `PROP_ONLY` if it's a cosmetic toggle or code-only behavior (no Figma binding required)
  - If `binding.field=PROP_ONLY` and `required=true`, schema must include a `default` (and `defaultSource`) for deterministic behavior
- `NONE` if the binding is unresolved (must be decided; generator will emit TODO placeholder)

## Choosing VARIANT_AXIS vs PROP_ONLY vs NONE (Phase-1)

- **VARIANT_AXIS**: Use only when the boolean represents a structural variant (layout-affecting, mutually exclusive visual states modeled as component set variants in Figma)
- **PROP_ONLY**: Use for cosmetic toggles, visibility states, or code-only behavior that doesn't need Figma binding in Phase-1
  - If `binding.field=PROP_ONLY` and `required=true`, schema must include a `default` (and `defaultSource`) for deterministic behavior (matches schema-cli validation `validatePropOnlyDefaults` and template generator behavior)
- **NONE**: Use when binding intent is unresolved (requires explicit decision; generator emits TODO placeholder)

**Note on NONE:** `NONE` is a temporary authoring/unresolved state. drift-auditor reports it as an error (`[NONE_BINDING] ... requires resolution`) for any param that is not `kind=EXCLUDED`. Therefore, Phase-1 "green" requires resolving NONE (callbacks are `kind=EXCLUDED` and are exempt).

**Phase-1 binding.field values:** `TEXT_CHARACTERS`, `VARIANT_AXIS`, `INSTANCE_SWAP`, `PROP_ONLY`, `NONE`

## Schema Rules (Authoritative)

### Binding Key Resolution

When matching schema params to Figma property definitions and references:

- If `binding.field == "VARIANT_AXIS"`:
  - the "binding key" is `variantAxis.propertyName` if present, otherwise `name`
- Else:
  - the "binding key" is `name`

This allows a Kotlin param name and the Figma axis name to differ, but only when explicitly declared via `variantAxis.propertyName`.

### Required Fields

For BOOLEAN variant axes:
- `variantAxis.valueMap` is required
- `valueMap` keys must match the Figma axis option labels (e.g., `"true"`, `"false"` or `"On"`, `"Off"`)
- `valueMap` values must be boolean (`true`/`false`)

## Tooling Behavior

### schema-cli (Validation)

schema-cli must hard-fail if:

A param has:
- `kind == "BOOLEAN"`
- `binding.field == "VARIANT_AXIS"`
- and `variantAxis.valueMap` is missing or empty

**Rationale:** generator and auditor depend on a deterministic mapping from Figma option → boolean.

### template-generator (Template V2)

If:
- `kind == "BOOLEAN"` and `binding.field == "VARIANT_AXIS"`

Then generate:
```javascript
const <paramName>Expr = i.getEnum("<bindingKey>", { "<opt>": true/false, ... });
```

**Notes:**
- The template must not guess: it reads only instance properties.
- For boolean variant axes, we always read via `getEnum` (because Figma exposes variant axes as enum-like).

### drift-auditor (Phase-1 Checks)

#### Expected Figma Type

For any param with `binding.field == "VARIANT_AXIS"`:
- expected property type is `VARIANT`

Otherwise:
- `TEXT` → `TEXT`
- `BOOLEAN` → `BOOLEAN`
- `INSTANCE_SWAP` → `INSTANCE_SWAP`
- `ENUM` (typically VARIANT) → `VARIANT` when it is a variant axis representation

#### Ghost Detection Policy (Phase-1)

Ghost property = "defined in Figma but not referenced by any layer's `componentPropertyReferences`".

In Phase-1 auditor:
- `PROP_ONLY` is exempt (no layer reference expected)
- `VARIANT_AXIS` is exempt (variants may not appear in node-layer references in REST payloads consistently)
- Everything else is eligible for ghost errors.

#### Variant Value-Domain Validation (Best-Effort)

If schema includes `variantAxis.valueMap`:
- If REST payload includes an options list for that property:
  - error on missing/extra option labels vs schema `valueMap` keys
- If REST payload does not include options:
  - emit `WARNING: [SKIPPED] Variant value-domain validation skipped ... option list not available in REST payload`
- “Params with binding.field = "PROP_ONLY" are exempt from MISSING checks, because they are not required to exist as Figma properties.”

## Practical Example: CustomIconButton.isEnabled

Current expected state:

- Kotlin param name: `isEnabled`
- Schema:
  ```json
  {
    "name": "isEnabled",
    "kind": "BOOLEAN",
    "nullable": false,
    "required": false,
    "default": true,
    "defaultSource": "literal",
    "binding": {
      "field": "VARIANT_AXIS"
    },
    "variantAxis": {
      "propertyName": "isEnabled",
      "valueMap": {
        "true": true,
        "false": false
      }
    }
  }
  ```
- Template output reads:
  ```javascript
  i.getEnum("isEnabled", { "true": true, "false": false })
  ```

## Migration Note: isEnable → isEnabled

If the system previously used `isEnable`:

Treat this as a breaking rename of a public API + schema key.

Required updates (in order):
1. Kotlin param rename to `isEnabled`
2. Raw schema / overrides updated so schema param name is `isEnabled`
3. Templates + golden tests regenerated
4. Figma property/axis renamed to `isEnabled` (plugin responsibility in Phase-2) or manually corrected

After migration, do not keep both names unless explicitly implementing a deprecation strategy in schema (not part of Phase-1).

## Acceptance Criteria (for this plan)

This plan is "correct" when:

1. The doc contains no corrupted/truncated tokens.
2. It matches current tool behavior:
   - schema-cli fails on missing boolean `valueMap` for `VARIANT_AXIS`
   - generator uses `getEnum` for BOOLEAN variant axes
   - auditor expects `VARIANT` for `VARIANT_AXIS` and emits `[SKIPPED]` warnings when options aren't available
   - auditor exempts `VARIANT_AXIS` and `PROP_ONLY` from ghost errors
