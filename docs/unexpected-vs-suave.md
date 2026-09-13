# Unexpected Keyboard vs Suave

Reference for what each engine can express, where Unexpected hits a wall on Thumbkey / Typesplit, and which Unexpected features are worth adding to Suave.

Sources: [Julow/Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard) (`doc/Custom-layouts.md`, `doc/Possible-key-values.md`, `srcs/layouts`, `srcs/compose`, `srcs/juloo.keyboard2`), plus Suave `docs/layout-schema.md` and the engine under `app/src/main/java/com/suave/keyboard/engine`. Unexpected's shipped QWERTY is `latn_qwerty_us.xml`. Suave's homage is `app/src/main/assets/layouts/unexpected.json` (corners and a QWERTY grid, not a port of Unexpected's engine).

This is a snapshot of engine-level capability, not a list of the ~90 locale XML files Unexpected ships. Those are content. Hangul composition, Fn overlays, and circle gestures are engine.

## How each product thinks

Unexpected is one XML keyboard plus injected chrome. A layout file is a single grid. Numeric, emoji, clipboard, and Greek/math are **system** keyboards you switch to (`switch_numeric`, `switch_emoji`, ...). A shared bottom row and an optional number row are spliced in unless the XML opts out. Extra keys (`loc esc`, locale extras, Settings > Add keys) occupy reserved swipe slots without editing the file. Custom layouts are XML you paste in Settings, or build in the [web editor](https://domportera.github.io/app-unexpected-keyboard-layout-editor/).

Suave is one JSON **document** with many named layers. `main`, `numeric`, `emoji`, `clipboard`, and any custom id live in the same file. Overlay layers remember the previous base. Placement (Center, Left, Right, Dual, Split) is a device setting, not something the layout XML/JSON has to encode. Custom layouts are JSON, with an in-app visual editor and a JSON editor.

That split explains most of the asymmetry below. Unexpected is deep on in-place remapping (Fn, compose, dead keys, extra-key injection). Suave is deep on layout-as-data (layers, placement, content strips, Thumbkey-sized keys).

## Can Unexpected do Thumbkey or Typesplit?

**A static Thumbkey-style grid: yes, with caveats.** XML can be 4 rows of 4 keys, `bottom_row="false"`, eight compass swipes per key. Keys fill the row, so four columns become large thumb targets. Multi-character keys (`c="sch"`) exist as string keys. You can fake a gap with `shift` / `width` spacers in the XML.

**Typesplit / Dual as Suave means them: no.** Unexpected's landscape splitter inserts a gap in the middle of each row (and may duplicate the overlapping key). That is a QWERTY typesplit for wide screens, automatic and orientation-tied. It is not:

- Two full copies of the board (Dual) that keep Center key width and can overlap.
- Two parked halves with a pass-through gap you can tap through to the app (Split).
- A Move-keyboard cycle of Center / Left / Right / Dual / Split independent of the layout file.
- Per-layout `landscapeFloating` so the host keeps drawing in the gap.

**What still breaks a real Thumbkey on Unexpected:**

| Suave Thumbkey / S12 behavior | Unexpected |
| --- | --- |
| Empty swipe falls back to center (Thumb-Key) | Unassigned compass directions do nothing |
| Overlay numeric / emoji in the same document, return to previous base | System keyboards; no overlay stack |
| `caseMaps` for digraphs (`sch` -> `Sch` / `SCH`) | Shift is character uppercase / compose tables, not layout-wide string maps |
| Space multitap cycle as layout data | Not a layout feature |
| `emojiPicker` / `clipboardHistory` cells | Separate system panes, not cells in the grid |
| Hold-repeat on any zone, including a locked swipe | Long-press is a different rewrite path; sliders are special keys |
| Per-key slide `MOVE_CURSOR` / `SELECT_AND_DELETE` | Slider key values and `role="space_bar"` |
| Hide-letters and related category filters | No equivalent |
| Circle gestures off (large keys + 8 swipes) | Circles are always in the gesture recognizer; easy to steal a Thumbkey swipe |

So: you can paste a 4x4 XML and it will look Thumbkey-ish. You cannot get Suave's Typesplit/Dual placement, overlay layers, or Thumbkey empty-swipe rules without engine work on Unexpected's side. Suave should not take Unexpected's auto landscape-split as a substitute for placement we already have.

## What Suave has that Unexpected does not

These are Suave limits **for Unexpected**, and reasons not to copy Unexpected blindly.

- **Placement modes:** Center, Left, Right, Dual, Split, with cramped-Dual / needless-Split guards.
- **Named layers in one document**, including user-defined layers and overlay return.
- **Content cells:** `emojiPicker`, `clipboardHistory` (full-width strips).
- **JSON schema** with fail-loud unknown commands, migrators, tags, import/export.
- **In-app visual layout editor** plus JSON tree editor (Unexpected: paste XML or use a website).
- **User copies** of every layout (editable / deletable), templates only as add-layout starts.
- **Space multitap** as layout data (`spaceMultitapCycle`).
- **Digraph / string `caseMaps`** (Shift and Caps Lock).
- **Empty-swipe-to-center.**
- **Inferred swipe geometry** from occupied zones (`docs/swipe-zone-inference.md`).
- **Landscape floating** and per-app overrides.
- **Input-type -> numeric layer** when the layout has a `numeric` layer.
- **Theme editor** and semantic colors.
- **Modifier model:** HELD / ONE_SHOT / LOCKED uniformly (Shift-hold is caps). Esc can be a modifier or a key.
- **IME action legend** from live `EditorInfo` (search / send / done), not a fixed XML label.

Unexpected's web XML editor and 90 locale files are the other direction (see below). Do not treat "Unexpected cannot Typesplit" as a feature we still need to add. We already did.

## What Unexpected has that Suave does not

Grouped the same way as the earlier survey. Items marked **workaround** can be faked in JSON today (raw Unicode text, an extra layer) but are not first-class engine features.

### Gestures

- Clockwise **circle** (applies a `GESTURE` modifier: Shift map, else Fn map). Circle on Shift -> Caps Lock. Circle on Backspace -> delete-word.
- **Anti-clockwise circle**, per-key `anticircle="..."` (ninth action, not a compass zone).
- **Round-trip:** swipe out, return to center.
- 16-sector rotation tracking vs Suave's lock-to-8-zones.
- First-class **slider keys** (`cursor_*`, `selection_cursor_*`) with velocity-based step size, slower vertical, slower while Ctrl is held.
- **Selection-mode** modifier when the editor has a selection (space/arrows become selection sliders without holding Shift).
- Long-press **rewrite** (voice typing -> chooser, IME next/prev -> picker).
- Double-tap lock flag on keys (Shift lock by double-tap).

Suave already has 8-direction swipe, hold, hold-repeat, and per-key slide `HORIZONTAL|VERTICAL|BOTH` with `MOVE_CURSOR|SELECT_AND_DELETE`.

### Key value kinds and syntax

Layouts can only put `text`, `command`, `modifier`, `switchLayer`, `noop` on a zone. Missing:

- `legend:key_def` (label distinct from action, including quoted strings with `:`). Suave has optional `label` on text/command, which covers part of this.
- `keyevent:<android-keycode>` (arbitrary `KeyEvent`, including media keys).
- **Macros** (`legend:ctrl,a,ctrl,c`).
- Compose-pending / dead-key as a key kind.
- Hangul initial/medial syllable composition.
- Placeholders hidden until Fn (`f11_placeholder`, Hebrew `*_placeholder`, `removed`).
- Stateful suggestion keys (`complete_first` / `second` / `third` / `emoji`).
- Named specials as a vocabulary (`accent_aigu`, `nbsp`, `page_up`, ...). Unknown Unexpected tokens commit verbatim. Suave rejects unknown command/modifier ids at load.

### Modifiers, Fn, compose

Suave modifiers: `SHIFT`, `CTRL`, `ALT`, `ESC`, `META`. Unexpected also has:

- **Fn**, keyboard-internal. `fn.json` remaps in place (`!` -> `¡`, `1` -> F1, space -> nbsp, arrows -> Home/End/Page, paste -> paste-plain, undo -> redo, numeric -> greekmath, ...).
- **Compose** with Linux-style sequences (`Compose` `A` `'` -> `Á`), compiled from `srcs/compose/`.
- **Dead-key accent modifiers** that rewrite the next letter: acute, grave, circumflex, tilde, diaeresis, caron, cedilla, macron, ring, ogonek, breve, bar, slash, horn, hook-above, double acute/grave, dot above/below, small caps, superscript, subscript, ordinal, arrows, box-drawing.
- Layout **`<modmap>`**: Shift / Fn / Ctrl remaps (Turkish `i` -> `İ`, Cyrillic Ctrl-V via `в` -> `v`). Suave `caseMaps` only rewrite committed text under Shift/Caps.
- Combining-mark specials (Latin, Arabic, Slavonic, ...) as named keys. **workaround:** commit the codepoint as `text`.
- Mirrored bracket keys (`b(`, `blt`, ...) that send one character but flip the legend for RTL.
- Latch / greyed keys that are not in the current compose sequence.

Suave's `unexpected.json` has an **`fn` layer** (copy, cut, paste, ...). That is not Unexpected Fn. Unexpected Fn does not switch layer; it remaps the current grid in place.

### Commands and editing

In Unexpected, absent from `CommandId` / `OutputExecutor`:

- F1-F12, Page Up/Down, Home/End, Insert, Menu, Scroll Lock
- Paste as plain text
- Share selected text
- Delete word / forward delete word (keyboard-side, not "send Ctrl+Backspace and hope")
- Selection cancel
- Named NBSP / narrow NBSP / ZWJ / ZWNJ (**workaround:** `text`)
- Literal `\t` / `\n` characters, distinct from Tab/Enter keyevents
- `switch_forward` / `switch_backward` (cycle enabled layouts from a key). Suave has `SWITCH_LANGUAGE` (one direction).
- `switch_greekmath` (builtin layer). **workaround:** a JSON layer.
- `change_method_prev` / `change_method_next` (Suave `SWITCH_IME` is the picker)
- `voice_typing_chooser` (Suave `SWITCH_IME_VOICE`)
- `change_dictionary`
- `capslock` as its own key (Suave: Shift-hold lock)
- Documented unused: replace, text assist, autofill

Already covered in Suave: Enter, Tab, Backspace, forward delete, Space, arrows, Escape, copy/cut/paste, select all, undo/redo, settings, hide letters, switch IME, voice IME, switch language, move keyboard, layer toggles, IME action, hide keyboard, Meta, landscape floating.

### Layout document model

XML attributes with no JSON equivalent:

- `script` / `numpad_script` (`latin`, `arabic`, `hebrew`, `hangul`, `devanagari`, ...). Numpad digits follow compose tables (`numpad_devanagari.json`, ...).
- Shared **bottom row** (`bottom_row.xml`: Ctrl, Fn, space+cursor sliders+layout cycle, compose+arrows, enter/action), `bottom_row="true|false"`.
- Injected **number row** unless `embedded_number_row="true"`.
- `loc ...` placeholders plus Add extra keys / locale extras from `method.xml`.
- User-defined extra keys without editing the layout.
- Per-row `height` and `scale`. Suave: layout-wide `keyHeight` / `landscapeKeyHeight` only.
- Key `shift` (left inset in key-width units). **workaround:** `spacer` cells.
- `indication` (pinpad-style extra legend, independent of the down-swipe).
- `role`: `action`, `space_bar`, `suggestion`.
- Keyboard `width` metadata.
- Portrait vs landscape **last-used layout** stored separately.
- Automatic landscape row split (see Typesplit above).
- Builtin pin-entry / numeric XML as **system** layouts. Suave switches to the layout's own `numeric` layer.

### IME extras layouts assume

`EditorCapabilityResolver` still only distinguishes RAW vs basic text. Unexpected layouts also assume:

- On-device dictionaries (cdict) and word/emoji suggestions
- Suggestion completion from keys (`complete_*`)
- Currently-typed-word tracking (delete-word, suggestions)
- Hangul Jamo composition
- RTL / script-aware numpad and mirrored legends
- Foldable portrait/landscape height and margin variants

Suave already shows Android **inline suggestions** when the editor provides them. That is not Unexpected's own dictionary.

## Priority for Suave

Filter: keep Thumbkey / Typesplit / Dual / JSON layers as the product. Borrow Unexpected depth where it makes QWERTY, terminal, and multilingual Latin actually good. Do not import a second gesture language that fights large-key swipes.

"Must" means the engine should grow a first-class primitive (command, modifier, or overlay). "Should" is worth doing once Must is in. "Nice" is content, niche scripts, or Unexpected-specific UX we can skip or ship much later.

### Must

Without these, Unexpected-style layouts and the terminal layout stay incomplete, and European QWERTY is a bag of extra swipe letters instead of a system.

1. **Fn as an in-place modifier**, with a table (port or subset of `fn.json`). A separate `fn` layer is not a substitute.
2. **Dead-key accent modifiers** (at least the common Latin set: acute, grave, circumflex, tilde, diaeresis, cedilla, caron, ring, macron, ogonek, slash, breve). This is how Unexpected types `á` / `ç` / `ň` without putting every glyph on a swipe.
3. **Navigation keyevents:** `PAGE_UP`, `PAGE_DOWN`, `HOME`, `END`, `INSERT`. Needed for terminal / PC layouts and for Fn(arrows) if we take Fn.
4. **F1-F12** as commands (Fn+number in Unexpected). Terminal and shortcuts.
5. **Delete word / forward delete word** as keyboard-side editing (circle-on-backspace in Unexpected; also extra keys). Useful on S12 too if bound to a swipe or slide.
6. **Paste as plain text.** Small output primitive, high daily value.

Do **not** treat as Must: circle/anticircle/round-trip, extra-key injection, shared bottom row, dictionary, Hangul engine, automatic landscape split.

### Should

Power-user completeness and layout expressiveness. None of these are required to type German on S12, but they are the rest of Unexpected's "the layout is a programming language" feel.

1. **Compose key** plus a subset of Linux compose sequences (or generate from Unexpected's `srcs/compose/`). Overlaps dead keys; compose is the superset for `æ`, quotes, symbols.
2. **Macros** (ordered list of intents on one zone). Clipboard chords, `Ctrl+Backspace`, snippets with modifiers.
3. **Arbitrary `keyevent` codes** in JSON (media keys, app-specific codes). F-keys and navigation in Must should be named commands first; this is the escape hatch.
4. **Layout modmaps** beyond `caseMaps`: Shift / Fn / Ctrl remaps of key identity (Turkish `i` -> `İ`, Cyrillic Ctrl-V). Natural extension of `caseMaps`.
5. **Named whitespace:** `nbsp`, `nnbsp`, `zwj`, `zwnj` (and literal tab/newline vs Tab/Enter).
6. **Per-row height** (and maybe `indication` as a second legend string). Spacer already covers horizontal `shift`.
7. **Layout cycle both directions** (`switch_backward` equivalent). Forward exists as `SWITCH_LANGUAGE`.
8. **Portrait vs landscape last-used layout** (Unexpected remembers them separately). Fits Dual/Split being more useful in landscape.
9. **`label:` / `legend:key_def` completeness** if macros and keyevents land (quoted strings, `:` in values).
10. **Share selected text**, **IME prev/next**, **voice-typing chooser**, explicit **Caps Lock** key. Small commands, obvious bindings.

Greek/math can be a JSON layer with no engine work; do that as content when someone wants it, not as `switch_greekmath`.

### Nice to have

Unexpected flavor, script-specific engines, or features Suave already solved a different way.

- Circle, anticircle, round-trip. If ever: **opt-in per layout**, default off. On S12 they will steal swipes.
- `loc` placeholders + extra-key / locale injection UI. Suave already lets you edit every zone; injection is Unexpected's answer to not having a visual editor.
- Shared system bottom row and number-row injection. Conflicts with per-layout bottom rows (S12, Terminal, Unexpected JSON).
- `script` / `numpad_script` and non-ASCII numpads.
- Hangul Jamo composition; Hebrew Fn-placeholders; Arabic combining specials as named keys; RTL mirrored bracket legends.
- On-device dictionary, `complete_*` keys, typed-word tracker (beyond Android inline suggestions).
- Selection-mode modifier (auto sliders when text is selected).
- Automatic landscape XML-style row split (obsolete if Split/Dual stay).
- Foldable-specific height/margin prefs.
- XML import of Unexpected layouts (parser + lossy mapping). Only useful after Fn, dead keys, and named commands exist.
- Unexpected's unused context-menu keys (replace, autofill, assist).

### Explicit non-goals

Copying these would make Suave worse at being Suave:

- Circle gestures on by default.
- Auto-injecting extra keys or a global bottom row into every layout.
- Replacing Dual/Split/floating with Unexpected's landscape gap-inserter.
- Making numeric/emoji/clipboard global system keyboards instead of layers in the JSON document.
- Dropping empty-swipe-to-center to match Unexpected's "unassigned direction does nothing."

## Suggested order of work

Engine increments that unlock layout content, not a calendar.

1. Add named commands: Page/Home/End/Insert, F1-F12, delete-word, paste-plain. Wire `OutputExecutor`. Terminal and Unexpected JSON can bind them immediately.
2. Add `FN` as `ModifierId` plus a remap table applied in `ModifierEngine` (in-place, like Shift). Start with a small table (numbers -> F-keys, arrows -> Home/End/Page, common punctuation). Do not switch layer.
3. Add dead-key / accent modifiers (compose-pending state, then the next `Text` key). Enough for de/fr/es/cs layouts without putting every accented letter on a swipe.
4. Extend JSON: `caseMaps`-like Fn/Ctrl maps, then macros, then `keyevent`.
5. Only after 1-4: optional circle gestures, XML import, dictionary, script-specific composition.

## Appendix: Unexpected custom layouts

Unexpected already allows custom layouts **via XML alone**. Settings -> add layout -> Custom layout pastes XML. There is also a web editor that emits XML. There is no in-app visual grid editor like Suave's.

A minimal keyboard:

```xml
<?xml version="1.0" encoding="utf-8"?>
<keyboard name="Simple example" script="latin">
    <row>
        <key c="a" />
        <key c="b" />
    </row>
</keyboard>
```

Compass swipes: `n`, `ne`, `e`, `se`, `s`, `sw`, `w`, `nw` (or `key0`..`key8`). `anticircle` is extra. `loc accent_aigu` reserves a slot for extra keys. `<modmap>` remaps Shift/Fn/Ctrl. `bottom_row="false"` if you draw your own last row (required for a Thumbkey-like XML).

Built-in US QWERTY (`latn_qwerty_us.xml`) is 3 rows + injected bottom row, 10 columns, numbers on `ne`, symbols on other corners, `loc` slots for Esc/Tab/Caps/accents. Suave `unexpected.json` copies the grid and corners, then uses layers for numeric/emoji/fn instead of Fn-in-place, compose, extra keys, and the shared bottom row.
