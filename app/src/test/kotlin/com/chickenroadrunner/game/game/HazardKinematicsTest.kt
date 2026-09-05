package com.chickenroadrunner.game.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HazardKinematicsTest {
    private val crossing = SpawnSpec(
        id = "crossing",
        worldDistance = 100f,
        lane = Lane.CENTER,
        kind = EntityKind.TRAFFIC_CAR,
        rule = ObstacleRule.MOVING_BLOCK,
        motion = MotionDefinition(MotionType.TRAFFIC_CROSSING, -1.9f, 1.9f, 2f, 1f),
        telegraph = TelegraphDefinition(TelegraphKind.TRAFFIC, 2.2f),
    )

    @Test
    fun oneShotTrafficStartsOffRoadCrossesAtContactAndLeaves() {
        assertEquals(-1.9f, HazardKinematics.sample(crossing, 90f, 10f).lanePosition, 0.001f)
        assertEquals(0f, HazardKinematics.sample(crossing, 100f, 10f).lanePosition, 0.001f)
        assertEquals(1.9f, HazardKinematics.sample(crossing, 110f, 10f).lanePosition, 0.001f)
    }

    @Test
    fun warningPhaseIsDeterministicFromContactTime() {
        assertEquals(0f, HazardKinematics.sample(crossing, 78f, 10f).warningPhase, 0.001f)
        assertEquals(0.5f, HazardKinematics.sample(crossing, 89f, 10f).warningPhase, 0.001f)
        assertEquals(1f, HazardKinematics.sample(crossing, 100f, 10f).warningPhase, 0.001f)
    }

    @Test
    fun physicsBoundsAreSmallerThanAFullLane() {
        assertTrue(HazardKinematics.overlapsPlayer(CollisionProfile.TALL_BLOCK, 0f, 0f))
        assertFalse(HazardKinematics.overlapsPlayer(CollisionProfile.TALL_BLOCK, 0f, 1f))
        assertFalse(HazardKinematics.overlapsPlayer(CollisionProfile.JUMPABLE, -1f, 0f))
    }

    @Test
    fun productionTrafficEndpointsStayInsideTheRenderedRoad() {
        val traffic = LevelCatalog.levels.flatMap { it.entities }
            .filter { it.motion.type == MotionType.TRAFFIC_CROSSING }
        assertTrue(traffic.isNotEmpty())
        traffic.forEach { spec ->
            assertTrue(kotlin.math.abs(spec.motion.fromLane) <= GameTuning.trafficLaneExtent)
            assertTrue(kotlin.math.abs(spec.motion.toLane) <= GameTuning.trafficLaneExtent)
            val left = GameplayProjection.project(0f, spec.motion.fromLane)
            val right = GameplayProjection.project(0f, spec.motion.toLane)
            val roadHalf = GameplayProjection.roadHalfWidthAt(GameplayProjection.contactY)
            assertTrue(kotlin.math.abs(left.x - 0.5f) < roadHalf)
            assertTrue(kotlin.math.abs(right.x - 0.5f) < roadHalf)
        }
    }
}
