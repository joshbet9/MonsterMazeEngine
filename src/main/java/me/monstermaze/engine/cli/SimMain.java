package me.monstermaze.engine.cli;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.render.TopDownRenderer;
import me.monstermaze.engine.trace.TraceRecorder;

import java.nio.file.Path;

/**
 * Minimal CLI: run a short Jumper sim, write PNG + CSV trace.
 *
 * Usage: java ... SimMain [ticks] [outDir]
 */
public final class SimMain {
    public static void main(String[] args) throws Exception {
        int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 400;
        Path out = Path.of(args.length > 1 ? args[1] : "sim-out");
        java.nio.file.Files.createDirectories(out);

        MonsterMazeEngine engine = new EngineImpl(25);
        GameState state = engine.initialState(MazeMode.ORIGINAL, 0, KitType.JUMPER, 42L);
        TraceRecorder recorder = new TraceRecorder();
        TopDownRenderer renderer = new TopDownRenderer(3);

        Action walk = new Action(0, 1, true, false, false, 0f, 0f, false, false, null);
        for (int i = 0; i < ticks; i++) {
            TickResult r = engine.tick(state, walk);
            recorder.record(r.next, walk, r.events);
            state = r.next;
            if (r.terminal) break;
            if (i % 50 == 0) {
                renderer.writePng(state, out.resolve("frame_" + i + ".png"));
            }
        }
        renderer.writePng(state, out.resolve("final.png"));
        recorder.writeCsv(out.resolve("trace.csv"));
        System.out.println("Done. tick=" + state.tick + " stage=" + state.stage
                + " hp=" + state.player.health + " monsters=" + state.monsters.size());
        System.out.println("Wrote " + out.toAbsolutePath());
    }
}
