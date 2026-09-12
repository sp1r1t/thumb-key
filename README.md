# Suave

A fully customizable, free and open-source keyboard with T9 soul.

Suave grew out of [Thumb-Key](https://github.com/dessalines/thumb-key) and now ships its own
keyboard engine. The product idea is older than either app: large, predictable keys you can
learn by feel, so your eyes stay on the text.

## Why this exists

Full-size keyboard layouts were designed for ten fingers on a desk. Phones leave you with one or two
thumbs. [QWERTY](https://en.wikipedia.org/wiki/QWERTY) is not even a good design for those ten
fingers: it is a historical accident that soft keyboards still shrink onto a phone. Layouts like
[Dvorak](https://en.wikipedia.org/wiki/Dvorak_keyboard_layout) and
[Workman](https://workmanlayout.org/) exist because people kept trying to fix what QWERTY gets
wrong for real hands (S12 borrows from both, later). Most phone keyboards then compensate with
denser keys and aggressive word prediction, often by shipping keystrokes to a server. That is the
opposite of how good physical typing works: fixed positions, muscle memory, and
[touch-typing](https://en.wikipedia.org/wiki/Touch_typing) with your eyes on the words, not
[hunting and pecking](https://www.keyboardingonline.com/hunting-and-pecking/) for the next letter.

Suave (and Thumb-Key before it) take the other path: fewer, larger keys in known places, with
directional swipes for the less common characters. Accuracy comes from geometry and practice,
not from a model that has to see what you type.

## Lineage

### MessagEase

[MessagEase](https://www.exideas.com/ME/) (Exideas) brought a large 9-key grid to Android soft
keyboards - the same form factor people already knew from feature-phone [T9](https://en.wikipedia.org/wiki/T9_(predictive_text))
pads, but with researched swipe gestures instead of big-data prediction. That was a real
innovation on Android: reclaim the phone-sized keybed, keep taps coarse and learnable, and
still aim for high words-per-minute. [Their design paper](https://www.exideas.com/ME/ICMI2003Paper.pdf)
is worth reading. The original app is closed-source and effectively unmaintained; the idea is
not.

### Thumb-Key

[Thumb-Key](https://github.com/dessalines/thumb-key) revived that approach as libre software
(AGPL): a privacy-conscious 3x3 hub with swipes, letter placement from English frequency, and
explicit left/right thumb roles (consonants one side, vowels the other) so digrams naturally
alternate.

Thumb-Key also introduced **split** layouts for phones held in both hands: two thumb beds that
**share a common column in the middle**. That matches how a phone is actually held. Each thumb
owns a side, the center column is reachable by either, and you get better key placement and
thumb coordination than a single cramped grid or a tiny full QWERTY. Split typing is what Suave
carries forward as **S12**.

### Unexpected Keyboard

[Unexpected Keyboard](https://github.com/Julow/Unexpected-Keyboard) is where directional swipes
first clicked as a daily typing habit: characters live on the corners of keys, and you slide
toward them instead of hunting a denser grid. That made it possible to skip layer switches and
still keep symbols in reach - one stable board, more of the alphabet and punctuation under the
same thumbs. That gesture language feeds directly into how Suave thinks about zones and motion.
Unexpected also lent Suave its haptic feedback feel - short, useful confirmation when a gesture
lands, not decorative buzz.

### Hacker Keyboard

[Hacker Keyboard](https://github.com/klausw/hackerskeyboard) treated the soft keyboard like a
real computer input device: proper modifier / meta keys, and sober behavior in terminal
emulators. Suave leans hard into that lineage. Meta keys and **RAW IME** handling for terminal
environments are first-class concerns here, not afterthoughts - Suave aims to improve on what
Hacker Keyboard made possible when you need Ctrl, Alt, Esc, and friends to reach apps that
expect key events rather than committed text.

### Suave and S12

Suave is that split idea as a first-class product, with S12 as the default layout and a rewritten
engine underneath (layouts as data, dedicated gesture recognition, not the old per-key Compose
stack). It sits at the intersection of MessagEase / Thumb-Key geometry, Unexpected's swipe
habits and haptics, and Hacker Keyboard's seriousness about modifiers and terminals.

S12 is designed from principles learned from desktop layouts that already cared about hand
roles, and from what soft keyboards usually get wrong:

- **[Dvorak](https://en.wikipedia.org/wiki/Dvorak_keyboard_layout) alternation** - favor
  sequences that bounce between sides. On a phone that maps cleanly to left thumb / right thumb.
  Alternation is a strong fit for two-thumb typing.
- **[Workman](https://workmanlayout.org/)** - Workman optimizes for *same-hand chords*: letter
  runs struck in one motion by one hand. That is a poor fit when each "hand" is a single thumb.
  S12 does not chase same-side runs; it prefers clear side ownership and alternation.
- **Visual stability** - many software keyboards wildly reshuffle the board between letter,
  symbol, and number layers. That is a cognitive mess: muscle memory never settles when the
  map keeps moving. S12 keeps geometry stable across layers so your thumbs can trust where
  things live.

The shared middle column is the structural trick: it improves reach, keeps the layout balanced
on a phone's form factor, and gives both thumbs a place to meet without collapsing back into a
miniature desk keyboard.

## Using it

1. Install a build (or a release when you publish one).
2. Enable Suave in system keyboard settings, then select it as the active input method.
3. Tap or swipe to type. Open the Suave app for appearance, behavior, clipboard, and the rest.

For contributors and build steps, see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

GNU Affero General Public License v3.0. See [LICENSE](LICENSE).

Suave includes history from Thumb-Key (AGPL-3.0). Credit for upstream Thumb-Key remains on the
About screen. MessagEase, Unexpected Keyboard, and Hacker Keyboard remain important inspirations
for the large-key, swipe-first, terminal-capable phone keyboard.
