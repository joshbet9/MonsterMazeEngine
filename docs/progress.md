# Progress

## Done

- API, maze layouts, coordinates, graph
- Physics, knockback, monsters, Safe Pads, stage timer
- **All kits**: Jumper, Slowball (+Cryo), Body Builder (+Body Rush), Repulsor, Maverick
- **Trace harness**: `TraceRecorder` / `TraceComparer` (CSV) for pure-vs-live diffs
- **Top-down renderer**: `TopDownRenderer` PNG export
- **CLI**: `SimMain` writes frames + trace.csv

## How to run a sim

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.cli.SimMain 400 sim-out
# -> sim-out/final.png, frame_*.png, trace.csv
```

## Trace validation workflow

1. Export a CSV from the live 1.8 plugin with the same columns as `TraceFrame.csvHeader()`
2. Replay actions in the pure engine (or compare engine-generated CSV)
3. `TraceComparer.compare(engineFrames, liveFrames)`

## Known approximations

- Speeding multiplier and exact jump-spam timing
- CreatureMoveFast vs step-toward-target
- Safe Pad placement heuristic
- Snowball only consumes charge in solo (no other players to slow)
