# State & Action API

All types live under `me.monstermaze.engine.api`.

## Engine entry point

```java
public interface MonsterMazeEngine {
    GameState initialState(MazeMode mode, int layoutId, KitType kit, long seed);
    TickResult tick(GameState state, Action action);
}
```

## GameState (immutable snapshot)

- `tick`, `mode`, `phase`, `stage`
- `phaseTimerTicks` / `phaseTimerMax`
- `centerDeteriorationStep`
- `maze` — layoutId, raw[][], traversable[][]
- `player` — pos, vel, yaw/pitch, onGround, health, kit, jump charges, ability resources, hit cooldown, onSafePad
- `monsters` — id, pos, vel, waypoint, direction, launched, frozenTicks
- `activePad` / `previewPad` — center, surfaceY, decayStep, active, isPreview

## Action (per-tick intent)

- `moveX`, `moveZ` — horizontal intent
- `sprint`, `jump`, `holdJump`
- `yaw`, `pitch`
- `usePrimary`, `useEnhanced`
- `approachMonsterId` — optional deliberate collision target

## TickResult

- `next` — resulting GameState
- `events` — list of GameEvent (DAMAGE, KNOCKBACK, PAD_REACHED, …)
- `terminal` — true when the run ends for this player

See the Java sources for full field lists and constructors.
