# Progress

## Architecture

**MonsterMazeEngine** is the single pure simulation core:

- Minecraft plugin (later adapter) and this local game both share the same mechanics
- **AI is not a separate game mode** — training (MonsterMazeAI) will call `MonsterMazeEngine.tick(state, action)` with its own policies
- The play UI is **human-only** (WASD, kits, pads, stages)

Optional `HeuristicAgent` / `AiMain` remain as headless smoke tools for the Action API only.

## Done

- 1.8-faithful engine (maze, physics, kits, pads, monsters, timer, center det)
- Playable local game (camera, zoom, minimap, SFX, mode-default mobs)
- Safe Pad placement (avoid 40 blocks, disable pad waypoints)
- Pathfinder utility (usable by external AI)
- Trace CSV harness for pure-vs-live diffs

## Run (player)

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.play.PlayMenu
```
