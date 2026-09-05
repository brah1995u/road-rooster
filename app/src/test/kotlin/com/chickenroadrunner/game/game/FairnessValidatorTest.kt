package com.chickenroadrunner.game.game

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class FairnessValidatorTest {
    @Test
    fun acceptsThreeBlockedLanesBecauseEveryHazardIsJumpable() {
        val entities = Lane.entries.mapIndexed { index, lane ->
            SpawnSpec("block_$index", 10f, lane, EntityKind.HAY_BALE, ObstacleRule.BLOCK)
        } + listOf(
            SpawnSpec("egg", 15f, Lane.CENTER, EntityKind.GOLDEN_EGG),
            SpawnSpec("finish", 19f, Lane.CENTER, EntityKind.FINISH_COOP, ObstacleRule.FINISH),
        )
        val level = LevelDefinition(7, "Impossible", 20f, 10f, 0, entities, emptyList())
        assertTrue(FairnessValidator.validate(level).isEmpty())
    }

    @Test
    fun rejectsImpossibleRapidCrossLaneTransition() {
        val entities = listOf(
            SpawnSpec("left_1", 10f, Lane.LEFT, EntityKind.HAY_BALE, ObstacleRule.BLOCK),
            SpawnSpec("center_1", 10f, Lane.CENTER, EntityKind.HAY_BALE, ObstacleRule.BLOCK),
            SpawnSpec("right_1", 10f, Lane.RIGHT, EntityKind.HAY_BALE, ObstacleRule.BLOCK),
            SpawnSpec("left_2", 12f, Lane.LEFT, EntityKind.HAY_BALE, ObstacleRule.BLOCK),
            SpawnSpec("center_2", 12f, Lane.CENTER, EntityKind.HAY_BALE, ObstacleRule.BLOCK),
            SpawnSpec("right_2", 12f, Lane.RIGHT, EntityKind.HAY_BALE, ObstacleRule.BLOCK),
            SpawnSpec("egg", 16f, Lane.CENTER, EntityKind.GOLDEN_EGG),
            SpawnSpec("finish", 19f, Lane.CENTER, EntityKind.FINISH_COOP, ObstacleRule.FINISH),
        )
        val level = LevelDefinition(8, "Impossible Zigzag", 20f, 10f, 0, entities, emptyList())
        assertTrue(FairnessValidator.validate(level).any { "No fair temporal path" in it.message })
    }

    @Test
    fun authoredLevelsValidateAtMinimumAndMaximumPlannedSpeed() {
        assertTrue(FairnessValidator.validateAll().isEmpty())
    }

    @Test
    fun catalogContainsFifteenBoundedAuthoredRoads() {
        assertEquals(15, LevelCatalog.levels.size)
        LevelCatalog.levels.forEach { level ->
            val duration = level.length / level.baseSpeed
            assertTrue("${level.name} duration $duration", duration in 55f..75f)
            assertEquals(1, level.entities.count { it.kind == EntityKind.GOLDEN_EGG })
            assertTrue(level.patterns.isNotEmpty())
        }
    }
}
