# Suave

A fully customizable, free and open-source keyboard with T9 poweruser soul.

Customizable. Built for [touch-typing](https://en.wikipedia.org/wiki/Touch_typing) on a phone.
Serious about hacker input (modifiers, terminals, RAW IME). Libre under AGPL-3.0.

## The engine

Suave is first an **engine**: layouts are data (keys, swipe zones, intents), and typing goes
through a dedicated gesture recognizer and dispatcher. That is what makes the keyboard
customizable without baking one alphabet into the code path.

The idea is that you can build **any** layout on that engine - not only letter boards. A classic
T9-style pad. A full [Hacker Keyboard](https://github.com/klausw/hackerskeyboard) clone (planned).
Even a silly Copy/Paste-only board, if that is what you need today. S12 is one layout; the point
of Suave is that it is not the only shape the product can take.

What the engine is for:

- **Any board you invent** - letter grids, hacker/meta boards, utility pads, experiments. If it
  is keys and gestures, it should be expressible as layout data.
- **Predictable geometry** - large keys in known places, directional swipes for the rest, so you
  can keep your eyes on the text instead of
  [hunting and pecking](https://www.keyboardingonline.com/hunting-and-pecking/).
- **Stable maps** - layers should not wildly reshuffle the board; muscle memory needs a map that
  stays put.
- **Real key events when it matters** - meta keys and **RAW IME** for terminal emulators and other
  apps that expect Ctrl, Alt, Esc, and friends - not only committed text.
- **Local by default** - accuracy from geometry and practice, not from shipping keystrokes to a
  cloud predictor.

How layouts are authored, shared, and shipped is still evolving. The product bet is the engine;
layouts plug into it.

## S12 and the Suave layout

On top of that engine sits **S12**, the default Suave layout: a split board with a shared middle
column, tuned for two alternating thumbs.

Design ideas behind S12:

- **[Dvorak](https://en.wikipedia.org/wiki/Dvorak_keyboard_layout) alternation** - sequences that
  bounce between sides map cleanly to left thumb / right thumb.
- **[Workman](https://workmanlayout.org/)** - same-hand *chords* are a poor fit when each "hand"
  is a single thumb; S12 prefers clear side ownership and alternation.
- **Shared middle column** - better reach and thumb coordination on a phone than a cramped single
  grid or a miniature desk QWERTY.

S12 is the layout you get out of the box. It is not the whole product - the engine is.

## Why this exists

Full-size keyboard layouts were designed for ten fingers on a desk. Phones leave you with one or
two thumbs. [QWERTY](https://en.wikipedia.org/wiki/QWERTY) is not even a good design for those ten
fingers, yet soft keyboards usually shrink it onto a phone and paper over the mess with denser
keys and aggressive prediction. Suave takes the other path: fewer, larger keys, swipes, and an
engine you can shape.

## Lineage

Ideas Suave stands on. None of these is Suave itself - they are the shoulders nearby.

### MessagEase

[MessagEase](https://www.exideas.com/ME/) (Exideas) brought a large 9-key grid to Android soft
keyboards - the same form factor people already knew from feature-phone [T9](https://en.wikipedia.org/wiki/T9_(predictive_text))
pads, but with researched swipe gestures instead of big-data prediction. That was a real
innovation on Android: reclaim the phone-sized keybed, keep taps coarse and learnable, and
still aim for high words-per-minute. [Their design paper](https://www.exideas.com/ME/ICMI2003Paper.pdf)
is worth reading. The original app is closed-source and effectively unmaintained; the idea is
not.

### Thumb-Key

[Thumb-Key](https://github.com/dessalines/thumb-key) kept the large-key, swipe-first,
privacy-conscious approach alive as libre software, and showed how a **split** phone layout can
share a middle column between two thumbs. S12 continues that split idea.

### Unexpected Keyboard

[Unexpected Keyboard](https://github.com/Julow/Unexpected-Keyboard) is where directional swipes
first clicked as a daily typing habit: characters live on the corners of keys, and you slide
toward them instead of hunting a denser grid. That made it possible to skip layer switches and
still keep symbols in reach - one stable board, more of the alphabet and punctuation under the
same thumbs. That gesture language feeds how Suave thinks about zones and motion. Unexpected
also lent Suave its haptic feedback feel - short, useful confirmation when a gesture lands, not
decorative buzz.

### Hacker Keyboard

[Hacker Keyboard](https://github.com/klausw/hackerskeyboard) treated the soft keyboard like a
real computer input device: proper modifier / meta keys, and sober behavior in terminal
emulators. That is the bar Suave aims to clear and raise for RAW IME and meta keys.

## Using it

1. Install a build (or a release when you publish one).
2. Enable Suave in system keyboard settings, then select it as the active input method.
3. Tap or swipe to type. Open the Suave app for appearance, behavior, clipboard, and the rest.

For contributors and build steps, see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

GNU Affero General Public License v3.0. See [LICENSE](LICENSE).

Credit for MessagEase, Thumb-Key, Unexpected Keyboard, and Hacker Keyboard lives on the About
screen and in the lineage notes above.
