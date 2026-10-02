package me.monstermaze.engine.audio;

import me.monstermaze.engine.api.GameEvent;
import me.monstermaze.engine.api.GameEventType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.List;

/** Tiny procedural SFX (no asset files). */
public final class Sfx {

    private static volatile boolean enabled = true;

    private Sfx() {}

    public static void setEnabled(boolean on) { enabled = on; }

    public static void playEvents(List<GameEvent> events) {
        if (!enabled || events == null) return;
        for (GameEvent e : events) {
            if (e.type == GameEventType.DAMAGE || e.type == GameEventType.KNOCKBACK) {
                beep(180, 40, 0.25);
            } else if (e.type == GameEventType.PAD_REACHED) {
                beep(520, 80, 0.2);
                beep(660, 80, 0.2);
            } else if (e.type == GameEventType.ABILITY_USED) {
                beep(320, 50, 0.18);
            } else if (e.type == GameEventType.STAGE_ADVANCE) {
                beep(400, 60, 0.15);
                beep(500, 60, 0.15);
                beep(600, 80, 0.2);
            } else if (e.type == GameEventType.ELIMINATED) {
                beep(120, 200, 0.3);
            }
        }
    }

    private static void beep(int hz, int ms, double vol) {
        Thread t = new Thread(() -> {
            try {
                float sampleRate = 22050f;
                byte[] buf = new byte[(int) (sampleRate * ms / 1000)];
                for (int i = 0; i < buf.length; i++) {
                    double angle = 2 * Math.PI * i * hz / sampleRate;
                    buf[i] = (byte) (Math.sin(angle) * 127 * vol);
                }
                AudioFormat fmt = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
                line.open(fmt);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
            }
        }, "sfx");
        t.setDaemon(true);
        t.start();
    }
}
