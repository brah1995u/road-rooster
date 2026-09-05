# Road Rooster — QA Checklist

## Gameplay

- [x] Lane transitions stop at road bounds.
- [x] Jump and dive timing are readable and mutually exclusive.
- [x] One buffered lane command cannot skip outside the three-lane road.
- [x] Dominant-axis gesture resolution and movement threshold are unit-tested.
- [x] Every authored pattern passes timed reachability at minimum and maximum planned speed.
- [x] Traffic has a visible warning marker and a one-shot audio warning event.
- [x] `HazardKinematics` drives the same one-shot traffic/tire paths in runtime and fairness validation.
- [x] Projection invariants lock distance-zero entities, chicken feet and lane centers to one contact model.
- [x] Full-road jump and dive tutorial gates are passed by scripted actions; the Golden Egg route and a simple safe route are deterministic.
- [x] Hop/jump/dive actions that visibly clear during the 300 ms impact grace survive; a missed profile-specific action still loses.
- [x] A single emulator swipe-up at distance 135.47 passes the Level 1 full-road barrier at distance 140; an intentionally early swipe at 127.89 expires before contact and is correctly explained by the dynamic tutorial cue.
- [x] The formerly failing large hay case was reproduced at distance 83.58, then passed with a swipe at distance 68.38; a continuous touch-script clears both the large hay and the following three-lane jump gate without collision.
- [x] The second phone-feedback pass raises the apex by 35%, extends jump duration to 1.35 s and validates a much earlier distance-60 jump against the first large hay; two ordinary emulator swipes clear both opening jump obstacles in one run.
- [x] All 15 catalog levels contain deterministic authored sections, one unique Golden Egg, a finish, and pass fairness validation at their planned speed bounds.
- [x] Coins, Corn, and Golden Eggs collect once per entity.
- [x] Win and loss are terminal/idempotent; collision loss, retry, pause, resume, next, and home paths are implemented.

## State and persistence

- [x] Fixed delta is clamped; background/foreground returns a live run to pause.
- [x] Result rewards are committed once using a run token.
- [x] Level unlocks, currency, Golden Eggs, and cosmetic rules are unit-tested.
- [x] Road Aid purchase, insufficient funds, arm, atomic consume, one-collision guard, magnet collection, and Golden Egg exclusion are unit-tested.
- [x] Lifetime run/win/Coin/Corn counters, idempotent commits, fastest per-level times, best pickup totals, loss exclusion, and record codec migration are unit-tested.
- [x] Older completed levels migrate to explicit `LEGACY` records without invented historical times or reset progress.
- [x] A sound-setting toggle survived emulator process restart through DataStore.
- [x] Sound and haptic preferences gate feedback behavior.
- [ ] Approved final music is unavailable; the persisted music preference is present but the track remains `TEMPORARY`.

## Visual gates

- [x] Menu: approved saturated road style, responsive branding, production hero mascot, no generic Material-demo appearance.
- [x] Gameplay: opaque dark road, lane perspective, chicken, hazards, pickups, flames, and HUD are readable.
- [x] Achievements/leaderboard/pause/results/settings/shop: supplied feather panels and gold/orange/brown controls are consistent and have safe touch targets.
- [x] Shop chicken portraits are clipped and scaled within their cards; Road Aid cards scroll without overlap.
- [x] Compact shop cards keep one-line chicken titles, smaller inset Buy buttons, and Road Aid content inside decorative panel bounds.
- [x] Level medals and achievement badges were reduced and inspected again at standard and compact density.
- [x] All 12 achievements scroll completely; the six new RGBA badges stay inside their cards with no checkerboard, black plate or clipping.
- [x] Local leaderboard summary and record row fit 1080×2400; migrated clears are labelled `LEGACY`, and the screen states that records stay on device.
- [x] Supplied wide gold/red button alpha bounds were measured; shared labels use the visible-frame optical center rather than the asymmetric bitmap-canvas center.
- [x] Optional button icons no longer participate in label centering; PLAY, BUY, ABOUT and PRIVACY were recaptured at 360×800 dp with centered text and no clipping.
- [x] Pause header uses round Back and Home controls at the left/right edges; the large in-panel HOME action is removed, Resume/Restart stay inside the supplied panel, and Android Back resumes the run.
- [x] About and Privacy screens are reachable from Settings, use the approved art language, scroll safely, and accurately describe offline/local-only storage.
- [x] 360×800dp compact menu, level select, and gameplay have no clipping or HUD overlap.
- [x] Approximately 412×915dp standard screens have no clipping or HUD overlap.
- [x] 480×960dp menu and gameplay have no clipping or HUD overlap.
- [x] Camera rework capture confirms broad road, 24.5% horizon, 88.5% foot contact, grounded shadow and shoulder-only roadside effects at 1080×2400.
- [x] A real 1080×2400 emulator path using dispatched swipe events completes Level 1 via the safe route, awards 19 Coins/8 Corn, persists the result, and unlocks Level 2.
- [x] A second 1080×2400 gameplay capture confirms fixed asphalt alignment, grounded reduced player scale, and one-shot traffic wheel contact inside the road corridor.
- [x] Win, lose, pause, menu, level select, achievements, and shop were recaptured at 360×800 dp; action buttons and text remain inside their panels.

## Build and performance

- [x] All 51 unit tests pass, including temporal fairness, controls, collisions, pickups, warnings, win, reward idempotency, lifetime achievement counters, personal records and record-codec migration.
- [x] `lintDebug`, `assembleDebug`, and minified `assembleRelease` pass.
- [x] No app-process errors occurred during the final emulator smoke tests.
- [x] Final `install -r` on physical device `SM02E4060323025` succeeded and the installed Road Rooster package reports versionCode 10 / versionName 0.4.2; existing app data was preserved. The phone was keyguard-locked during this pass, so Pause visuals were inspected on `tt_pixel`.
- [x] Bitmaps decode once into a cache; production runtime copies are downscaled independently from masters.
- [x] Choreographer callbacks stop while paused/terminal; HUD publication is throttled to 10 Hz.
- [x] No frame-loop logging and no bitmap decoding inside `onDraw`.

## Visual evidence

- `01-menu-final-1080x2400.png`
- `02-level-select-compact-360x800dp.png`
- `03-gameplay-final-412x915dp.png`
- `05-pause-final-1080x2400.png`
- `07-menu-compact-360x800dp.png`
- `08-menu-large-480x960dp.png`
- `10-shop-final-1080x2400.png`
- `11-settings-persistence-1080x2400.png`
- `12-gameplay-compact-360x800dp.png`
- `13-gameplay-large-480x960dp.png`
- `20-menu-camera-rework.png`
- `21-level-select-camera-rework.png`
- `22-gameplay-camera-rework.png`
- `23-gameplay-camera-rework-final.jpg`
- `28-safe-route-touch-result.jpg`
- `30-menu-ui-revision.png`
- `31-settings-ui-revision.png`
- `32-achievements-ui-revision.png`
- `33-gameplay-smaller-chicken.jpg`
- `35-pause-ui-revision.png`
- `36-menu-ui-revision-360x800dp.png`
- `37-menu-ui-revision-480x960dp.png`
- `38-jump-single-swipe-pass.jpg`
- `2026-09-02-expansion/two-jumps-cleared.png`
- `2026-09-02-expansion/traffic-on-road-view.jpg`
- `2026-09-02-expansion/shop-final.png`
- `2026-09-02-expansion/settings-final.png`
- `2026-09-02-expansion/about-final.png`
- `2026-09-02-expansion/privacy-final.png`
- `2026-09-02-expansion/levels-15-final.png`
- `2026-09-02-corrective/higher-jump-final.jpg`
- `2026-09-02-corrective/two-easy-jumps.jpg`
- `2026-09-02-corrective/lose-menu-final.png`
- `2026-09-02-corrective/win-menu-final.png`
- `2026-09-02-corrective/compact-menu.png`
- `2026-09-02-corrective/compact-levels.png`
- `2026-09-02-corrective/compact-achievements.png`
- `2026-09-02-corrective/compact-shop-final.png`
- `2026-09-02-corrective/compact-pause.png`
- `2026-09-02-achievements-leaderboard/menu.png`
- `2026-09-02-achievements-leaderboard/goals-top.png`
- `2026-09-02-achievements-leaderboard/goals-lower.png`
- `2026-09-02-achievements-leaderboard/leaderboard.png`
- `2026-09-02-button-centering/menu.png`
- `2026-09-02-button-centering/settings.png`
- `2026-09-02-button-centering/shop.png`
- `2026-09-02-button-centering/pause-redesign.png`
