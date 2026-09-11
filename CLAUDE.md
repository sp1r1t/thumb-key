# Suave (S12)

## UI Principles

1. **Never show a settings control for a feature the current engine doesn't act on.** Delete it
   from the UI the moment it goes dead, rather than leaving an inert toggle around - a user has
   no way to tell a live setting from a dead one just by looking. The underlying DB field can
   stay dormant (no migration needed to remove a control from the UI); the control itself cannot
   stay visible.

2. **A setting's description always states its current value/effect inline, dynamically** -
   never a fixed sentence that doesn't say what will actually happen. For a switch: a full
   sentence starting "On - ..." / "Off - ...". For a stepper or dropdown: the current value
   folded into a descriptive sentence ("Currently 64dp - ..."), not into the title. This is what
   actually removes clutter: no separate value display, no mentally cross-referencing a toggle's
   position against generic help text.

3. **Deeper rationale that doesn't fit in one line goes in a compact "i" in the setting
   title** (not a trailing row control), opening a bottom sheet. Judged per-setting rather
   than added reflexively to every row - simple settings (a plain on/off with an obvious
   effect) don't need one; settings with real nuance (an unusual interaction, a caveat, a
   "why this exists") do.

4. **Every non-boolean setting (stepper, dropdown) gets an explicit reset-to-default action** next
   to it, since there's otherwise no way to recover the original value once it's been changed.

Shared implementation: `SettingTitle` (in `SettingRow.kt`) puts the optional info "i" in the
title; `SettingRow` wraps a preference with the reset button. Integer quantities use
`IntStepperPreference` (tap for one step, hold to repeat), not sliders. Named discrete
choices use a dropdown.
