# MonsterMazeEngine

Pure, headless **1.8-accurate** Monster Maze sim + local playable game.

- No Bukkit / NMS
- Deterministic 20 TPS engine
- Kits: Jumper, Body Builder, Slowballer, Repulsor, Maverick
- Swing play window + CSV traces + PNG renderer

## Play

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.play.PlayMenu
```

Or skip the menu:

```bash
java -cp target/classes me.monstermaze.engine.play.PlayFrame \
  --kit JUMPER --mode ORIGINAL --layout 0 --monsters 40
```

**Controls:** WASD move · arrows turn · Space jump · Shift sprint · Q primary · E enhanced

## Headless sim / traces

```bash
java -cp target/classes me.monstermaze.engine.cli.SimMain 400 sim-out
```

## Structure

```
api/ game/ maze/ physics/ monster/ kit/ pad/
trace/ render/ play/ cli/
```

## License

MIT
