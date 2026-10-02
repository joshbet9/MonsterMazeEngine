# MonsterMazeEngine

Pure **1.8-accurate** Monster Maze sim + local playable game.

## Quick start (test)

```bash
git clone https://github.com/joshbet9/MonsterMazeEngine.git
cd MonsterMazeEngine
mvn -q package
java -cp target/classes me.monstermaze.engine.play.MainMenu
```

Or: `java -jar target/monstermaze-engine-0.1.0-SNAPSHOT.jar`

Needs **Java 17+** and a display (Swing).

### What to try

1. **Main menu** — dark theme by default · Play / Settings / Quit
2. **Settings** — kit, mode, layout, mobs, seed, zoom, window size, **Dark mode**, SFX, controls overlay
3. **Play** — reach the green Safe Pad before the timer; avoid red mobs
4. **In-game**
   - WASD move · arrows turn · Space jump · Shift sprint · Q/E abilities
   - `P` pause · `Esc` pause then menu · `H` toggle help · `M` mute · `+/-` zoom
   - Minimap: green = active pad, yellow = preview, blue = you
   - HP bar turns red when low

Config is saved to `~/.monstermaze-engine.properties`.

## License

MIT
