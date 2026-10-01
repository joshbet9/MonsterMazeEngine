# Progress

## Done

- Repo scaffold, API types
- Maze module (3 layouts, coordinates, graph)
- **Jumper vertical slice**
  - `PlayerPhysics18` - walk/sprint, jump, gravity, Jumper charges (15-tick gate), approximate speeding
  - `Knockback` - UtilAction port: str 1.0, yAdd 0.75, yMax 1.2, ground +0.2, 4 HP, 20-tick cooldown
  - `MonsterSimulator` - random-walk, 0.4 waypoint, U-turn avoid, 1.4 speed, launch/freeze stubs
  - `SafePadSimulator` - 5x5 isOn (+/-2.5), spawn heuristic
  - `StageTimer` - 60/35->15s, first-arrival shorten, +15/+30 monsters, center det clock
  - `EngineImpl` wires all of the above; `new EngineImpl(n)` for lighter sims
  - Integration tests in `EngineSliceTest`

## Known approximations (validate vs live 1.8)

- Speeding multiplier (1.5x) and exact jump-spam timing
- Air control coefficients
- Monster CreatureMoveFast pathing vs pure step-toward-target
- Safe Pad placement (heuristic, not full plugin pad list)

## Next

- Other kits (Slowball, Body Builder, Repulsor, Maverick)
- Trace-replay validation harness against live plugin
- Optional top-down renderer
