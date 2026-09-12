# Legacy ThumbKey keyboard stack

Orphaned pre-rewrite keyboard UI, layout definitions, and helpers. The live IME
mounts `ui.engine.EngineKeyboardScreen` only.

```
legacy/
  ui/           KeyboardScreen, KeyboardKey
  keyboards/    DETypeSplitSuave, CommonKeys
  utils/        KeyItemC / KeyAction / SwipeNWay Types, performKeyAction, swipeDirection, ...
  KeyboardLayout.kt
  KeyboardModificationService.kt
```

Live shared leftovers stay in top-level `utils/` (theme enums, clipboard store,
`toBool`, settings chrome helpers). `IMEService` still reads
`legacy.utils.KeyboardDefinition` for text-processor settings.
