package com.chickenroadrunner.game.game

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

data class FairnessTuning(
    val minimumSpeedFactor: Float = GameTuning.runSpeedMultiplier,
    val maximumSpeedFactor: Float = GameTuning.runSpeedMultiplier * (1f + GameTuning.runProgressSpeedIncrease),
    val checkpointWindow: Float = 1.5f,
)

object FairnessValidator {
    private data class ReachableState(val lane: Lane, val actionReadyAtDistance: Float)

    fun validate(
        level: LevelDefinition,
        tuning: FairnessTuning = FairnessTuning(),
    ): List<ValidationIssue> = buildList {
        if (level.length <= 0f) add(issue(level, "Level length must be positive"))
        if (level.baseSpeed <= 0f) add(issue(level, "Base speed must be positive"))
        if (level.entities.map { it.id }.toSet().size != level.entities.size) add(issue(level, "Entity ids must be unique"))
        if (level.entities.none { it.collisionProfile == CollisionProfile.FINISH }) add(issue(level, "Level needs a finish entity"))
        if (level.entities.count { it.kind == EntityKind.GOLDEN_EGG } != 1) add(issue(level, "Level must contain exactly one Golden Egg"))

        level.entities.filter { it.worldDistance !in 0f..level.length }.forEach {
            add(issue(level, "${it.id} is outside the level bounds"))
        }
        level.entities.filter { it.motion.type != MotionType.STATIC }.forEach {
            if (it.motion.durationSeconds <= 0f) add(issue(level, "${it.id} has invalid motion duration"))
            if (it.telegraph.leadSeconds < 1.35f) add(issue(level, "${it.id} has less than 1.35 s warning"))
        }

        level.patterns.forEach { pattern ->
            if (pattern.startDistance >= pattern.endDistance) add(issue(level, "${pattern.id} has an invalid distance range"))
            if (pattern.startDistance < 0f || pattern.endDistance > level.length) add(issue(level, "${pattern.id} is outside the level bounds"))
            if (pattern.safeLanes.isEmpty()) add(issue(level, "${pattern.id} declares no safe lane"))
            if (pattern.entryLanes.isEmpty() || pattern.exitLanes.isEmpty()) add(issue(level, "${pattern.id} has an empty entry/exit state"))
            val minimum = level.baseSpeed * tuning.minimumSpeedFactor
            val maximum = level.baseSpeed * tuning.maximumSpeedFactor
            // Products grouped in a different order can differ by a few ULPs.
            // Treat those float-rounding crumbs as the same authored range.
            val speedEpsilon = 0.001f
            if (pattern.minimumSpeed > minimum + speedEpsilon || pattern.maximumSpeed + speedEpsilon < maximum) {
                add(issue(level, "${pattern.id} does not cover the planned speed range"))
            }
        }

        if (level.baseSpeed > 0f) {
            listOf(
                level.baseSpeed * tuning.minimumSpeedFactor,
                level.baseSpeed * tuning.maximumSpeedFactor,
            ).distinct().forEach { speed ->
                temporalIssue(level, speed, tuning)?.let(::add)
            }
        }
    }.distinct()

    fun validateAll(
        levels: List<LevelDefinition> = LevelCatalog.levels,
        tuning: FairnessTuning = FairnessTuning(),
    ): List<ValidationIssue> = levels.flatMap { validate(it, tuning) }

    private fun temporalIssue(
        level: LevelDefinition,
        speed: Float,
        tuning: FairnessTuning,
    ): ValidationIssue? {
        val checkpoints = level.entities
            .filter { it.collisionProfile !in setOf(CollisionProfile.PICKUP, CollisionProfile.FINISH) }
            .groupBy { (it.worldDistance / tuning.checkpointWindow).roundToInt() }
            .values
            .sortedBy { group -> group.minOf { it.worldDistance } }

        var states = setOf(ReachableState(Lane.CENTER, 0f))
        var previousDistance = 0f
        checkpoints.forEach { hazards ->
            val checkpointDistance = hazards.minOf { it.worldDistance }
            val travelSeconds = ((checkpointDistance - previousDistance) / speed).coerceAtLeast(0f)
            val availableHops = floor(travelSeconds / GameTuning.laneHopSeconds).toInt()
            val expanded = states.flatMap { state ->
                Lane.entries.filter { candidate -> abs(candidate.coordinate - state.lane.coordinate) <= availableHops }
                    .map { candidate -> state.copy(lane = candidate) }
            }

            states = expanded.mapNotNull { state ->
                val overlapping = hazards.filter { hazard ->
                    val sample = HazardKinematics.sample(hazard, checkpointDistance, speed)
                    HazardKinematics.overlapsPlayer(hazard.collisionProfile, sample.lanePosition, state.lane.coordinate.toFloat())
                }
                if (overlapping.isEmpty()) return@mapNotNull state

                // Runtime permits a jump over every physical hazard. Validate
                // the same universal escape route so authored content and play
                // never disagree about what the player can clear.
                val leadSeconds = GameTuning.jumpClearStartSeconds
                val duration = GameTuning.jumpSeconds
                val latestStart = checkpointDistance - leadSeconds * speed
                if (state.actionReadyAtDistance > latestStart) return@mapNotNull null
                state.copy(actionReadyAtDistance = checkpointDistance + (duration - leadSeconds) * speed)
            }.groupBy { it.lane }.values.map { laneStates -> laneStates.minBy { it.actionReadyAtDistance } }.toSet()

            if (states.isEmpty()) {
                return issue(level, "No fair temporal path near $checkpointDistance at ${"%.2f".format(speed)} units/s")
            }
            previousDistance = checkpointDistance
        }
        return null
    }

    private fun issue(level: LevelDefinition, message: String) = ValidationIssue(level.id, message)
}
