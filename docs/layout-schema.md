# Layout JSON schema

Versioned layout documents Suave loads into [`NamedLayout`](../app/src/main/java/com/suave/keyboard/layout/NamedLayout.kt). The runtime engine stays unchanged: JSON compiles to `Map<KeyPosition, KeyMapping>` plus layer metadata.

## Versioning

| Field | Rule |
| --- | --- |
| `schemaVersion` | Required integer. Current: `1`. |
| Unknown `schemaVersion` | Reject until a migrator exists for that version. |
| Unknown object fields | Ignored (forward compatible). |
| Unknown enum / command / modifier ids | Reject at load (fail loud). |

Migrators live in `layout/json/LayoutSchemaMigrator.kt`. v1 has an identity migrator only.

## Document shape (v1)

```json
{
  "schemaVersion": 1,
  "id": "s12",
  "title": "Suave Layout",
  "rows": [ /* main layer */ ],
  "numeric": [ /* optional rows */ ],
  "emojiBottomRow": [ /* optional single row */ ],
  "clipboardBottomRow": [ /* optional single row */ ],
  "shiftMappings": { "ß": "SS" },
  "capsLockMappings": { "sch": "SCH" },
  "layerHeights": { "EMOJI": 6, "CLIPBOARD": 6 },
  "layerContent": { "EMOJI": "emojiPicker", "CLIPBOARD": "clipboardHistory" },
  "spaceMultitapCycle": [", ", ". ", "? ", "! ", ": ", "; "]
}
```

### Rows and keys

- `rows` / `numeric` / overlay bottom rows: arrays of rows; each row is an array of keys.
- Uneven row lengths are allowed.
- Keys compile left-to-right; column index is the key's index in the row (span does not skip columns for later keys - Enter at index 3 with `columnSpan: 2` still sits at col 3).

### Key object

```json
{
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
| `columnSpan` | Optional, default `1`. |
| `slide.axis` | `HORIZONTAL` \| `VERTICAL` \| `BOTH` ([SlideAxis](../app/src/main/java/com/suave/keyboard/engine/gesture/GestureConfig.kt)). |
| `slide.behavior` | `MOVE_CURSOR` \| `SELECT_AND_DELETE` ([SlideBehavior](../app/src/main/java/com/suave/keyboard/engine/intent/Layout.kt)). |
| `style.fill` | `auto` (default) \| `letter` \| `control`. |
| `zones` | Map of zone name -> action. Only occupied directional zones are listed; swipe geometry is inferred from occupancy (no FOUR_WAY / EIGHT_WAY). |

Zone names: `center`, `up`, `down`, `left`, `right`, `upLeft`, `upRight`, `downLeft`, `downRight`.

### Zone actions (tagged union)

| type | Fields | Maps to |
| --- | --- | --- |
| `text` | `value` (string), optional `label` (display-only), optional `repeatsOnHold` | `KeyIntent.Text` |
| `command` | `id` ([CommandId](../app/src/main/java/com/suave/keyboard/engine/intent/CommandId.kt) name), optional `repeatsOnHold` | `KeyIntent.Command` |
| `modifier` | `id` (`SHIFT` \| `CTRL` \| `ALT` \| `ESC`) | `KeyIntent.ModifierPress` |
| `noop` | (none) | `KeyIntent.Noop` |

`label`: when set, the key legend shows `label` while commit still uses `value` (numeric combining marks, etc.).

`repeatsOnHold`: optional override of the engine default for that command/text.

### Layer content

`layerContent` values: `none` \| `emojiPicker` \| `clipboardHistory`.

### Space multitap

Optional `spaceMultitapCycle`: list of replacement strings after the first space tap (first tap is always a plain space from the key's center action). When omitted, the engine uses its built-in cycle.

## Builtin assets

Shipped under `app/src/main/assets/layouts/<id>.json`. Registry keys by string `id` (e.g. `s12`), not ordinal.

## User layouts

Copied or edited layouts live in app files (`files/layouts/<id>.json`) with a Room index. Import/export shares the same JSON document.

## Deferred / intentional holes

These are not silent gaps in the schema; they wait for a real layout need:

- Extra Thumb-Key-era commands not in `CommandId` (word nav, HideKeyboard, IME complete, etc.): add the command and JSON enum together when a layout requires them.
- Pure-grid replacements for emoji/clipboard overlays: keep using `layerContent` until designed.
