# Road Rooster — Architecture

## Runtime flow

`gesture -> PlayerAction -> GameEngine fixed update -> HazardKinematics/contact collision -> RenderSnapshot -> GameplayProjection/Canvas draw`

- The pure Kotlin engine owns authoritative rules, time, entities, collisions, pickups, and terminal results.
- Authored `LevelDefinition` data is separate from rendering resources.
- `GameSession` uses `Choreographer`, clamps large deltas, steps the engine at 60 Hz, and fully stops callbacks while paused or terminal.
- `GameRenderView` draws the whole world in one Canvas and emits gestures. Compose hosts it and renders low-frequency HUD/overlays.
- `ProgressRepository` persists progression, lifetime achievement counters, per-level personal records, Road Aid inventory/armed state, and settings with DataStore only at meaningful state transitions. `LevelRecordsCodec` keeps the record map compact and backwards-compatible.

## Boundaries

- `game/`: models, tuning, engine, level catalog, validation.
- `presentation/`: app state, loop/controller, Compose screens, custom renderer.
- `data/`: player progress and persistence.
- `feedback/`: sound/haptic reactions to game events.

## Performance

- Fixed entity arrays/list reuse and no bitmap decoding in `onDraw`.
- `GameplayProjection` is the single camera contract for renderer/debug geometry: horizon 24.5%, player contact 88.5%, top road half-width 13.5%, bottom half-width 63%, and contact lane spacing 25.5%.
- Render ordering is far-to-near. Player feet, shadow, lane centers, entities, telegraphs, debug bounds, and collision contact all use the same normalized projection.
- HUD state is throttled; per-frame world state does not rebuild the Compose screen tree.
- A three-finger tap toggles the debug-only lane, bounds, speed, active pattern, entity count, progress, and FPS overlay; release builds compile it out.

## Fairness validation

Each authored pattern declares entry/exit motion state, entry/exit lanes, safe lanes, required action, minimum approach distance, and planned speed range. `HazardKinematics` is a pure shared component for runtime snapshots and validation, including static, rolling cross-lane, and one-shot traffic trajectories. `FairnessValidator` builds a timed lane/action reachability graph at both the minimum and maximum speed curves and reports the exact distance where no valid state survives.

Collision is resolved only after the swept world-distance transition crosses the player's contact line. Profile-specific reduced lane bounds are then tested against the live hop position and jump/dive clearance window; a 300 ms grace period and a 28-unit recorded jump-intent window allow a timely visible swipe to clear a large collider without forgiving a wrong-lane input.

Road Aids are explicit one-run engine inputs, not global modifiers. `FEATHER_GUARD` can consume itself on one collision event; `CORN_MAGNET` changes pickup bounds for Coins/Corn only. DataStore inventory is decremented atomically at run start, so retrying cannot duplicate an item.

## Progress and records

`ProgressRules.commitRun` is the single atomic transition for run totals, lifetime pickups, win totals, level unlocks, Golden Eggs and best records. The run token makes terminal commits idempotent. A winning result updates the fastest non-zero finish time and independently preserves the best Coin/Corn totals; a loss increments run/lifetime counters but cannot create a finish record. Older saves infer completed-road counts and expose unknown historical times as `LEGACY` instead of fabricating leaderboard data.
