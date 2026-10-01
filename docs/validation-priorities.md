# Validation Priorities

Highest-risk 1.8 pieces that need measurement / differential testing against the live plugin before the pure engine can be trusted for AI training.

## Priority 1 — Legacy 1.8 movement / “speeding”

- Exact effect of running + rapid Space presses (tick-by-tick position or blocks/s).
- Difference between hold vs rapid press.
- Whether a one-block gap can be crossed and under what timing.
- Interaction with the –10 Jump potion amplifier.
- Grounded detection and when a jump charge can actually be consumed.

## Priority 2 — Knockback trajectories

- Exact post-hit velocity (horizontal strength 1.0, vertical +0.75, cap 1.2, grounded +0.2) under different approach angles.
- How much air control / steering remains during knockback.
- Behaviour near walls, corners, and gaps.
- Confirmation that only one monster applies knockback at a time.
- Maverick redirect: exact target (pad centre?) and resulting trajectory.

## Priority 3 — Safe-pad timer rules

- Precise shortening formula when the first player arrives.
- Effect of all alive players standing on the pad (force to 4 s).
- Exact preview timing (~2 s).
- Whether leaving the pad after touching it still counts for “first arrival” benefits.
- Center deterioration start (20 s after live) and the 11 decay steps.

## Priority 4 — Monster random-walk details

- Exact distance threshold for “reached waypoint” (docs say ~0.4).
- U-turn avoidance rule when multiple options exist.
- Decision rate (10 Hz) vs movement tick rate.
- Actual movement speed (1.4 × mode multiplier) in blocks per tick.
- Independence of each monster’s RNG.

## Priority 5 — Kit resource timing

- Jumper 750 ms charge gate and pad restore behaviour.
- Slowball regen (exactly +1 every 2 s, max 16).
- Repulsor / Body Rush launch duration and “cannot hit while launched”.
- Cryo Blitz radius, freeze duration, and thaw behaviour.

## Priority 6 — Coordinate & collision geometry

- Final confirmation that the three layouts + `center ± 49` mapping match the live world.
- Practical contact radius / hitbox used for monster–player collision.
