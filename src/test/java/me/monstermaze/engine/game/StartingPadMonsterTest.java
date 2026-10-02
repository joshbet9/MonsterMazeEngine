package me.monstermaze.engine.game;

import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.MonsterState;
import me.monstermaze.engine.pad.SafePadSimulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class StartingPadMonsterTest {
    @Test
    void starterMonstersCannotSpawnOnActiveStartingPad() {
        EngineImpl engine = new EngineImpl(225);
        GameState state = engine.initialState(MazeMode.SPEED, 0, KitType.JUMPER, 4242L);

        for (int i = 0; i < 10; i++) {
            state = engine.tick(state, Action.noop()).next;
        }

        for (MonsterState monster : state.monsters) {
            assertFalse(
                    SafePadSimulator.isOn(state.activePad, monster.pos),
                    "starter monster spawned on the active starting Safe Pad");
        }
    }
}
