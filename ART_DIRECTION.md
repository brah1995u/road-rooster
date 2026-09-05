# Road Rooster — Art Direction

## Source of truth

The untouched files in `design/` and the approved portrait gameplay reference define the visual language. The `black.jpg` promotional composition is a mood reference only; its multiplier/wager graphics are not gameplay requirements.

## Style

- Premium casual-game 2.5D rendering with large readable silhouettes.
- Gold/yellow/orange glossy UI, dark brown outlines, cream panels, saturated blue sky, dark asphalt, warm fire accents.
- Rounded forms, polished highlights, tactile materials, soft contact shadows, controlled bloom.
- Objects share a slightly elevated front-facing perspective suitable for a road receding toward the horizon.

## Character

The chicken is white, round, expressive, and prominent near the bottom of the screen without hiding the nearest hazard. Run poses are capped at 56% of screen width (40% while diving) and embedded 3 dp into the contact plane. Red comb/wattle and orange feet provide recognition at small scale. The run cycle uses contact-left, passing, and contact-right identity-preserved sprites at 7–9 fps. Each sprite has a measured alpha-bound foot anchor; squash and bob pivot around the contact point so a planted foot never appears to hover.

## Production rules

- Generated gameplay objects use transparent backgrounds and contain no text or watermarks.
- Runtime art is trimmed, consistently anchored, and decoded before gameplay.
- UI text remains code-rendered for exact spelling and responsive layout.
- Procedural particles supplement art; they do not cover hazards or pickups.
- The opaque horizon/farm layer contains field terrain only: no baked road and no baked fences. Code-rendered asphalt, the transparent roadside/guardrail layer, and shoulder flame accents remain separate and share one projection. Fire is restricted to road shoulders and does not repeat through crop fields.
- Asphalt texture sampling and dashed lane dividers are locked to the road/guardrails. Animated asphalt specks and distance-shifted divider dashes were removed completely; forward motion comes from gameplay entities, preventing any road element from sliding sideways near the chicken. Lane spacing follows one uninterrupted perspective slope from the horizon through the player instead of clamping halfway down the screen.
- Traffic art is bottom-anchored by measured transparent padding and mirrored from its authored motion direction; its wheel contact and lateral path use the same road projection as hazards and lane guides.
- The scene camera locks the visual horizon to 24.5% and the chicken foot contact to 88.5% of the gameplay viewport.
