# Road Rooster — Asset Map

`design/` is the immutable visual source of truth. Its 19 originals are never renamed or overwritten. Runtime copies use normalized Android resource names in `app/src/main/res/drawable-nodpi/`.

## Supplied inventory

| Source file | Dimensions | Alpha | Intended role | Runtime / status |
|---|---:|:---:|---|---|
| `appname_Icon_Final.png` | 4096×4096 | no | Original road-sign/flame launcher reference | `launcher_source.png`; supplied master retained, superseded by Road Rooster identity icon |
| `bg-01 (1).png` | 1290×2792 | no | Bright road/menu backdrop | `bg_menu_road.png`; production |
| `bg-02 (1).png` | 1290×2792 | no | Feather supporting backdrop | `bg_feathers.png`; production |
| `bg-03.png` | 1290×2792 | no | Dark road and flame supporting backdrop | `bg_fire_road.png`; production |
| `black.jpg` | 4320×1920 | no | Promotional style/composition reference | reference only; multiplier content forbidden |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1.png` | 2048×1076 | yes | Wide gold button | `ui_button_gold_wide.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (1).png` | 2048×1076 | yes | Wide red button | `ui_button_red_wide.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (2).png` | 2048×2008 | yes | Round gold control frame | `ui_button_gold_round.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (3).png` | 2048×1568 | yes | Gold rectangular panel/button | `ui_panel_gold.png`; production candidate; not stretched into tall cards |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (4).png` | 2048×1568 | yes | Red rectangular panel/button | `ui_panel_red.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (5).png` | 2048×1280 | yes | Cream feather reward panel | `ui_panel_reward.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (6).png` | 2016×1568 | yes | Large cream feather panel | `ui_panel_large.png`; production card/panel base |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (7).png` | 1140×908 | yes | Right-arrow icon | `ui_icon_next.png`; production; mirrored for Back |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (8).png` | 1140×1124 | yes | Restart icon | `ui_icon_restart.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (9).png` | 1140×1124 | yes | Star icon | `ui_icon_star.png`; production; Goals navigation/win accent |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (10).png` | 1140×1124 | yes | Upgrade-like icon | reference only; boost mechanic forbidden |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (11).png` | 1140×1124 | yes | Settings icon | `ui_icon_settings.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (12).png` | 2048×628 | yes | Dark/gold HUD progress frame | `ui_progress_frame.png`; production |
| `Gemini_Generated_Image_gsfvpsgsfvpsgsfv 1 (13).png` | 1168×1248 | yes | Chicken-emblem coin | `pickup_coin.png`; production |

Runtime optimization never changes these originals. The phone copies are sized to their actual display role: 512 px for the launcher-source/menu glyph and round icons, 1200 px maximum for wide buttons/panels/progress chrome, 768 px for the gameplay coin, and 896 px for the asphalt/flame layers and Classic run cycle. This pass reduced the observed debug emulator screen-tour/gameplay PSS from roughly 315 MiB to 168 MiB while preserving the full-resolution masters.

The supplied wide-button canvases are intentionally asymmetric: alpha inspection measured the visible gold frame at `x=56..1153, y=180..573` inside the 1200×630 runtime bitmap and the red frame at `x=46..1139, y=180..573`. `ArcadeButton` therefore uses their visible-frame optical center (about 9.8% below raw canvas center), while gold/red horizontal offsets are calibrated separately. Text is absolutely centered and optional icons occupy an independent leading slot, so an icon never pushes the label off center.

## Generated production masters

Masters are preserved in `art/generated/source/`. Runtime copies are independent Android resources; earlier assets are optimized/downscaled where appropriate, while V3 alpha-anchored run frames retain master resolution to preserve clean feather edges.

| Master | Dimensions | Alpha | Runtime use / status |
|---|---:|:---:|---|
| `bg_gameplay_farm.png` | 1024×1536 | opaque | Farm/sky horizon behind the Canvas road; production |
| `chicken_runner.png` | 1024×1536 | yes | Run and lane-hop anchor; production |
| `chicken_idle.png` | 1024×1536 | yes | Main-menu hero portrait; production |
| `chicken_jump.png` | 1024×1536 | yes | Jump state; production |
| `chicken_duck.png` | 1536×1024 | yes | Chicken dive state; production |
| `chicken_hit.png` | 1199×1312 | yes | Collision state; production |
| `chicken_win.png` | 1024×1536 | yes | Finish state; production |
| `pickup_corn.png` | 1214×1295 | yes | Corn collectible; production |
| `pickup_golden_egg.png` | 1187×1326 | yes | Unique level collectible; production |
| `obstacle_hay_bale.png` | 1536×1024 | yes | Solid lane blocker; production |
| `obstacle_low_barrier.png` | 1536×1024 | yes | Jump barrier; production |
| `obstacle_duck_gate.png` | 1188×1324 | yes | Overhead dive gate; production |
| `obstacle_cone.png` | 1223×1286 | yes | Small hazard; production |
| `obstacle_manhole.png` | 1536×1024 | yes | Ground hazard; production |
| `obstacle_cart.png` | 1205×1305 | yes | Cart/wheelbarrow obstacle; production |
| `obstacle_rolling_tire.png` | 1214×1295 | yes | Moving tire hazard; production |
| `traffic_car.png` | 1536×1024 | yes | Side-view crossing vehicle; production |
| `traffic_truck.png` | 1536×1024 | yes | Heavy crossing vehicle; production |
| `finish_coop.png` | 1536×1024 | yes | Level finish landmark; production |
| `road_asphalt_texture_v2.png` | 1254×1254 | no | Seamless dark asphalt grain, perspective-warped and scrolled by the Canvas renderer; production |
| `roadside_flame_accent_v2.png` | 1024×1536 | yes | Transparent multi-tongue roadside flame with warm bloom; production |
| `bg_gameplay_horizon_v3.png` | 992×1586 | no | Earlier sky/farm concept with a baked dirt road and fences; master retained for provenance, runtime fallback only |
| `bg_gameplay_horizon_v4.png` | 992×1585 | no | Corrected road-free and fence-free farm/sky layer; runtime copy normalized to 992×1586 and cropped to the shared 24.5% horizon; production |
| `roadside_props_v3.png` | 992×1586 | yes | Perspective fences, guardrails, depth markers, crate, sacks and plants with a transparent playable corridor; production |
| `chicken_run_contact_left_v3.png` | 1254×1254 | yes | Grounded run contact frame; measured bottom alpha padding 9.1%; production |
| `chicken_run_pass_v3.png` | 1254×1254 | yes | Low passing frame; measured bottom alpha padding 6.1%; production |
| `chicken_run_contact_right_v3.png` | 1254×1254 | yes | Opposite grounded run contact; measured bottom alpha padding 6.4%; production |
| `ui_title_logo_road_rooster.png` | 1130×840 | yes | Exact two-line `ROAD` / `ROOSTER` brand logo; chroma extraction was converted to true RGBA, runtime `ui_title_logo.png` optimized to 1004×746; production |
| `bg_splash_road_rooster.png` | 864×1821 | no | Dedicated 9:20 Road Rooster launch scene with grounded hero, straight three-lane road and title-safe sky; runtime `bg_splash_road_rooster.webp`; production |
| `ic_launcher_road_rooster.png` | 1254×1254 | no | Text-free Road Rooster portrait with road, sunrise and gold chevron frame; runtime 768×768 and Android adaptive-icon wrapper; production |
| `ui_icon_home.png` | 1254×1254 | yes | Coop/home navigation icon; runtime optimized to 512×512; production |
| `ui_icon_shop.png` | 1254×1254 | yes | Chicken Closet/shop navigation icon; runtime optimized to 512×512; production |
| `chicken_farmer_idle_alpha.png` | 1024×1536 | yes | Farmer hero/shop idle; runtime `chicken_farmer_idle.png`; production |
| `chicken_farmer_run_contact_left_alpha.png` | 1254×1254 | yes | Farmer left-foot contact; runtime anchor 9.2%; production |
| `chicken_farmer_run_pass_alpha.png` | 1254×1254 | yes | Farmer passing step; runtime anchor 5.2%; production |
| `chicken_farmer_run_contact_right_alpha.png` | 1244×1264 | yes | Farmer right-foot contact; runtime anchor 1.0%; production |
| `chicken_farmer_jump_alpha.png` | 1024×1536 | yes | Farmer jump; runtime anchor 11.6%; production |
| `chicken_farmer_duck_alpha.png` | 1536×1024 | yes | Farmer dive; runtime anchor 6.0%; production |
| `chicken_farmer_hit_alpha.png` | 1199×1312 | yes | Farmer collision; runtime anchor 3.0%; production |
| `chicken_farmer_win_alpha.png` | 1024×1536 | yes | Farmer victory; runtime anchor 4.7%; production |
| `chicken_racer_idle_alpha.png` | 1024×1536 | yes | Racer hero/shop idle; runtime `chicken_racer_idle.png`; production |
| `chicken_racer_run_contact_left_alpha.png` | 1230×1278 | yes | Racer left-foot contact; runtime anchor 1.2%; production |
| `chicken_racer_run_pass_alpha.png` | 1235×1274 | yes | Racer passing step; runtime anchor 3.1%; production |
| `chicken_racer_run_contact_right_alpha.png` | 1230×1278 | yes | Racer right-foot contact; runtime anchor 2.5%; production |
| `chicken_racer_jump_alpha.png` | 1024×1536 | yes | Racer jump; runtime anchor 11.6%; production |
| `chicken_racer_duck_alpha.png` | 1536×1024 | yes | Racer dive; runtime anchor 4.5%; production |
| `chicken_racer_hit_alpha.png` | 1194×1317 | yes | Racer collision; runtime anchor 2.8%; production |
| `chicken_racer_win_alpha.png` | 1024×1536 | yes | Racer victory; runtime anchor 5.9%; production |
| `chicken_golden_idle_alpha.png` | 1024×1536 | yes | Golden hero/shop idle; runtime `chicken_golden_idle.png`; production |
| `chicken_golden_run_contact_left_alpha.png` | 1254×1254 | yes | Golden left-foot contact; runtime anchor 8.8%; production |
| `chicken_golden_run_pass_alpha.png` | 1230×1278 | yes | Golden passing step; runtime anchor 4.1%; production |
| `chicken_golden_run_contact_right_alpha.png` | 1254×1254 | yes | Golden right-foot contact; runtime anchor 6.0%; production |
| `chicken_golden_jump_alpha.png` | 1024×1536 | yes | Golden jump; runtime anchor 11.5%; production |
| `chicken_golden_duck_alpha.png` | 1536×1024 | yes | Golden dive; runtime anchor 4.7%; production |
| `chicken_golden_hit_alpha.png` | 1206×1305 | yes | Golden collision; runtime anchor 4.6%; production |
| `chicken_golden_win_alpha.png` | 1024×1536 | yes | Golden victory; runtime anchor 5.8%; production |
| `achievement_first_crossing.png` | 1254×1254 | yes | Coop-road achievement; runtime `ui_achievement_first_crossing.png`; production |
| `achievement_road_tested.png` | 1310×1200 | yes | Road/flag achievement; runtime `ui_achievement_road_tested.png`; production |
| `achievement_golden_trail.png` | 1254×1254 | yes | Three Golden Eggs achievement; runtime `ui_achievement_golden_trail.png`; production |
| `achievement_corn_keeper.png` | 1230×1278 | yes | Corn collection achievement; runtime `ui_achievement_corn_keeper.png`; production |
| `achievement_coin_clucker.png` | 1254×1254 | yes | Coin collection achievement; runtime `ui_achievement_coin_clucker.png`; production |
| `achievement_fashion_flock.png` | 1254×1254 | yes | Cosmetic collection achievement; runtime `ui_achievement_fashion_flock.png`; production |
| `achievement_midway_rooster_alpha.png` | 1261×1247 | yes | Road 8 milestone badge; runtime `ui_achievement_midway_rooster.png`; production |
| `achievement_grand_coop_alpha.png` | 1263×1246 | yes | Road 15 completion badge; runtime `ui_achievement_grand_coop.png`; production |
| `achievement_egg_carton_alpha.png` | 1278×1230 | yes | Eight Golden Eggs badge; runtime `ui_achievement_egg_carton.png`; production |
| `achievement_crowned_rooster_alpha.png` | 1270×1239 | yes | All fifteen Golden Eggs badge; runtime `ui_achievement_crowned_rooster.png`; production |
| `achievement_harvest_hero_alpha.png` | 1278×1231 | yes | Lifetime Corn milestone badge; runtime `ui_achievement_harvest_hero.png`; production |
| `achievement_road_veteran_alpha.png` | 1272×1237 | yes | Twenty-five run milestone badge; runtime `ui_achievement_road_veteran.png`; production |
| `ui_icon_leaderboard.png` | 1254×1254 | yes | Gold road-podium and finish-flag navigation icon; runtime optimized to 512×512; production |

The 24 cosmetic runtime sprites are alpha-preserving PNGs capped at 896 px on the longest edge. They are decoded lazily by drawable id, so only frames actually visited by the selected cosmetic enter the renderer cache. All 12 achievement runtime copies are capped at 384 px. Earlier badge generation intermediates without the `_alpha` suffix are retained for provenance only; their rendered backgrounds were rejected for runtime use.

The generated roadblock-emblem candidates retained a baked checkerboard after a cleanup pass and were rejected. Lose results therefore use the supplied visual language plus the already-approved transparent `obstacle_low_barrier` art; no defective bitmap is copied to runtime resources.

Generated art uses a consistent premium saturated 2.5D mobile-arcade treatment, top-front daylight, warm orange rim, readable silhouette, and no text/watermark. V3 character/overlay generations were alpha-inspected; checkerboard preview pixels were removed with separate identity-preserving ImageGen edits before runtime copies were accepted. Prompt provenance is recorded in `art/generated/PROMPTS.md`.

## First compact APK resource overlays — 2026-09-03 (superseded size profile)

- Original `design/`, art masters, and all `src/main/res` PNGs remain unchanged.
- `tools/optimize_release_art.py` produces build-only WebP overlays registered with the Android release variant. Pillow with WebP support is required; override the Python executable with `-PassetPython=...` if necessary.
- The 85 PNGs total 55,878,900 bytes; their WebP copies total 23,117,550 bytes before resource shrinking. Resource names, pixel dimensions and alpha channels are unchanged and verified during generation, preserving crop, sprite anchors and touch-control layout.
- 51 sensitive images use lossless WebP. The remaining 34 use quality 94 (96 for UI/icons); a visible-channel error check selects lossless encoding whenever the mean absolute channel error exceeds 3/255.
- Generated files live under the AGP-managed `app/build/generated/res/optimizeReleaseArt`, with an inventory at `app/build/reports/release-art.json`. R8/resource shrinking removes unused resources from the final APK. Debug development builds continue using the original PNGs.

## Under-10-MB resource profile — 2026-09-03

- The same 85 original PNGs are encoded at their original pixel dimensions. Every alpha channel is compared bit-for-bit after WebP decode. No backgrounds, sprites, cosmetic poses, achievements, levels or audio are removed, and no sprite anchors or UI layouts change.
- WebP RGB quality is 94 for UI/launcher art, 90 for characters/props/backgrounds, and 86 for the asphalt texture. The previous automatic lossless-RGB fallback is removed; it inflated yellow/orange assets because it treated blue-channel error as equally perceptible as green/red.
- The converter reports alpha-weighted RGB error plus luminance-weighted mean absolute error (0.2126 R / 0.7152 G / 0.0722 B); it rejects an image above 4.5/255 weighted error or 12/255 in any composited channel. These numerical checks supplement, not replace, screenshot inspection.
- The resulting complete overlay pack is **8,018,502 bytes**, versus 23,117,550 bytes for the first profile and 55,878,900 bytes of original PNGs. The resource budget is 8,050,000 bytes; final signed APK and ZIP sizes are checked separately against **10,000,000 bytes**.
- The existing splash WebP, Ogg music and all native ABIs remain unchanged. Original `design/`, art masters and `src/main/res` assets remain untouched; only generated release overlays change.
