# Road Rooster — Game Design

## Product promise

A polished portrait arcade runner about an expressive chicken crossing an increasingly chaotic road. The player reads space and timing, chooses routes, and reaches a visible coop destination. The game does not use score-chain or multiplier farming.

## Controls and feel

- Swipe left/right: immediate 0.18 s one-lane hop with smoothstep lateral motion, a small parabolic lift, contact dust, and one buffered lane command.
- Swipe up: deterministic 1.35 s ballistic jump with a 35% taller apex; clearance begins with the first visible lift (normalized height 0.06) and remains active until 1.34 s. A 28-world-unit jump-intent window preserves a swipe made while a large barrier is visibly approaching the lower half of the road.
- Swipe down: 0.62 s chicken dive: 0.10 s enter, 0.36 s hold, and 0.16 s exit.
- Gestures dispatch on `ACTION_MOVE` after `max(18dp, 3.5% of the short edge)`; the dominant axis must win by 1.15×. Jump and dive are mutually exclusive and vertical input buffers for at most 160 ms.
- A 300 ms impact grace window lets a visibly clearing hop/jump/dive finish instead of producing a late unfair hit.
- Level 1 uses context-aware `GET READY` then `JUMP NOW` / `DIVE NOW` prompts calculated from the actual hazard contact distance. During the active teaching window a large arrow can also be tapped, while swipes remain the primary control.

## Collectibles

- Coins: cosmetic currency.
- Corn: per-level objective and persistent total.
- Golden Egg: one authored risky pickup per level; permanently recorded once collected.

## Levels

1. **First Crossing** — 68–72 second teaching run: pickup-guided lane choice, full-road jump tutorial, full-road dive tutorial, one obvious safe-lane split, one-shot tire and traffic crossings, a readable Golden Egg jump route, and a clean coop runway.
2. **Fireline Farm** — cones, manholes, carts, alternating actions, two traffic waves, Corn route, hard egg route.
3. **Coop Run** — timed gates, trucks, rolling tire, route split, mixed final challenge.
4. **Sunrise Sprint** through 15. **Grand Coop Run** — twelve deterministic 68–70 second roads that progressively tighten reaction budgets while retaining authored jump, dive, route-split, traffic, rolling-tire, Golden Egg, recovery, and finish sections.

Levels alternate introduction, pressure, recovery, signature event, reward route, final challenge, and finish. Every authored sequence declares a safe response.

Tall carts remain lane-change hazards; hay, cones, manholes, and low barriers require jump; overhead gates require dive. Traffic and the rolling tire use authored one-shot cross-lane trajectories instead of a global repeating timer. Pickups have a deliberately larger forgiving collection radius than hazards.

## Progression

- Level 1 unlocked initially; completing a level unlocks the next.
- Classic skin is free. Farmer and Racer cost Coins. Golden unlocks after all three Golden Eggs.
- Cosmetics never change gameplay statistics.
- Optional **Road Aids** are bought only with earned Coins and consumed when an armed run starts: Feather Guard forgives the first collision once, while Corn Magnet widens only the Coin/Corn collection radius. They never alter speed, score, Golden Eggs, or unlock requirements.
- Twelve achievements cover first/total wins, Road 8 and Road 15 milestones, Golden Egg sets, lifetime Coins/Corn, total runs, and cosmetic collection. Their counters use lifetime statistics, so spending currency never reduces progress.
- The local leaderboard stores a personal best finish per road: fastest valid completion time, best Coins, best Corn, Golden Egg status, and clear count. Existing clears from older builds migrate as `LEGACY` records without inventing a time.

## Explicit exclusions

No combo, streak, multiplier, rechargeable boost meter/loop, endless mode, ads, IAP, daily missions, analytics, global leaderboard, account system, or network dependency in MVP.
