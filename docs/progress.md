# Progress

## Done

- Engine core (maze, physics, monsters, pads, kits, timer)
- Trace harness + top-down PNG renderer
- **Playable local game**
  - `PlayMenu` kit/mode/layout select
  - `PlayFrame` 20 TPS Swing window + HUD
  - Controls: WASD move, arrows turn, Space jump, Shift sprint, Q primary, E enhanced
- **Center deterioration** disables graph cells as the stage progresses

## Run the game

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.play.PlayMenu
# or directly:
java -cp target/classes me.monstermaze.engine.play.PlayFrame --kit JUMPER --monsters 40
```

## Next ideas

- Camera follow / zoom on player
- Sound/juice
- AI agent loop using the same Action API
- Live-plugin CSV exporter for TraceComparer
