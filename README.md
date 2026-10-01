# MonsterMazeEngine

Pure, headless, **1.8-accurate** simulation of Mineplex Monster Maze.

- No Bukkit / NMS / Minecraft dependencies
- Deterministic tick-based engine
- Shared state & action API for AI training
- All kits, Safe Pads, monsters, stage timer
- CSV trace harness + top-down PNG renderer

## Quick start

```bash
mvn -q test
mvn -q package
java -cp target/classes me.monstermaze.engine.cli.SimMain 400 sim-out
```

## Structure

```
api/      GameState, Action, TickResult, MonsterMazeEngine
maze/     layouts, graph, coordinates
physics/  1.8 movement, knockback
monster/  random-walk
kit/      Jumper, Slowball, Body Builder, Repulsor, Maverick
pad/      Safe Pad
game/     EngineImpl, StageTimer
trace/    TraceRecorder, TraceComparer
render/   TopDownRenderer
cli/      SimMain
```

## Kits

| Kit | Primary | Enhanced (Speed/Modern) |
|-----|---------|-------------------------|
| Jumper | Charged jumps | Pad restores charges |
| Slowball | Snowballs (max 16, +1/2s) | Cryo Blitz freeze 6 blocks |
| Body Builder | +max HP on first pad | Body Rush deflect |
| Repulsor | Launch monsters (3 charges, 6 blocks) | — |
| Maverick | (enhanced only) KB toward pad | — |

See `docs/` for mechanics inventory and validation notes.

## License

MIT — see LICENSE.
