package me.monstermaze.engine.ai;

import me.monstermaze.engine.api.Action;
import me.monstermaze.engine.api.GameState;
import me.monstermaze.engine.ml.CpuObservationEncoder;

/**
 * Stateful direct-policy competitor used by Engine training/evaluation.
 *
 * <p>The old planner-based competitor remains available as a teacher/reference
 * implementation. This class is the production-shaped path: fixed observation
 * encoding, one policy decision, then normal Engine mechanics.
 */
public final class DirectCpuCompetitor {
    private final DirectCpuPolicy policy;
    private final CpuObservationEncoder encoder;
    private final float[] observation = new float[CpuObservationEncoder.FEATURE_COUNT];

    public DirectCpuCompetitor(DirectCpuPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("policy");
        this.policy = policy;
        this.encoder = new CpuObservationEncoder();
    }

    public void reset(long seed) {
        policy.reset(seed);
    }

    public Action decide(GameState state) {
        if (state == null || !state.alive || state.completed) return Action.noop();
        encoder.encode(state, observation);
        return policy.decide(state);
    }
}
