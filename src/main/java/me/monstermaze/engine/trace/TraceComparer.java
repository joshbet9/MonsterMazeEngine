package me.monstermaze.engine.trace;

import java.util.ArrayList;
import java.util.List;

/** Compares two traces (e.g. pure engine vs live plugin export). */
public final class TraceComparer {

    public static final class DiffReport {
        public final int framesCompared;
        public final double maxPosDelta;
        public final double maxVelDelta;
        public final double maxHealthDelta;
        public final int eventMismatches;
        public final List<String> samples;

        public DiffReport(int framesCompared, double maxPosDelta, double maxVelDelta,
                          double maxHealthDelta, int eventMismatches, List<String> samples) {
            this.framesCompared = framesCompared;
            this.maxPosDelta = maxPosDelta;
            this.maxVelDelta = maxVelDelta;
            this.maxHealthDelta = maxHealthDelta;
            this.eventMismatches = eventMismatches;
            this.samples = samples;
        }

        @Override
        public String toString() {
            return "DiffReport{frames=" + framesCompared
                    + ", maxPos\u0394=" + String.format("%.4f", maxPosDelta)
                    + ", maxVel\u0394=" + String.format("%.4f", maxVelDelta)
                    + ", maxHp\u0394=" + String.format("%.2f", maxHealthDelta)
                    + ", eventMismatches=" + eventMismatches + "}";
        }
    }

    public static DiffReport compare(List<TraceFrame> a, List<TraceFrame> b) {
        int n = Math.min(a.size(), b.size());
        double maxPos = 0, maxVel = 0, maxHp = 0;
        int eventMis = 0;
        List<String> samples = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            TraceFrame x = a.get(i);
            TraceFrame y = b.get(i);
            double pd = Math.hypot(x.px - y.px, x.pz - y.pz);
            double vd = Math.hypot(x.pvx - y.pvx, x.pvz - y.pvz);
            double hd = Math.abs(x.health - y.health);
            if (pd > maxPos) maxPos = pd;
            if (vd > maxVel) maxVel = vd;
            if (hd > maxHp) maxHp = hd;
            if (!x.events.equals(y.events)) {
                eventMis++;
                if (samples.size() < 10) {
                    samples.add("tick " + x.tick + " events a=[" + x.events + "] b=[" + y.events + "]");
                }
            }
            if (pd > 1.0 && samples.size() < 20) {
                samples.add(String.format("tick %d pos\u0394=%.3f a=(%.2f,%.2f) b=(%.2f,%.2f)",
                        x.tick, pd, x.px, x.pz, y.px, y.pz));
            }
        }
        return new DiffReport(n, maxPos, maxVel, maxHp, eventMis, samples);
    }
}
