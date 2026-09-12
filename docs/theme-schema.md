# Theme JSON schema

Versioned theme documents Suave loads into named color palettes for [`SuaveTheme`](../app/src/main/java/com/suave/keyboard/ui/theme/Theme.kt).

## Versioning

| Field | Rule |
| --- | --- |
| `schemaVersion` | Required integer. Current: `2`. |
| Unknown `schemaVersion` | Reject until a migrator exists. |
| Unknown object fields | Ignored. |
| Unknown color role keys | Reject at load. |
| v1 documents | Migrated on load: missing `error*` / `success*` roles filled from Suave defaults. |

## Document shape (v2)

```json
{
  "schemaVersion": 2,
  "id": "suave",
  "title": "Suave",
  "light": {
    "primary": "#FF1B1B1B",
    "onPrimary": "#FFF4F4F4",
    "secondary": "#FF5C5C5C",
    "onSecondary": "#FFF4F4F4",
    "tertiary": "#FF111111",
    "onTertiary": "#FFF4F4F4",
    "background": "#FFE6E6E6",
    "onBackground": "#FF1B1B1B",
    "surface": "#FFF3F3F3",
    "onSurface": "#FF1B1B1B",
    "surfaceVariant": "#FFE0E0E0",
    "onSurfaceVariant": "#FF4A4A4A",
    "outline": "#FFB5B5B5",
    "inversePrimary": "#FFCFCFCF",
    "tertiaryContainer": "#FFD2D2D2",
    "onTertiaryContainer": "#FF1B1B1B",
    "error": "#FFB33B3B",
    "onError": "#FFFFFFFF",
    "errorContainer": "#FFF5D6D6",
    "onErrorContainer": "#FF3F1010",
    "success": "#FF2E7D32",
    "onSuccess": "#FFFFFFFF",
    "successContainer": "#FFC8E6C9",
    "onSuccessContainer": "#FF1B5E20"
  },
  "dark": { }
}
```

Colors are ARGB hex strings (`#AARRGGBB` or `#RRGGBB`). Both `light` and `dark` maps are required and must include every role listed above.

`error*` roles map onto Material `ColorScheme`. `success*` roles are Suave extras via `LocalSemanticExtras` (affirmative actions, not a Material slot).

## Special id

`dynamic` is not a JSON document: it selects Android 12+ Material You schemes. Settings may store `themeColor` as the string id `"dynamic"` or a theme document id. Dynamic themes use Material You for Material roles and Suave soft defaults for success extras.

## Builtin / user storage

Builtins ship under `app/src/main/assets/themes/<id>.json` (or as code seeds that encode to the same schema). User themes live in `files/themes/<id>.json` with a Room index. Import/export shares the same JSON document.
