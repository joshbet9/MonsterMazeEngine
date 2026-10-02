# Progress

## Done

- Full 1.8 engine + kits + center deterioration
- Playable game (camera, zoom, SFX, mode-default mobs)
- **Safe Pad placement** closer to plugin: avoid prior pads by 40 blocks, disable +/-2 waypoint cells, clear mobs on new pad
- **BFS Pathfinder** + AI walks maze graph toward pad
- **Minimap** (top-right): paths, pad, player
- **End-of-run dialog**: peak stage, time, mob count

## Run

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.play.PlayMenu
java -cp target/classes me.monstermaze.engine.play.PlayFrame --ai --kit JUMPER
java -cp target/classes me.monstermaze.engine.cli.AiMain 8000 JUMPER ORIGINAL
```
