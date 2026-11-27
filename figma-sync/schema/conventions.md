## Stable Property Naming
- Use camelCase for schema property names.
- Schema property name must match the Figma property name.
- Exception: `VARIANT_AXIS` may use `variantAxis.propertyName` as the axis/property name.

## Layer Targeting Markers
- `#bind:<prop>`: marks TEXT_CHARACTERS binding target layers (e.g., `#bind:label`).
- `#swap:<prop>`: marks INSTANCE_SWAP target layers (e.g., `#swap:icon`).

## Binding Field Meanings
- `TEXT_CHARACTERS`: text content from the layer with `#bind:<prop>`.
- `VARIANT_AXIS`: reads a variant axis value via `variantAxis.propertyName`/`valueMap`.
- `INSTANCE_SWAP`: swaps to an instance from the layer with `#swap:<prop>`.
- `PROP_ONLY`: no Figma layer reference; value is provided only via properties.
- `NONE`: no binding; generator ignores the Figma layer.

## Ghost Detection (summary)
- Bindings that point to layers (`TEXT_CHARACTERS`, `VARIANT_AXIS`, `INSTANCE_SWAP`) require matching layer markers; missing markers are treated as ghosts.
- `PROP_ONLY` and `NONE` bindings are exempt from ghost checks.

## Callback Policy
- Params with `kind: EXCLUDED` and `required: true` get stub lambdas in generated templates.
- `callbackArity` controls the stub signature (arity 0 → `{ }`, arity 1 → `{ _ -> }`, etc.).

## Variant Governance (guidance)
- Variant axes should describe structural variants.
- Use `PROP_ONLY` for purely cosmetic toggles instead of variant axes.
