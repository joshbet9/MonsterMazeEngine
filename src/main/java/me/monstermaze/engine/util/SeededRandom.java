package me.monstermaze.engine.util;

import java.util.Random;

/** Deterministic RNG wrapper for the pure engine. */
public final class SeededRandom {
    private final Random random;

    public SeededRandom(long seed) {
        this.random = new Random(seed);
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public Random raw() {
        return random;
    }
}
