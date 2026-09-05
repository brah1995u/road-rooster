# Road Rooster — Project Status

Status date: 2026-09-03

## Under-10-MB APK handoff — DONE (test-signed, smoke QA)

- The user clarified a strict delivery size limit. Both the signed APK and its one-APK ZIP must be below 10,000,000 bytes; using a larger binary MiB threshold is not acceptable.
- Same version `0.4.3` / code `11`, package ID and save schema. The previously approved existing test signature remains in use. No phone installation/data changes are made for this packaging task.
- Further compression is RGB-only WebP optimization: original dimensions and exact alpha are retained for all 85 PNG resources. No levels, skins, animation frames, music or native-device support are removed. Source and supplied art remain unchanged.
- The previous 24,686,264-byte archive below is **superseded and above the requested size limit**. It is retained unchanged rather than deleted or silently overwritten.
- Current delivery: `dist/Road-Rooster-0.4.3-under-10MB-APK.zip`, **9,604,277 bytes**, containing only `Road-Rooster-0.4.3-under-10MB-test-signed.apk`, **9,822,014 bytes**, at the root. Both are strictly below 10,000,000 bytes.
- APK SHA-256: `D63D3ADB3279E091E485E56B6E241CAD17973559CFEC7874ADC371F46BFF9D76`; ZIP SHA-256: `5590159262CF47ED8D1811F9780B6787C92D9A5A96592535E725A657AC62E46C`. Reopened ZIP has exactly one entry and its embedded APK hash matches the signed input.
- Release build and lint pass; 51 unit tests retain passing results (gameplay code unchanged). Lint: 0 errors, 15 pre-existing warnings. Signature v2/v3 verifies against the same existing test certificate; `zipalign -c -P 16 4` passes; no `debuggable` flag; package/version and all four existing native ABIs are preserved.
- All 85 PNG overlays keep their original dimensions and bit-exact alpha. Full overlay size: 8,018,502 bytes. Per-image luminance-weighted error stays below 4.5/255 (maximum approximately 4.24); original source files are retained.
- Final APK installed with `install -r` and cold-started on isolated Android 15 / API 35 emulator, 1080×2400. Existing 76 Coins, 14 Corn and level unlocks persisted. Checked Menu, Settings, Shop, Achievements, Levels, touch input, running/pickups, pause/resume, background return, expected loss with no avoidance at the first hay, and result retry. Music OFF persisted through process restart and was restored to ON. No app crash or app media/bitmap/resource-decoding error was found.
- Visually reviewed real menu, achievements and pause captures and the compressed Golden dive asset. Native-size menu mean absolute RGB difference against the original PNG build: approximately 0.806/0.556/0.971 out of 255. Alpha and layout anchors do not move. Evidence is kept separately in ignored `test-output/apk-under10mb-2026-09-03/`, never inside the delivery ZIP.
- QA scope remains a packaging smoke test, not a new full 15-level victory/Golden Egg/per-skin or multi-device certification. Audio was not subjectively listened to and physical haptics were not tested. The phone installation and save data were not modified.
- `tools/package_apk_zip.ps1` enforces byte limits on both files, refuses to overwrite previous ZIPs, checks the root entry and SHA-256, and rejects the former oversized APK before creating an archive.

## Previous compact APK handoff — SUPERSEDED (above 10 MB)

- The user asked for an optimized APK archive using the Flame Jester approach. The previously added APK-ZIP rules were removed from `AGENTS.md` at the user's request; this handoff does not re-add them.
- The source version is `0.4.3` / versionCode `11`. No additional version was invented for packaging; `com.chickenroadrunner.game` and the save schema remain unchanged.
- Release builds enable R8 and resource shrinking plus generated WebP overlays. The original 85 PNGs remain untouched; the overlay pack is 23,117,550 bytes versus 55,878,900 bytes of source PNGs, with identical dimensions and alpha. The final APK removes unused resources on top of this reduction.
- The installed phone APK was read without modifying the installation: `0.4.2` / versionCode `10`, `application-debuggable`, certificate DN `C=US, O=Android, CN=Android Debug`, certificate SHA-256 `cfb12b8f8a3a6da7f50cbdd1ea7f5ac796ea64edabab46ebf7eda71d869ecd4a`.
- The user explicitly selected the existing test signature, as used by Flame Jester, to preserve update compatibility with the phone installation. This is a release-optimized, non-debuggable APK signed with the existing test key, not a permanent-key store release. No new key is created; no phone app/data is removed or changed by this archive task.
- The existing local signing certificate was verified against the installed phone certificate above. The final archive contains one clearly named test-signed APK, no source or credentials. No permanent-key/store-release claim is made.
- The latest pause changes remove the inactive in-panel pause glyph and explanatory sentence; only Resume and Restart remain. The temporary visual-only launch harness remains removed. No gameplay, save schema or UI-layout code was changed for this packaging task.
- Delivery: `dist/Road-Rooster-0.4.3-APK.zip`, exactly **24,686,264 bytes**, containing only `Road-Rooster-0.4.3-optimized-test-signed.apk` at the root.
- Final APK: **24,899,390 bytes**. Baseline unoptimized-art release APK was 46,915,265 bytes; the existing development debug APK was 67,183,497 bytes. Final APK contains 84 WebP resources and the unchanged Ogg soundtrack; no production PNG payload remains.
- APK SHA-256: `2F4C614A0D2599CE9BF1AADDCF56D6C898B042B92B350183B2718F6C6E878B3B`. ZIP SHA-256: `5673590801A0422F5FD9E6A9F2F7C66D9587DE0BA6B06C0E5A50155BEBBA62F2`.
- Checks: 51 unit tests pass; `lintDebug`, `lintRelease` and `assembleRelease` pass. Both lint reports have 0 errors and 15 pre-existing warnings. APK signature v2/v3 verifies, `zipalign -c -P 16 4` passes, version/package are unchanged, `debuggable` and debug tools are disabled. The archive was reopened, its single entry checked, and the embedded APK SHA-256 matched byte-for-byte.
- Exact final APK installed with `install -r` on an isolated Android 15 / API 35 emulator at 1080×2400. Existing Coins (76), Corn (14), unlocked roads and the legacy personal record survived the update. Cold launch, menu, Settings/About/Privacy, Shop, Achievements, Leaderboard, level selection, touch input, pause/resume, Home/background pause, Restart and lose-screen retry were exercised.
- Music OFF persisted through a force-stop/cold launch and was then restored to ON. Audio playback appeared active in the emulator service, and no app crash or media/resource-decoding error was found. Audio was not subjectively listened to, and physical haptics were not assessed on the headless emulator.
- Visual comparison: inspected real final menu, settings, shop, gameplay, pause and lose captures. Menu mean absolute RGB difference versus the PNG build was approximately 0.45/0.35/0.51 out of 255; transparent sprite/frame anchors and dimensions were checked by the converter.
- QA limitation: the real repeated-swipe run cleared the first hay and low-barrier sections, then lost at the gate at 33% (10 Coins / 4 Corn); it is not a completed victory playthrough. The final APK's full 15-level win/egg/cosmetic matrix and all screen sizes were not re-run. Screenshots and test drivers are in ignored `test-output/apk-compact-2026-09-03/`, outside the ZIP. Historical full-playthrough results below are not claimed as new final-APK QA.

## DONE

- Native Android application `com.chickenroadrunner.game`: Kotlin/JVM 17, minSdk 24, compile/targetSdk 35, Compose shell, custom Canvas gameplay renderer, DataStore persistence.
- Deterministic fixed-step engine: three lanes, 0.18 s buffered lane hop, brief 0.90 s / 1.42-height ballistic jump with a 28-unit intent window, staged 0.62 s chicken dive, 160 ms vertical buffer, 180 ms impact grace, reduced profile hitboxes, delta clamp, pause/resume, and single-hit terminal flow. Every physical hazard accepts a valid jump-clear window, while lane changes and dive remain alternative routes/actions.
- Fifteen approximately 68–72 second deterministic levels with Coins, Corn, one unique Golden Egg each, route choices, fixed hazards, action gates, one-shot moving traffic/tire trajectories, and coop finishes. Level 1 explicitly teaches lane choice, jump, and dive before combining actions; Levels 4–15 progressively increase speed without introducing unavoidable random layouts.
- Pattern metadata and timed fairness graph validated at each level's minimum and maximum planned speed.
- Dedicated Road Rooster splash, production menu, scrollable 15-level select, gameplay/HUD, pause, win/lose results, settings, About, Privacy, 12-goal achievements, local leaderboard, and expanded shop are implemented and reachable.
- Atomic run rewards, level unlocks, currency, unique eggs, lifetime Coins/Corn, run/win counters, per-level personal records, cosmetic purchases/selection, Road Aid purchase/arming/consumption, and settings persistence.
- Complete Classic, Farmer, Racer, and Golden cosmetic sets; every skin now has its own idle, left contact, passing step, right contact, jump, dive, hit, and win artwork. Cosmetic selection changes artwork only and never modifies gameplay.
- Production visual pass: all 19 supplied files inventoried and used first; approved generated masters are preserved. The exact Road Rooster title, dedicated grounded-rooster splash, adaptive launcher icon, Home, Chicken Closet and Leaderboard icons, 24 cosmetic frames, and 12 unique achievement badges complete the supplied buttons/panels/icons without replacing them.
- Achievements no longer repeat the generic star. The expanded set covers first and total wins, Road 8/15 milestones, three/eight/all Golden Eggs, lifetime Corn and Coins, total runs, and cosmetic collection; all 12 have readable RGBA medallions.
- The local leaderboard records each road's fastest valid finish, best Coins/Corn, Golden Egg, and clear count. Old completed levels migrate visibly as `LEGACY` when historical time data is unavailable; no fake global players or fabricated scores are shown.
- Camera/physics rework: a shared projection locks the horizon at 24.5%, foot contact at 88.5%, broad road geometry and 25.5% contact lane spacing. Render, shadow, debug bounds, entity lanes, and collision contact no longer use separate coordinate models.
- Road/fence alignment corrective pass: the earlier background's baked dirt road and fences were removed in `bg_gameplay_horizon_v4`; the asphalt edge, split roadside layer, guardrails, flames, lane centers and collision projection now converge symmetrically through the same calibrated corridor. Standard 1080×2400 emulator capture was visually inspected after the change.
- Asphalt sampling, road edges, guardrails, and dashed lane dividers are all fixed to one projection. Animated surface specks and distance-shifted dashes have been removed, so no road element travels sideways toward the chicken; only gameplay entities advance. Lane rays keep one linear perspective slope and use a lightly widened 27% contact spacing for clearer routes without changing lane logic. Traffic endpoints remain constrained to ±1.45 lanes with measured wheel anchors and direction-aware mirroring.
- Gameplay Visual Gate 2 corrective pass: separate horizon, asphalt and transparent guardrail layers; shoulder-only flames; measured per-frame chicken foot anchors; 7–9 fps three-frame grounded run cycle; pivoted squash, contact dust and jump-independent body motion.
- The coop finish is rendered as a full-road landmark rather than a lane-sized obstacle: its open arch now spans the three-lane corridor at contact, with a proportionally widened ground shadow and matching debug art bounds.
- Cosmetic anchors are alpha-measured per pose, and runtime sprite selection is centralized in `ChickenArtCatalog`; Farmer, Racer and Golden therefore remain grounded through run, jump, dive, hit and victory instead of falling back to tinted Classic art.
- Touch actions dispatch as soon as the dominant-axis gesture crosses `max(18dp, 3.5% short edge)` with a 1.15× axis rule, retaining one buffered lane command and incompatible jump/dive exclusion.
- Level 1 now teaches real collision timing: `GET READY` becomes `JUMP NOW` / `DIVE NOW` from runtime contact distance, with a temporary in-window tap arrow as an accessible fallback. A measured single swipe at distance 135.47 clears the distance-140 barrier.
- Large hay is now a jump profile rather than a lane-only block. The original failure was reproduced (jump at 68.53, collision at 83.58), and the corrected intent-aware physics then cleared both that hay and the next full-road barrier in one continuous emulator run.
- Shop portraits were reduced and clipped to their supplied parchment cards. Two optional single-run Road Aids are available for earned Coins: Feather Guard forgives one hit and Corn Magnet widens Coin/Corn pickup reach; neither creates score multipliers, speed changes, paid advantages, or rechargeable loops.
- Responsive UI corrective pass: level medals are 46 dp, achievement badges 36 dp, shop buttons are smaller/inset, and compact shop titles remain one line. Win/lose results now use the equipped chicken's dedicated victory/hit art, a layered star/barrier stage, three illustrated reward cards, Golden Egg presentation, coin-bank summary, a strong full-width primary action and compact secondary navigation.
- Button optical-centering pass: the supplied gold/red wide PNGs were alpha-measured instead of treated as symmetric canvases. Every shared PLAY/BUY/ABOUT/PRIVACY/pause/result action now centers its label on the visible embossed frame; optional restart/home/next icons render in a separate leading slot and cannot shift the text. Menu, Settings and Shop variants were visually inspected at 360×800 dp.
- Pause Visual Gate redesign: navigation is now a dedicated top row matching the supporting screens, with round Back/resume on the left, centered `ROAD PAUSED`, and round Home on the right. The oversized HOME action was removed from the panel; the card contains only the pause state, Resume and Restart. Android system Back also resumes a paused run instead of unexpectedly leaving for the menu.
- Feedback hooks for pickups, Golden Egg, actions, traffic warnings, collisions, buttons, win/lose, and haptics. An original 32-second, 120 BPM Road Rooster farm-arcade score is packaged as an efficient Ogg loop; it follows the persisted MUSIC setting, app background lifecycle, Android audio focus, and ducking.
- Debug-only three-finger overlay for lane guides, red contact line, green art bounds, magenta physics bounds, player bounds, speed, pattern, entity count, progress and FPS; off by default and compiled out of release.
- Originality audit passed: no combo, streak, multiplier, rechargeable boost loop, Fever/Overdrive/Rage, risk meter, or near-miss chain exists in code or content. Road Aids are finite inventory items consumed once at run start.
- Automated gate: 51 unit tests pass after the universal jump-clear, faster pacing, atomic lifetime-stat and personal-record updates, idempotency, fastest-time selection, loss exclusion, and record-codec migration coverage. Projection tests pin the road/fence corridor and lane expansion; gameplay tests cover every physical collision profile, brief jump duration, fast obstacle retirement, the large-hay window and Road Aids; catalog tests validate all 15 levels and traffic endpoints.
- Emulator gate on `tt_pixel`: install/launch, touch gameplay, collision/retry, pause, Home/background/foreground, DataStore process restart, Visual Gate captures, and a full swipe-driven Level 1 safe-route win with persisted Level 2 unlock. The 2026-09-02 pass additionally verifies Shop portraits, all 12 achievements, the local leaderboard and legacy migration, Farmer run/jump/dive and per-foot grounding.
- Runtime art was capped by actual display role while all masters remain untouched. The same debug emulator screen-tour/gameplay audit dropped from roughly 315 MiB to 168 MiB PSS after optimization.
- Road Rooster v0.4.2 debug APK installed successfully with `install -r` on physical device `SM02E4060323025`; existing app data was preserved. The physical install reports versionCode 10 / versionName 0.4.2. This installation occurred while the device keyguard was locked, so the Pause visual gate was performed on `tt_pixel` rather than falsely claiming a phone-side screen inspection.
- Responsive captures verified at 360×800dp, approximately 412×915dp, and 480×960dp with no final clipping or HUD overlap.

## TEMPORARY

- Current SFX are lightweight Android tone cues. They are functional and lifecycle-safe but remain `TEMPORARY` until an original authored SFX bank is approved.

## PLANNED OUTSIDE MVP

- Endless mode, daily missions, ads/IAP, analytics, cloud saves, online services, additional environments, and a large cosmetic catalog.
- AAB, permanent-key store publishing, and store-specific publishing artifacts remain outside this explicitly test-signed APK handoff.

## Superseded development artifacts — NOT APK-ZIP delivery

These files are retained for development history only. They are not acceptable final handoff artifacts under the APK-ZIP standard.

- Installable debug APK: `dist/RoadRooster-debug.apk`
- Debug APK: 64.07 MiB; SHA-256 `72D5A6338D2F3ADF83EDE35D7FF3E908156C35FE24D8473EDAADA35EDCC336FD`
- Release compile artifact: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Release artifact: 44.74 MiB; SHA-256 `600D8D7416B89CA7BE41C972FBC28993C523D4226C2FE25AC617286C1038947D`
- Visual evidence: `screenshots/visual-gates/` including `screenshots/visual-gates/2026-09-02-road-rooster/`
- Generated masters and prompt provenance: `art/generated/source/` and `art/generated/PROMPTS.md`
