package me.monstermaze.engine.trace;

import me.monstermaze.engine.api.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Records (action, observable state) pairs for offline comparison with live plugin traces. */
public final class TraceRecorder {
    private final List<TraceFrame> frames = new ArrayList<>();

    public void record(GameState state, Action action, List<GameEvent> events) {
        String ev = events == null ? "" : events.stream()
                .map(e -> e.type.name())
                .collect(Collectors.joining("|"));
        frames.add(new TraceFrame(
                state.tick,
                state.player.pos.x, state.player.pos.y, state.player.pos.z,
                state.player.vel.x, state.player.vel.y, state.player.vel.z,
                state.player.yaw, state.player.health,
                state.stage, state.phaseTimerTicks, state.monsters.size(),
                action.forward, action.strafe,
                action.sprint, action.jump, action.yawDelta,
                action.useAbility,
                ev
        ));
    }

    public List<TraceFrame> frames() {
        return frames;
    }

    public void writeCsv(Path path) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write(TraceFrame.csvHeader());
            w.newLine();
            for (TraceFrame f : frames) {
                w.write(f.toCsv());
                w.newLine();
            }
        }
    }

    public static List<TraceFrame> readCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<TraceFrame> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) out.add(TraceFrame.fromCsv(line));
        }
        return out;
    }
}
