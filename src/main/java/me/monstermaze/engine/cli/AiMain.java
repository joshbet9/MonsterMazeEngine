package me.monstermaze.engine.cli;

import me.monstermaze.engine.ai.HeuristicAgent;
import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.game.StageTimer;
import me.monstermaze.engine.trace.TraceRecorder;

import java.nio.file.Path;

/** Headless AI run. Usage: AiMain [ticks] [kit] [mode] [outDir] */
public final class AiMain {
    public static void main(String[] args) throws Exception {
        int maxTicks = args.length > 0 ? Integer.parseInt(args[0]) : 6000;
        KitType kit = args.length > 1 ? parseKit(args[1]) : KitType.JUMPER;
        MazeMode mode = args.length > 2 ? MazeMode.valueOf(args[2].toUpperCase()) : MazeMode.ORIGINAL;
        Path out = Path.of(args.length > 3 ? args[3] : "ai-out");
        java.nio.file.Files.createDirectories(out);

        int monsters = StageTimer.starterMonsters(mode);
        MonsterMazeEngine engine = new EngineImpl(monsters);
        GameState state = engine.initialState(mode, 0, kit, 42L);
        HeuristicAgent agent = new HeuristicAgent();
        TraceRecorder rec = new TraceRecorder();

        int peakStage = 0;
        for (int i = 0; i < maxTicks; i++) {
            Action a = agent.act(state);
            TickResult r = engine.tick(state, a);
            rec.record(r.next, a, r.events);
            state = r.next;
            if (state.stage > peakStage) peakStage = state.stage;
            if (r.terminal) break;
        }
        rec.writeCsv(out.resolve("ai-trace.csv"));
        System.out.println("AI done. stage=" + peakStage + " tick=" + state.tick
                + " hp=" + state.player.health + " terminal=" + (state.phase == GamePhase.ENDING)
                + " mobs=" + monsters);
    }

    private static KitType parseKit(String s) {
        s = s.toUpperCase().replace('-', '_');
        if (s.equals("SLOWBALLER")) s = "SLOWBALL";
        if (s.equals("BODYBUILDER")) s = "BODY_BUILDER";
        return KitType.valueOf(s);
    }
}
