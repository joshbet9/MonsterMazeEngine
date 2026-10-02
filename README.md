# MonsterMazeEngine

Pure **1.8-accurate** Monster Maze sim + local playable game.

- No Bukkit / NMS · deterministic 20 TPS
- Kits: Jumper, Body Builder, Slowballer, Repulsor, Maverick
- Main menu + settings GUI (persistent config)

## Play

```bash
mvn -q package
java -cp target/classes me.monstermaze.engine.play.MainMenu
# or:
java -jar target/monstermaze-engine-0.1.0-SNAPSHOT.jar
```

**Main menu:** Play · Settings · Quit  
**Settings:** kit, mode, layout, mobs, seed, zoom, window size, SFX  
Config file: `~/.monstermaze-engine.properties`

**In-game:** WASD · arrows · Space · Shift · Q/E · `+/-` zoom · `M` mute · **Esc** back to menu

## Headless sim

```bash
java -cp target/classes me.monstermaze.engine.cli.SimMain 400 sim-out
```

## License

MIT
