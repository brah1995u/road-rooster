package com.chickenroadrunner.game.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    @Test
    fun authoredLevelsPassFairnessValidation() {
        assertTrue(FairnessValidator.validateAll().joinToString { it.message }, FairnessValidator.validateAll().isEmpty())
    }

    @Test
    fun laneMovementStopsAtRoadBoundsAndBuffersOneMove() {
        val engine = GameEngine()
        engine.start(testLevel())
        engine.submit(PlayerAction.MOVE_LEFT)
        engine.submit(PlayerAction.MOVE_LEFT)
        advance(engine, 0.6f)
        assertEquals(Lane.LEFT, engine.snapshot().player.lane)
        assertEquals(-1f, engine.snapshot().player.lanePosition, 0.001f)
    }

    @Test
    fun jumpClearsJumpHazard() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("jump", 4f, Lane.CENTER, EntityKind.LOW_BARRIER, ObstacleRule.JUMP)))
        engine.submit(PlayerAction.JUMP)
        advance(engine, 0.48f)
        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
    }

    @Test
    fun jumpClearsEveryPhysicalHazardProfile() {
        val hazards = listOf(
            SpawnSpec("block", 4f, Lane.CENTER, EntityKind.CART, ObstacleRule.BLOCK),
            SpawnSpec("jump", 4f, Lane.CENTER, EntityKind.LOW_BARRIER, ObstacleRule.JUMP),
            SpawnSpec("gate", 4f, Lane.CENTER, EntityKind.DUCK_GATE, ObstacleRule.DUCK),
            SpawnSpec("traffic", 4f, Lane.CENTER, EntityKind.TRAFFIC_CAR, ObstacleRule.MOVING_BLOCK),
        )

        hazards.forEach { hazard ->
            val engine = GameEngine().also { it.start(testLevel(hazard)) }
            engine.submit(PlayerAction.JUMP)
            advance(engine, 0.7f)
            assertEquals("Failed to jump ${hazard.collisionProfile}", RunPhase.RUNNING, engine.snapshot().phase)
        }
    }

    @Test
    fun jumpIsBriefAndPassedObstacleLeavesTheSceneQuickly() {
        val hazard = SpawnSpec("quick_pass", 4f, Lane.CENTER, EntityKind.CART, ObstacleRule.BLOCK)
        val engine = GameEngine().also { it.start(testLevel(hazard)) }
        engine.submit(PlayerAction.JUMP)
        advance(engine, 1.0f)

        assertTrue(engine.snapshot().player.grounded)
        assertTrue(engine.snapshot().entities.none { it.id == hazard.id })
    }

    @Test
    fun jumpHazardCausesLossWithoutJump() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("jump", 4f, Lane.CENTER, EntityKind.LOW_BARRIER, ObstacleRule.JUMP)))
        advance(engine, 0.82f)
        assertEquals(RunPhase.LOST, engine.snapshot().phase)
    }

    @Test
    fun jumpStartedAtBarrierEdgeClearsDuringImpactGrace() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("late_jump", 4f, Lane.CENTER, EntityKind.LOW_BARRIER, ObstacleRule.JUMP)))
        advance(engine, 0.39f)
        engine.submit(PlayerAction.JUMP)
        advance(engine, 0.18f)

        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
        assertEquals(ActionWindow.JUMP_CLEAR, engine.snapshot().player.actionWindow)
    }

    @Test
    fun visibleJumpLiftAndClearanceBeginTogether() {
        val engine = GameEngine().also { it.start(testLevel()) }
        engine.submit(PlayerAction.JUMP)
        advance(engine, 0.06f)

        assertTrue(engine.snapshot().player.height >= GameTuning.jumpClearMinHeight)
        assertEquals(ActionWindow.JUMP_CLEAR, engine.snapshot().player.actionWindow)
    }

    @Test
    fun duckClearsOverheadHazard() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("gate", 3f, Lane.CENTER, EntityKind.DUCK_GATE, ObstacleRule.DUCK)))
        engine.submit(PlayerAction.DUCK)
        advance(engine, 0.4f)
        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
    }

    @Test
    fun diveStartedAtGateEdgeClearsDuringImpactGrace() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("late_dive", 4f, Lane.CENTER, EntityKind.DUCK_GATE, ObstacleRule.DUCK)))
        advance(engine, 0.39f)
        engine.submit(PlayerAction.DUCK)
        advance(engine, 0.20f)

        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
        assertEquals(ActionWindow.DIVE_CLEAR, engine.snapshot().player.actionWindow)
    }

    @Test
    fun jumpAndDiveExposeOnlyTheirClearanceWindows() {
        val jumpEngine = GameEngine().also { it.start(testLevel()) }
        jumpEngine.submit(PlayerAction.JUMP)
        advance(jumpEngine, 0.20f)
        assertEquals(ActionWindow.JUMP_CLEAR, jumpEngine.snapshot().player.actionWindow)
        assertFalse(jumpEngine.snapshot().player.grounded)
        advance(jumpEngine, GameTuning.jumpSeconds)
        assertTrue(jumpEngine.snapshot().player.grounded)

        val diveEngine = GameEngine().also { it.start(testLevel()) }
        diveEngine.submit(PlayerAction.DUCK)
        advance(diveEngine, 0.12f)
        assertEquals(ActionWindow.DIVE_CLEAR, diveEngine.snapshot().player.actionWindow)
        advance(diveEngine, 0.52f)
        assertTrue(diveEngine.snapshot().player.grounded)
    }

    @Test
    fun hopCanClearColliderDuringImpactGrace() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("block", 4f, Lane.CENTER, EntityKind.HAY_BALE, ObstacleRule.BLOCK)))
        advance(engine, 0.35f)
        engine.submit(PlayerAction.MOVE_LEFT)
        advance(engine, 0.24f)

        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
        assertEquals(Lane.LEFT, engine.snapshot().player.lane)
    }

    @Test
    fun pickupsCollectExactlyOnce() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("coin", 2f, Lane.CENTER, EntityKind.COIN)))
        advance(engine, 0.5f)
        assertEquals(1, engine.snapshot().coins)
        advance(engine, 0.5f)
        assertEquals(1, engine.snapshot().coins)
        assertEquals(1, engine.drainEvents().filterIsInstance<GameEvent.Pickup>().size)
    }

    @Test
    fun pauseFreezesDistanceAndResumeContinues() {
        val engine = GameEngine()
        engine.start(testLevel())
        advance(engine, 0.2f)
        engine.pause()
        val pausedDistance = engine.snapshot().distance
        advance(engine, 1f)
        assertEquals(pausedDistance, engine.snapshot().distance, 0.001f)
        engine.resume()
        advance(engine, 0.2f)
        assertTrue(engine.snapshot().distance > pausedDistance)
    }

    @Test
    fun deltaIsClampedAfterResumeSpike() {
        val engine = GameEngine()
        engine.start(testLevel())
        engine.update(10f)
        assertTrue(engine.snapshot().distance < 1f)
    }

    @Test
    fun trafficWarningIsEmittedOnceBeforeCrossing() {
        val engine = GameEngine()
        engine.start(testLevel(SpawnSpec("traffic", 20f, Lane.CENTER, EntityKind.TRAFFIC_CAR, ObstacleRule.MOVING_BLOCK)))
        advance(engine, 0.24f)
        assertEquals(1, engine.drainEvents().filterIsInstance<GameEvent.TrafficWarning>().size)
        engine.update(GameTuning.fixedStepSeconds)
        assertTrue(engine.drainEvents().none { it is GameEvent.TrafficWarning })
    }

    @Test
    fun finishCreatesOneWinningTerminalEvent() {
        val engine = GameEngine()
        engine.start(testLevel())
        advance(engine, 2.2f)
        assertEquals(RunPhase.WON, engine.snapshot().phase)
        assertEquals(1, engine.drainEvents().filterIsInstance<GameEvent.Finished>().size)
        advance(engine, 1f)
        assertTrue(engine.drainEvents().none { it is GameEvent.Finished })
    }

    @Test
    fun firstCrossingHasACompletableGoldenEggRoute() {
        val engine = GameEngine()
        engine.start(LevelCatalog.byId(1))
        var movedLeft = false
        var jumpedTutorial = false
        var returnedCenter = false
        var divedTutorial = false
        var movedForTraffic = false
        var choseEggLane = false
        var crossedToEggLane = false
        var jumpedForEgg = false
        var failureAt = "none"

        repeat(4_200) {
            val snapshot = engine.snapshot()
            if (!movedLeft && snapshot.distance >= 52f) {
                engine.submit(PlayerAction.MOVE_LEFT)
                movedLeft = true
            }
            if (!jumpedTutorial && snapshot.distance >= 136f) {
                engine.submit(PlayerAction.JUMP)
                jumpedTutorial = true
            }
            if (!returnedCenter && snapshot.distance >= 180f) {
                engine.submit(PlayerAction.MOVE_RIGHT)
                returnedCenter = true
            }
            if (!divedTutorial && snapshot.distance >= 201f) {
                engine.submit(PlayerAction.DUCK)
                divedTutorial = true
            }
            if (!movedForTraffic && snapshot.distance >= 300f) {
                engine.submit(PlayerAction.MOVE_LEFT)
                movedForTraffic = true
            }
            if (!choseEggLane && snapshot.distance >= 445f) {
                engine.submit(PlayerAction.MOVE_RIGHT)
                choseEggLane = true
            }
            if (!crossedToEggLane && snapshot.distance >= 448f) {
                engine.submit(PlayerAction.MOVE_RIGHT)
                crossedToEggLane = true
            }
            if (!jumpedForEgg && snapshot.distance >= 491f) {
                engine.submit(PlayerAction.JUMP)
                jumpedForEgg = true
            }
            engine.update(GameTuning.fixedStepSeconds)
            engine.drainEvents().filterIsInstance<GameEvent.Collision>().firstOrNull()?.let {
                failureAt = "${it.kind} at ${engine.snapshot().distance}, lane ${engine.snapshot().player.lanePosition}"
            }
            if (engine.snapshot().phase != RunPhase.RUNNING) return@repeat
        }

        val result = engine.runResult()
        assertEquals(failureAt, RunPhase.WON, engine.snapshot().phase)
        assertTrue(result?.won == true)
        assertTrue(result?.goldenEgg == true)
        assertTrue((result?.corn ?: 0) >= 4)
    }

    @Test
    fun hayBalesAreClearlyJumpableInAuthoredContent() {
        val hay = LevelCatalog.levels.flatMap { it.entities }.filter { it.kind == EntityKind.HAY_BALE }
        assertTrue(hay.isNotEmpty())
        assertTrue(hay.all { it.collisionProfile == CollisionProfile.JUMPABLE && it.rule == ObstacleRule.JUMP })
    }

    @Test
    fun firstLargeHayBaleCanBeJumpedFromTheCenterLane() {
        val engine = GameEngine().also { it.start(LevelCatalog.byId(1)) }
        while (engine.snapshot().distance < 60f) engine.update(GameTuning.fixedStepSeconds)
        engine.submit(PlayerAction.JUMP)
        while (engine.snapshot().distance < 90f && engine.snapshot().phase == RunPhase.RUNNING) {
            engine.update(GameTuning.fixedStepSeconds)
        }
        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
        assertTrue(engine.snapshot().distance >= 90f)
    }

    @Test
    fun jumpArcIsHighAndForgivingForLargePhoneObstacles() {
        val engine = GameEngine().also { it.start(testLevel()) }
        engine.submit(PlayerAction.JUMP)
        advance(engine, GameTuning.jumpSeconds / 2f)

        assertTrue(engine.snapshot().player.height >= 1.30f)
        assertEquals(ActionWindow.JUMP_CLEAR, engine.snapshot().player.actionWindow)
        assertTrue(GameTuning.jumpIntentWindowDistance >= 28f)
    }

    @Test
    fun featherGuardForgivesExactlyOneCollision() {
        val engine = GameEngine()
        engine.start(
            testLevel(
                SpawnSpec("guarded", 4f, Lane.CENTER, EntityKind.CART, ObstacleRule.BLOCK),
                SpawnSpec("unguarded", 9f, Lane.CENTER, EntityKind.CART, ObstacleRule.BLOCK),
            ),
            RoadAid.FEATHER_GUARD,
        )
        advance(engine, 0.70f)
        assertEquals(RunPhase.RUNNING, engine.snapshot().phase)
        assertFalse(engine.snapshot().roadAidAvailable)
        assertEquals(1, engine.drainEvents().filterIsInstance<GameEvent.RoadAidUsed>().size)

        advance(engine, 0.55f)
        assertEquals(RunPhase.LOST, engine.snapshot().phase)
    }

    @Test
    fun cornMagnetReachesAdjacentCoinButNotGoldenEgg() {
        val engine = GameEngine()
        engine.start(
            testLevel(
                SpawnSpec("side_coin", 4f, Lane.LEFT, EntityKind.COIN),
            ),
            RoadAid.CORN_MAGNET,
        )
        advance(engine, 1.5f)
        assertEquals(1, engine.snapshot().coins)
        assertFalse(engine.snapshot().goldenEgg)
    }

    @Test
    fun allThreeRoadAidsCanBeActivatedDuringARun() {
        val engine = GameEngine().also { it.start(testLevel()) }

        assertTrue(engine.activateRoadAid(RoadAid.FEATHER_GUARD))
        assertTrue(engine.snapshot().featherGuardActive)
        assertFalse(engine.activateRoadAid(RoadAid.FEATHER_GUARD))

        assertTrue(engine.activateRoadAid(RoadAid.CORN_MAGNET))
        assertTrue(engine.snapshot().cornMagnetSeconds > 11f)
        assertFalse(engine.activateRoadAid(RoadAid.CORN_MAGNET))

        assertTrue(engine.activateRoadAid(RoadAid.WING_BOOST))
        advance(engine, GameTuning.jumpSeconds / 2f)
        assertTrue(engine.snapshot().wingBoostActive)
        assertTrue(engine.snapshot().player.height > GameTuning.jumpHeight)
        assertFalse(engine.activateRoadAid(RoadAid.WING_BOOST))

        assertEquals(3, engine.drainEvents().filterIsInstance<GameEvent.RoadAidActivated>().size)
    }

    @Test
    fun runtimeCornMagnetCollectsFromAnAdjacentLaneAndExpires() {
        val engine = GameEngine().also {
            it.start(testLevel(SpawnSpec("runtime_side_coin", 4f, Lane.LEFT, EntityKind.COIN)))
        }
        assertTrue(engine.activateRoadAid(RoadAid.CORN_MAGNET))
        advance(engine, 0.55f)
        assertEquals(1, engine.snapshot().coins)

        val longLevel = LevelDefinition(
            id = 98,
            name = "Long",
            length = 200f,
            baseSpeed = 10f,
            cornTarget = 0,
            entities = listOf(
                SpawnSpec("long_egg", 2f, Lane.RIGHT, EntityKind.GOLDEN_EGG),
                SpawnSpec("long_finish", 199f, Lane.CENTER, EntityKind.FINISH_COOP, ObstacleRule.FINISH),
            ),
            patterns = emptyList(),
        )
        engine.start(longLevel)
        assertTrue(engine.activateRoadAid(RoadAid.CORN_MAGNET))
        advance(engine, GameTuning.cornMagnetDurationSeconds + 0.2f)
        assertEquals(0f, engine.snapshot().cornMagnetSeconds, 0.001f)
    }

    private fun testLevel(vararg extras: SpawnSpec): LevelDefinition {
        val entities = buildList {
            addAll(extras)
            add(SpawnSpec("egg", 14f, Lane.RIGHT, EntityKind.GOLDEN_EGG))
            add(SpawnSpec("finish", 19f, Lane.CENTER, EntityKind.FINISH_COOP, ObstacleRule.FINISH))
        }
        return LevelDefinition(99, "Test", 20f, 10f, 0, entities.sortedBy { it.worldDistance }, emptyList())
    }

    private fun advance(engine: GameEngine, seconds: Float) {
        repeat((seconds / GameTuning.fixedStepSeconds).toInt()) { engine.update(GameTuning.fixedStepSeconds) }
    }
}
