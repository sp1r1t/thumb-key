# Swipe zone inference

Design note: how a key's swipe geometry should be derived from the intents
assigned to it, instead of an explicit `swipeType` / `SwipeDirections` config.

## Problem

ThumbKey-style layouts carry a separate swipe model per key
(`EIGHT_WAY`, `FOUR_WAY_CROSS`, `FOUR_WAY_DIAGONAL`, `TWO_WAY_*`). The new
gesture engine still carries a reduced form (`NONE` / `FOUR_WAY` / `EIGHT_WAY`)
on `GestureConfig`.

That field is a second source of truth. Which zones have intents already
implies the geometry. Named swipe modes (`four-way cross`, `four-way
diagonal`, and so on) are just particular occupancy patterns - keeping them
as config only adds noise and drift.

## Goal

Drop the explicit swipe-model config. Infer the circle partition from which
of the eight directional zones are occupied. Center stays a distance
threshold (not a compass wedge): if the finger does not move far enough,
the press resolves to center regardless of where on the key face it started.

**Position on the key face does not matter.** Tapping the upper-left corner
of a key with no movement is still a center tap. Only the movement vector
after press selects a directional zone.

## Geometry model

Every key has eight possible swipe directions, in circle order:

`U`, `UR`, `R`, `DR`, `D`, `DL`, `L`, `UL`

1. Each occupied direction owns **45°** by default.
2. If a direction is missing, its 45° is split **50:50** to its two
   neighbours (**22.5°** each).
3. **Propagation is one hop only.** A share is never forwarded again when
   the neighbour is also missing.
4. Therefore each direction can gain at most **+22.5° from each side**
   (**+45°** total), so an occupied direction is never larger than **90°**.

If a neighbour is missing, the 22.5° that would have gone to it stays
**unclaimed**.

### Unclaimed angle

Unclaimed angle means:

- No swipe lock
- No swipe-lock haptic (even when swipe vibration is enabled)
- On release, resolve as **center**

### Examples (consequences of the rule, not named modes)

All eight occupied: each keeps 45°; nothing to split.

Only U, R, D, L occupied: each missing corner gives 22.5° to each adjacent
cardinal -> each cardinal ends at 90°. The old "four-way cross" shape, with
no special case in the engine.

Only UR, DR, DL, UL occupied: same rule on the other phase -> each diagonal
ends at 90°.

Only L and R occupied:

- Missing `DL`: 22.5° -> L, 22.5° -> D (unclaimed)
- Missing `UL`: 22.5° -> L, 22.5° -> U (unclaimed)
- Same mirror for R
- L = 90°, R = 90°; pure U and D remain unclaimed -> center
- Not two 180° half-planes, because propagation does not chain

## Worked example: DL, D, DR missing

L and R are occupied; the three down-side directions are empty.

- `DL` missing: 22.5° -> L, 22.5° -> D (unclaimed)
- `DR` missing: 22.5° -> R, 22.5° -> D (unclaimed)
- `D` missing: 22.5° -> DL (unclaimed), 22.5° -> DR (unclaimed)

| Swipe | Lock? | Haptic | Result on release |
|---|---|---|---|
| Into L's 22.5° share of former DL | yes -> L | swipe-lock vibrate (if enabled) | L action |
| Straight down (unclaimed) | no | none | center action |
| Into R's 22.5° share of former DR | yes -> R | swipe-lock vibrate (if enabled) | R action |

## Feedback

- Press / activation feedback follows existing toggles.
- `SwipeLocked` (mid-drag direction lock) vibrates only when a directional
  zone actually locks.
- An unclaimed-angle swipe that falls through to center does not fire
  swipe-lock feedback.

## Implications for the engine

- `KeyMapping.intents` (which `Zone`s are present) is enough to compute the
  partition.
- `SwipeNWay` on legacy `KeyItemC` and `SwipeDirections` on `GestureConfig`
  become redundant for layout definition and can be removed or ignored once
  the recognizer implements this inference.
- The recognizer needs: occupied directional set -> wedge boundaries (45°
  base, one-hop 22.5° shares from missing neighbours, cap 90°), then
  angle-to-zone lookup, with unclaimed angle mapping to center at commit time
  (and no lock event while dragging through it).

## Status

Agreed design direction. Not yet implemented in the gesture recognizer.
