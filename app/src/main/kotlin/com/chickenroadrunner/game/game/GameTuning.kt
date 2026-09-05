package com.chickenroadrunner.game.game

object GameTuning {
    const val fixedStepSeconds = 1f / 60f
    const val maxFrameDeltaSeconds = 0.05f
    const val laneHopSeconds = 0.18f
    const val laneHopHeight = 0.12f
    // Keep the jump visually high but brief: clearance comes from the action
    // window and intent forgiveness, not from leaving the chicken airborne.
    const val jumpSeconds = 0.90f
    const val jumpHeight = 1.42f
    // Clearance follows visible lift instead of an arbitrary late timer. The lower
    // threshold lets a swipe made as the feet reach the barrier still succeed.
    const val jumpClearMinHeight = 0.06f
    const val jumpClearStartSeconds = 0.008f
    const val jumpClearEndSeconds = 0.89f
    const val jumpIntentWindowDistance = 28f
    const val wingBoostJumpHeightMultiplier = 1.35f
    const val cornMagnetDurationSeconds = 12f
    const val duckSeconds = 0.62f
    const val duckEnterSeconds = 0.10f
    const val duckHoldSeconds = 0.36f
    const val playerLaneRadius = 0.18f
    const val pickupLaneRadius = 0.48f
    const val pickupLongitudinalRadius = 1.25f
    const val impactGraceSeconds = 0.18f
    const val verticalInputBufferSeconds = 0.16f
    const val visibleAheadDistance = 108f
    const val visibleBehindDistance = 1.5f
    const val runSpeedMultiplier = 1.08f
    const val runProgressSpeedIncrease = 0.12f
    const val gestureThresholdDp = 18f
    const val gestureThresholdScreenRatio = 0.035f
    const val gestureDominanceRatio = 1.15f
    const val trafficLaneExtent = 1.45f
    const val playerRunSpriteHeightScreenWidth = 0.56f
    const val playerDuckSpriteHeightScreenWidth = 0.40f
    const val playerGroundEmbedDp = 3f
    // Finish is a full-road landmark, not a lane-sized obstacle. At contact its
    // open arch visually spans the three lane centers and reads as a real gate.
    const val finishGateHeightFactor = 4.05f
    const val finishGateShadowScale = 2.8f
}
