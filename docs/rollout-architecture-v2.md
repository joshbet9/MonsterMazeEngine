# Monster Maze Engine Rollout Architecture v2

## Goal

Make large-scale policy improvement cheap enough to run continuously on a local development machine.

## Environment ownership

Each simulation episode is an independent session.

A session owns:
- maze dynamic overlays;
- RNG streams;
- monster runtime;
- player runtime;
- policy state;
- episode seed.

Static layout data is shared.

No mutable episode state may live in a singleton.

## Scalar reference path

SimulationSession.step(action) -> Engine.tick(state, action)

This remains the correctness reference.

## Fast training path

The production training runner should:
1. create a pool of simulation sessions;
2. generate compact policy observations;
3. batch policy inference;
4. step environments;
5. append compact transitions to dataset shards.

The batch runner must not change scalar game semantics.

## Shared static data

Precompute once per layout:
- raw topology;
- physical-floor maps;
- route catalogue;
- gap transitions;
- coordinate transforms;
- spawn cells;
- reusable neighbour tables.

Do not recompute them for every bot or every tick.

## Dynamic state

Keep dynamic fields per session:
- player state;
- monsters;
- pads;
- progression;
- timers;
- RNG;
- policy state.

## Allocation policy

Correctness APIs may allocate.

Training APIs should avoid:
- maze cloning;
- repeated object graphs;
- per-tick action objects;
- per-tick route lists;
- per-tick feature arrays.

Prefer reusable buffers and primitive arrays.

## Batching

Policy inference is the natural batching point.

A batch contains N observations by feature_count and returns N actions.

The simulation step remains per environment.

This lets a GPU evaluate many CPU brains while CPU workers advance independent worlds.

## Teacher/search workers

Teacher search uses a separate worker pool.

It may be slower and more allocation-heavy.

It must never block policy rollout workers.

A state selected for relabelling is copied exactly once into an offline teacher request.

## Failure-state queue

The rollout system ranks states by novelty and failure severity.

Do not simply generate more random episodes forever.

Actively collect states around:
- first failure;
- oscillation;
- late steering;
- missed gap;
- dangerous monster contact;
- poor ability timing;
- competitive losses.

## Model evaluation mode

The Engine exposes a production-emulation path using:
- direct policy only;
- production observation;
- production profile;
- production action semantics.

Teacher/search assistance is disabled.

Only this mode is eligible for deployment claims.

## Matrix evaluation

Full matrix evaluation is a batch workload.

Parallelize across independent sessions.

For each cell retain the same seeds across model versions so regressions are measurable.

The matrix continues past stage 10.

## Throughput metrics

Record:
- environments/second;
- simulation ticks/second;
- policy inferences/second;
- teacher states/second;
- memory used;
- CPU utilization;
- GPU utilization;
- allocation rate.

Do not optimize only wall-clock runtime; ensure generated data remains mechanically valid.

## Determinism

For the same layout, mode, kit, seed, initial state and action sequence, the scalar engine must produce the same result.

Batching must preserve this property.
