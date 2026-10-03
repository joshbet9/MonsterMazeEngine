# CPU Policy Training Contract v2

## Purpose

MonsterMazeEngine exists to make the CPU-opponent policy trainable and measurable at high speed.

The engine must support both:

1. expensive teacher/search evaluation;
2. exact production-policy evaluation.

Those modes must remain distinguishable.

## Policy interface

The production-style policy interface is conceptually:

```java
Observation -> PolicyState
PolicyState + Profile -> Action
```

It must not require:

```
Observation -> enumerate candidates -> simulate each -> score -> select
```

That second form is the teacher.

## Hierarchical execution

The engine supports:

```
GameState
   |
   +--> tactical policy (event/cadence)
   |
   +--> locomotion policy (per tick)
   |
   v
Action
   |
Engine.tick
```

This is the same execution structure that the 1.8 server will use.

The engine must be able to run a candidate model in this mode with the teacher disabled.

## Teacher

Existing route planners, beam searches and trajectory simulations remain valuable.

They answer:

> What source-valid action or short tactical sequence has good expected outcome from this exact state?

Teacher outputs should label candidate-policy states.

Teacher computations never execute in the production server.

## Dataset generation

Engine should generate samples from:

- clean teacher trajectories;
- candidate-policy rollouts;
- deliberately difficult states;
- randomized monster configurations;
- randomized starting offsets;
- multi-player competitions;
- sampled CPU profiles.

Each sample records the model contract version and source of its label.

## Multi-player environment

A match environment owns one shared:

- maze;
- pad lifecycle;
- monster population;
- game clock;
- participant set.

Each participant owns:

- player state;
- profile;
- policy state;
- deterministic seed.

This is required for training CPU opponents that understand actual competition rather than only solo navigation.

## Efficiency tiers

### Correctness mode

Prioritises transparency and exact state copies.

Useful for mechanics tests and differential traces.

### Training mode

Prioritises throughput:

- reusable state;
- precomputed layouts;
- primitive buffers;
- batched observations;
- parallel environments;
- reduced event allocation;
- optional packed trajectory storage.

### Production-emulation mode

Uses exactly:

- production feature encoder;
- production policy artifact;
- production profile interpretation;
- production action projector.

No teacher assistance.

## State ownership

Static layout data is immutable and shared.

Dynamic match state is mutable per environment.

Per-tick observation objects should be avoided in the hot training loop; write directly into reusable primitive feature buffers where possible.

## Rollout batching

The first scalar `MonsterMazeEngine.tick` API remains the correctness contract.

A second batched environment API can operate over many environments while preserving the same tick semantics.

Batching must not alter mechanics.

## Evaluation

Every candidate model is tested against a fixed holdout seed set and a changing exploration set.

The full matrix has no permanent stage cap.

A CI stage ceiling is allowed only as a smoke test.

Promotion compares:

- stage distribution;
- maximum stage;
- survival/completion;
- damage;
- elimination reasons;
- ability use;
- multi-player outcomes;
- inference cost.

## Differential validation

When Engine and MonsterMaze differ, the failure is a mechanics/adapter issue until disproven.

Do not train the policy to compensate for a known simulation mismatch.

Engine trace comparison remains a core validation mechanism.

## Server readiness gate

The Engine must be capable of loading the exact artifact that the 1.8 server intends to use and executing it without teacher/search support.

Only then is an apparently successful training result considered meaningful for deployment.
