# Progress

## Done

- Full 1.8 engine (maze, physics, kits, pads, monsters, timer, center det)
- **Playable game** with camera follow + zoom
- **HeuristicAgent** on the shared Action API (`F` in-game, or `--ai` / menu checkbox)
- **Procedural SFX** (hit, pad, ability, stage, death) — mute with `M`
- **Mode-default mobs**: ORIGINAL/SPEED = 150, MODERN = 225 (override optional)
- Clearer path checkerboard, pad borders + beacon, player facing line

## Run

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.play.PlayMenu
java -cp target/classes me.monstermaze.engine.play.PlayFrame --kit JUMPER --mode ORIGINAL
java -cp target/classes me.monstermaze.engine.cli.AiMain 6000 JUMPER ORIGINAL ai-out
```

## Controls

WASD move · arrows turn · Space jump · Shift sprint · Q primary · E enhanced  
`+`/`-` zoom · `0` reset zoom · `F` AI · `M` mute
