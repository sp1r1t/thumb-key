# Theme JSON schema

Versioned theme documents Suave loads into named color palettes for [`SuaveTheme`](../app/src/main/java/com/suave/keyboard/ui/theme/Theme.kt).

## Versioning

| Field | Rule |
| --- | --- |
| `schemaVersion` | Required integer. Current: `1`. |
| Unknown `schemaVersion` | Reject until a migrator exists. |
| Unknown object fields | Ignored. |
| Unknown color role keys | Reject at load. |

## Document shape (v1)

```json
{
  "schemaVersion": 1,
  "id": "suave",
  "title": "Suave",
  "light": {
    "primary": "#FFA63166",
    "onPrimary": "#FFFFFFFF",
    "secondary": "#FF745660",
    "onSecondary": "#FFFFFFFF",
    "tertiary": "#FF7D5636",
    "onTertiary": "#FFFFFFFF",
    "background": "#FFFFFBFF",
    "onBackground": "#FF201A1C",
    "surface": "#FFFFFBFF",
    "onSurface": "#FF201A1C",
    "surfaceVariant": "#FFF2DDE2",
    "onSurfaceVariant": "#FF514347",
    "outline": "#FF837377",
    "inversePrimary": "#FFFFB0CB",
    "tertiaryContainer": "#FFFFDCC3",
    "onTertiaryContainer": "#FF2F1500"
  },
  "dark": { }
}
```

Colors are ARGB hex strings (`#AARRGGBB` or `#RRGGBB`). Both `light` and `dark` maps are required and must include every role listed above.

## Special id

`dynamic` is not a JSON document: it selects Android 12+ Material You schemes. Settings may store `themeColor` as the string id `"dynamic"` or a theme document id.

## Builtin / user storage

Builtins ship under `app/src/main/assets/themes/<id>.json` (or as code seeds that encode to the same schema). User themes live in `files/themes/<id>.json` with a Room index. Import/export shares the same JSON document.
