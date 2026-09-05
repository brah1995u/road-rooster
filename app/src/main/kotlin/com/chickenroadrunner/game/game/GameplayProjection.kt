package com.chickenroadrunner.game.game

import kotlin.math.pow

data class NormalizedProjection(
    val x: Float,
    val y: Float,
    val scale: Float,
    val depth: Float,
)

object GameplayProjection {
    const val horizonY = 0.245f
    const val contactY = 0.885f
    const val roadBottomY = 1.03f

    // The supplied roadside layer is split and moved out by this amount at render
    // time. Its inner rails then follow the same broad, straight corridor as the
    // road instead of squeezing the asphalt into a needle at the horizon.
    const val roadsideLayerHalfShift = 0.084f
    const val roadTopHalfWidth = 0.12f
    const val roadBottomHalfWidth = 1.18f
    const val laneTopSpacing = 0.037f
    const val laneContactSpacing = 0.270f
    private const val depthExponent = 1.52f

    fun roadHalfWidthAt(normalizedY: Float): Float {
        val progress = ((normalizedY - horizonY) / (roadBottomY - horizonY)).coerceIn(0f, 1f)
        return roadTopHalfWidth + (roadBottomHalfWidth - roadTopHalfWidth) * progress
    }

    fun laneSpacingAt(normalizedY: Float): Float {
        // Keep each lane ray straight from the vanishing point through the player.
        // Deriving this from the much wider road shoulders reached the contact width
        // halfway down the screen and then turned the dividers vertical.
        val maximumProgress = (roadBottomY - horizonY) / (contactY - horizonY)
        val progress = ((normalizedY - horizonY) / (contactY - horizonY))
            .coerceIn(0f, maximumProgress)
        return laneTopSpacing + (laneContactSpacing - laneTopSpacing) * progress
    }

    fun project(
        relativeDistance: Float,
        lanePosition: Float,
        visibleAheadDistance: Float = GameTuning.visibleAheadDistance,
    ): NormalizedProjection {
        val normalized = (1f - relativeDistance / visibleAheadDistance).coerceIn(0f, 1.06f)
        val depth = normalized.pow(depthExponent)
        val y = horizonY + (contactY - horizonY) * depth
        val laneSpacing = laneSpacingAt(y)
        val scale = 0.16f + 0.84f * depth
        return NormalizedProjection(
            x = 0.5f + lanePosition * laneSpacing,
            y = y,
            scale = scale,
            depth = depth,
        )
    }
}
