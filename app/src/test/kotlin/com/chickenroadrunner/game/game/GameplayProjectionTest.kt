package com.chickenroadrunner.game.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayProjectionTest {
    @Test
    fun contactProjectionMatchesPlayerFeetAndLaneCenters() {
        val center = GameplayProjection.project(0f, Lane.CENTER.coordinate.toFloat())
        val left = GameplayProjection.project(0f, Lane.LEFT.coordinate.toFloat())
        val right = GameplayProjection.project(0f, Lane.RIGHT.coordinate.toFloat())

        assertEquals(0.5f, center.x, 0.0001f)
        assertEquals(GameplayProjection.contactY, center.y, 0.0001f)
        assertEquals(1f, center.scale, 0.0001f)
        assertEquals(GameplayProjection.laneContactSpacing, center.x - left.x, 0.0001f)
        assertEquals(GameplayProjection.laneContactSpacing, right.x - center.x, 0.0001f)
    }

    @Test
    fun farProjectionConvergesAtBroadHorizon() {
        val left = GameplayProjection.project(GameTuning.visibleAheadDistance, -1f)
        val right = GameplayProjection.project(GameTuning.visibleAheadDistance, 1f)

        assertEquals(GameplayProjection.horizonY, left.y, 0.0001f)
        assertEquals(GameplayProjection.laneTopSpacing * 2f, right.x - left.x, 0.0001f)
        assertEquals(0.16f, left.scale, 0.0001f)
    }

    @Test
    fun roadEdgesFollowTheSuppliedFenceCorridor() {
        assertEquals(0.120f, GameplayProjection.roadHalfWidthAt(0.245f), 0.001f)
        assertEquals(0.194f, GameplayProjection.roadHalfWidthAt(0.300f), 0.003f)
        assertEquals(0.329f, GameplayProjection.roadHalfWidthAt(0.400f), 0.003f)
        assertEquals(0.572f, GameplayProjection.roadHalfWidthAt(0.580f), 0.004f)
        assertTrue(GameplayProjection.roadHalfWidthAt(0.530f) >= 0.5f)
    }

    @Test
    fun laneCentersExpandWithOneStraightPerspectiveSlope() {
        assertEquals(GameplayProjection.laneTopSpacing, GameplayProjection.laneSpacingAt(0.245f), 0.001f)
        assertEquals(0.093f, GameplayProjection.laneSpacingAt(0.400f), 0.003f)
        assertEquals(0.130f, GameplayProjection.laneSpacingAt(0.500f), 0.003f)
        assertEquals(0.159f, GameplayProjection.laneSpacingAt(0.580f), 0.003f)
        assertEquals(GameplayProjection.laneContactSpacing, GameplayProjection.laneSpacingAt(GameplayProjection.contactY), 0.001f)

        val midpointY = (GameplayProjection.horizonY + GameplayProjection.contactY) / 2f
        val midpointSpacing = (GameplayProjection.laneTopSpacing + GameplayProjection.laneContactSpacing) / 2f
        assertEquals(midpointSpacing, GameplayProjection.laneSpacingAt(midpointY), 0.0001f)
        assertTrue(GameplayProjection.laneSpacingAt(GameplayProjection.roadBottomY) > GameplayProjection.laneContactSpacing)
    }
}
