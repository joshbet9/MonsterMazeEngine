# MonsterMazeEngine

Pure, headless, **1.8-accurate** simulation of Mineplex Monster Maze.

- No Bukkit / NMS / Minecraft dependencies
- Deterministic tick-based engine
- Shared state & action API for AI training and future adapters
- Starts as a faithful reflection of the 1.8 plugin; used to train and adjust logic that also runs on the real servers

## Goal

This repository is the **standalone game / simulation core**.

The real Minecraft 1.8 plugin (in the separate MonsterMaze repo) remains the reference implementation.  
This engine must match its mechanics so that AI logic trained here transfers cleanly.

## Status

Initial scaffold. API and package layout are in place. Mechanics implementation is next.

## Structure

```
src/main/java/me/monstermaze/engine/
├── api/          # GameState, Action, TickResult, MonsterMazeEngine
├── maze/         # layouts, graph, coordinates
├── physics/      # 1.8 movement, knockback, collision
├── monster/      # random-walk simulation
├── kit/          # Jumper, Slowball, Body Builder, Repulsor, Maverick
├── pad/          # Safe Pad logic
├── game/         # stage timer, spawner, EngineImpl
└── util/
```

See `docs/` for the full mechanics inventory, state/action contract, and validation priorities.

## Building

```bash
mvn -q test
```

Java 17+ recommended (Java 8 compatible API surface is intentional for easier interop).

## License

See LICENSE.
