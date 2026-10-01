# Progress

## Done

- Repo scaffold, API types, stub engine
- **Maze module**
  - Three 99×99 layouts extracted from MonsterMaze 1.8 `MazeLayouts` (gzip+base64 in `MazeData1/2/3`)
  - `Layouts` unpack/parse helpers (`isRawPath`, `isSpawn`, `isCenter`, `isBarrier`, `copy`)
  - `Coordinates` — `center ± 49` mapping, path centres
  - `MazeGraph` — raw + traversable grid, spawn cells, cardinal neighbours
  - `EngineImpl.initialState` loads a real layout, places player on nearest spawn cell, sets Jumper charges / phase timer
  - Smoke tests for layout load + tick advance

## Next

1. 1.8 player physics + knockback (highest validation risk)
2. Safe Pad + stage timer logic
3. Monster random-walk
4. Jumper-only vertical slice playable in sim
