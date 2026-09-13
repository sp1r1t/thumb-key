# Layout JSON schema

Versioned layout documents Suave loads into [`NamedLayout`](../app/src/main/java/com/suave/keyboard/layout/NamedLayout.kt). The runtime engine stays unchanged in spirit: JSON compiles to per-layer key grids plus optional content strips.

## Versioning

| Field | Rule |
| --- | --- |
| `schemaVersion` | Required integer. Current: `1`. |
| Unknown `schemaVersion` | Reject until a migrator exists for that version. |
| Unknown object fields | Ignored (forward compatible). |
| Unknown enum / command / modifier / cell / icon ids | Reject at load (fail loud). |

Migrators live in `layout/json/LayoutSchemaMigrator.kt`. v1 has an identity migrator only. There is no public legacy layout format: older private shapes are not accepted.

## Document shape (v1)

```json
{
  "schemaVersion": 1,
  "id": "s12",
  "title": "Suave Layout",
  "homeLayerId": "main",
  "caseMaps": {
    "shift": { "ß": "SS", "sch": "Sch" },
    "capsLock": { "sch": "SCH" }
  },
  "spaceMultitapCycle": [", ", ". ", "? ", "! ", ": ", "; "],
  "layers": [
    {
      "id": "main",
      "title": "ABC",
      "icon": "Abc",
      "overlay": false,
      "rows": [ /* cells */ ]
    },
    {
      "id": "emoji",
      "title": "Emoji",
      "icon": "EmojiEmotions",
      "overlay": true,
      "rows": [
        [ { "type": "emojiPicker", "rowSpan": 5, "columnSpan": 4 } ],
        [ /* bottom keys */ ]
      ]
    }
  ]
}
```

| Field | Notes |
| --- | --- |
| `homeLayerId` | Required. Must match a `layers[].id`. Keyboard starts here. |
| `caseMaps.shift` / `caseMaps.capsLock` | Layout-wide string rewrites while Shift / Caps Lock are active. |
| `spaceMultitapCycle` | Optional. Replacements after the first space tap; omit for the engine default. |
| `layers` | Required, non-empty. Cap: 14 layers. Ids must be unique, non-blank. |

Well-known S12 layer ids: `main`, `numeric`, `emoji`, `clipboard`. Any other id is a normal layer (same model).

### Layer object

| Field | Notes |
| --- | --- |
| `id` | Stable string used by `switchLayer` and settings height overrides. |
| `title` | Chip / editor label. |
| `icon` | [`LayerIcon`](../app/src/main/java/com/suave/keyboard/layout/LayerDefinition.kt) name (`Abc`, `Numbers`, `EmojiEmotions`, `History`, `Functions`, ...). |
| `overlay` | Default `false`. Overlays remember the previous base layer and return to it when left. |
| `rows` | Array of rows; each row is an array of cells. |

### Cells (tagged union)

| type | Fields | Meaning |
| --- | --- | --- |
| `key` | `columnSpan` (number, default 1), `slide`, `style`, `zones` | Normal keycap. |
| `spacer` | `columnSpan` (number, default 1) | Invisible width-only gap (row insets). Not drawn, not interactive. |
| `emojiPicker` | `rowSpan` (default 1), `columnSpan` (default 1) | Built-in emoji picker strip. |
| `clipboardHistory` | `rowSpan`, `columnSpan` | Built-in clipboard history strip. |

**This cut:** a content panel (`emojiPicker` / `clipboardHistory`) must be the only cell in its row (full-width strip). Height of that strip is `rowSpan` key-height units. `spacer` cells may sit beside keys in the same row. Side-by-side panels next to keys are deferred.

Uneven key-row lengths are allowed. Keys compile left-to-right; column index is the key's index in the row (span does not skip columns for later keys). Fractional `columnSpan` (e.g. `0.5`) is allowed for half-key insets.

### Key object (`type: "key"`)

```json
{
  "type": "key",
  "columnSpan": 1,
  "slide": { "axis": "HORIZONTAL", "behavior": "SELECT_AND_DELETE" },
  "style": { "fill": "auto" },
  "zones": {
    "center": { "type": "command", "id": "BACKSPACE" },
    "up": { "type": "text", "value": "'" },
    "down": { "type": "text", "value": "\"" }
  }
}
```

| Field | Notes |
| --- | --- |
| `columnSpan` | Optional number, default `1`. Fractions allowed (e.g. `0.5`, `1.5`). |
| `slide.axis` | `HORIZONTAL` \| `VERTICAL` \| `BOTH`. |
| `slide.behavior` | `MOVE_CURSOR` \| `SELECT_AND_DELETE`. |
| `style.fill` | `auto` (default) \| `letter` \| `control` \| `spacer`. |
| `zones` | Map of zone name -> action. Only occupied directional zones are listed. |

Zone names: `center`, `up`, `down`, `left`, `right`, `upLeft`, `upRight`, `downLeft`, `downRight`.

### Zone actions (tagged union)

| type | Fields | Maps to |
| --- | --- | --- |
| `text` | `value`, optional `label`, optional `repeatsOnHold`, optional `case` | `KeyIntent.Text` |
| `command` | `id` ([CommandId](../app/src/main/java/com/suave/keyboard/engine/intent/CommandId.kt) name), optional `label`, optional `repeatsOnHold` | `KeyIntent.Command` |
| `modifier` | `id` (`SHIFT` \| `CTRL` \| `ALT` \| `ESC` \| `META`) | `KeyIntent.ModifierPress` |
| `switchLayer` | `layerId` (any `layers[].id`) | `KeyIntent.SwitchLayer` |
| `noop` | (none) | `KeyIntent.Noop` |

`label`: when set, the key legend shows `label` while commit still uses `value` (text) or the command id (commands). Useful for IME action keys (`Search` / `Done`) and labeled Fn keys.

Notable commands beyond editing: `IME_ACTION` (perform the editor's current action), `HIDE_KEYBOARD`, `META` (also available as a modifier).

### Per-text case overrides

Layout `caseMaps` are the default. A text zone may set:

```json
{
  "type": "text",
  "value": "ß",
  "case": { "shift": "ẞ", "capsLock": null }
}
```

| JSON | Meaning |
| --- | --- |
| omit `case.shift` / `case.capsLock` | Inherit layout `caseMaps`, then engine default casing. |
| `"shift": "SS"` | Use this string under Shift. |
| `"shift": null` | Disable maps and automatic uppercase; commit/show raw `value`. |

Same rules for `capsLock`.

### Space multitap

Optional `spaceMultitapCycle`: list of replacement strings after the first space tap (first tap is always a plain space from the key's center action). When omitted, the engine uses its built-in cycle.

## Builtin assets

Shipped under `app/src/main/assets/layouts/<id>.json`. Registry keys by string `id` (e.g. `s12`), not ordinal.

| id | Role |
| --- | --- |
| `s12` | Suave / MessagEase-style 4x4 (default) |
| `unexpected` | Dense QWERTY with corner glyphs (Unexpected-inspired) |
| `simple` | Phone QWERTY with inset rows (iOS-inspired) |
| `terminal` | PC-like terminal keyboard with number row and Meta |

## User layouts

Copied or edited layouts live in app files (`files/layouts/<id>.json`) with a Room index. Import/export shares the same JSON document.

## Appearance height overrides

Device settings may override a layer's total height in key-row units with a blob keyed by layer id (`emoji=8,main=5`). Overrides never shrink below the key grid.

## Deferred / intentional holes

- Side-by-side content cell next to keys in one row (2D packer).
- Extra Thumb-Key-era commands not in `CommandId`: add the command and JSON enum together when a layout requires them.
