# Mechanics Inventory (1.8 reference)

Source of truth for what this engine must match. Derived from MonsterMaze `1.8/MonsterMazeStandalone` and MonsterMazeAI mechanics docs.

## Maze

- Fixed 99×99 layouts (three patterns).
- Cell types: empty / path / spawn / center / barrier combinations.
- World mapping: `worldX = centerX - 49 + row`, same for Z; path entities at block centres.
- Dynamic graph: raw topology vs current traversability (Safe Pads, center deterioration, disabled waypoints).

## Timing & Progression

- Modes: Original / Speed / Modern (different starting timers and monster spawn rates).
- Phase timer starts high (60s or 35s) and decreases toward 15s.
- First arrival can shorten remaining time; all alive players on pad can force it to 4s.
- Preview pad appears ~2s before transition.
- Center deterioration starts 20s after live start (11 decay steps).
- Monster population increases on each transition (+15 or +30 depending on mode).

## Monsters

- Random-walk on the path graph (not player-seeking).
- At waypoints (within ~0.4 blocks): choose random legal cardinal neighbour, prefer not to U-turn if alternatives exist.
- Waypoint decisions throttled (~10 Hz); movement is still tick-driven.
- Speed: base × mode multiplier (documented as 1.4× factor in current 1.8 code).
- Only one monster applies knockback per contact event.

## Collision / Knockback

- Contact: 4 HP damage, 1s player hit cooldown.
- Launch: normalized horizontal direction (monster → player), strength 1.0, vertical +0.75, vertical cap 1.2, grounded bonus +0.2.
- Safe-pad players are immune.
- Knockback is a usable movement primitive (direction of approach matters).

## Player Physics (1.8-specific)

- Legacy jump effect (amplifier –10) for non-Jumpers / exhausted Jumpers.
- “Speeding” via jump spam while running is an emergent 1.8 behaviour that must be modelled tick-by-tick.
- Jumper charges: 5 (Original) or 3 (enhanced modes); 750 ms gate; pad restore in enhanced modes; pad jumps free in enhanced.

## Kits

| Kit | Core | Enhanced / QoL notes |
|-----|------|----------------------|
| Jumper | Charged jumps | Charges restored on pad |
| Slowball | Snowballs (max 16, +1/2s), slow players | Cryo Blitz (freeze monsters) |
| Body Builder | +max HP on first-to-pad (cap 30 HP / 15 hearts) | Body Rush (deflect + launch monsters) |
| Repulsor | 3 charges, 6-block radius, launch monsters | — |
| Maverick | (enhanced only) monster hits redirect toward Safe Pad | — |

## Safe Pads

- 5×5 surface (beacon at centre), `isOn` uses ±2.5 block horizontal + height check.
- Decay visual + eventual destroy (11 steps).
- Immunity while on pad.

## Game Flow

- States: IDLE → STARTING → LIVE → ENDING.
- Objective for solo/AI: highest Safe Pad / stage reached (time still matters because faster stages = fewer monsters).
