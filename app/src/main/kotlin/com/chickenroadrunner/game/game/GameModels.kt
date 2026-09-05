package com.chickenroadrunner.game.game

import kotlin.math.roundToInt

enum class Lane(val coordinate: Int) {
    LEFT(-1), CENTER(0), RIGHT(1);

    fun shifted(delta: Int): Lane = entries.minBy { kotlin.math.abs(it.coordinate - (coordinate + delta).coerceIn(-1, 1)) }

    companion object {
        fun nearest(value: Float): Lane = entries.minBy { kotlin.math.abs(it.coordinate - value) }
    }
}

enum class RunPhase { READY, RUNNING, PAUSED, WON, LOST }

enum class PlayerAction { MOVE_LEFT, MOVE_RIGHT, JUMP, DUCK }

enum class RoadAid(val price: Int) {
    FEATHER_GUARD(45),
    CORN_MAGNET(30),
    WING_BOOST(35),
}

enum class PlayerPose { RUN, HOP_LEFT, HOP_RIGHT, JUMP, DUCK, HIT, WIN }

enum class MotionState { GROUNDED, AIRBORNE, DIVING }

enum class ActionWindow { NONE, JUMP_CLEAR, DIVE_CLEAR }

enum class CollisionProfile {
    PICKUP,
    TALL_BLOCK,
    JUMPABLE,
    OVERHEAD,
    MOVING_SOLID,
    FINISH,
}

enum class MotionType { STATIC, CROSS_LANE, TRAFFIC_CROSSING }

data class MotionDefinition(
    val type: MotionType = MotionType.STATIC,
    val fromLane: Float = 0f,
    val toLane: Float = 0f,
    val durationSeconds: Float = 1f,
    val startLeadSeconds: Float = 0f,
)

enum class TelegraphKind { NONE, BLOCKED, JUMP, DIVE, TRAFFIC }

data class TelegraphDefinition(
    val kind: TelegraphKind = TelegraphKind.NONE,
    val leadSeconds: Float = 0f,
)

enum class EntityKind {
    COIN,
    CORN,
    GOLDEN_EGG,
    HAY_BALE,
    LOW_BARRIER,
    DUCK_GATE,
    CONE,
    MANHOLE,
    CART,
    ROLLING_TIRE,
    TRAFFIC_CAR,
    TRAFFIC_TRUCK,
    FINISH_COOP,
}

enum class ObstacleRule { NONE, BLOCK, JUMP, DUCK, MOVING_BLOCK, FINISH }

data class SpawnSpec(
    val id: String,
    val worldDistance: Float,
    val lane: Lane,
    val kind: EntityKind,
    val rule: ObstacleRule = ObstacleRule.NONE,
    val crossingDirection: Int = 1,
    val crossingOffset: Float = 0f,
    val collisionProfile: CollisionProfile = rule.defaultCollisionProfile(),
    val motion: MotionDefinition = MotionDefinition(),
    val telegraph: TelegraphDefinition = TelegraphDefinition(rule.defaultTelegraphKind(), if (rule == ObstacleRule.NONE) 0f else 1.8f),
)

data class PatternDefinition(
    val id: String,
    val startDistance: Float,
    val endDistance: Float,
    val safeLanes: Set<Lane>,
    val requiredAction: PlayerAction? = null,
    val entryState: MotionState = MotionState.GROUNDED,
    val exitState: MotionState = MotionState.GROUNDED,
    val entryLanes: Set<Lane> = Lane.entries.toSet(),
    val exitLanes: Set<Lane> = safeLanes,
    val minimumApproachDistance: Float = 8f,
    val minimumSpeed: Float = 1f,
    val maximumSpeed: Float = Float.MAX_VALUE,
)

data class LevelDefinition(
    val id: Int,
    val name: String,
    val length: Float,
    val baseSpeed: Float,
    val cornTarget: Int,
    val entities: List<SpawnSpec>,
    val patterns: List<PatternDefinition>,
)

data class PlayerSnapshot(
    val lanePosition: Float,
    val lane: Lane,
    val height: Float,
    val duckAmount: Float,
    val pose: PlayerPose,
    val grounded: Boolean = true,
    val verticalOffset: Float = height,
    val laneHopProgress: Float = 0f,
    val actionWindow: ActionWindow = ActionWindow.NONE,
)

data class EntitySnapshot(
    val id: String,
    val kind: EntityKind,
    val rule: ObstacleRule,
    val relativeDistance: Float,
    val lanePosition: Float,
    val telegraph: Float,
    val collisionProfile: CollisionProfile = rule.defaultCollisionProfile(),
    val motionPhase: Float = 0f,
    val warningKind: TelegraphKind = TelegraphKind.NONE,
    val contactDistance: Float = relativeDistance,
    val motionDirection: Int = 1,
)

data class GameSnapshot(
    val phase: RunPhase = RunPhase.READY,
    val levelId: Int = 1,
    val levelName: String = "First Crossing",
    val distance: Float = 0f,
    val levelLength: Float = 1f,
    val speed: Float = 0f,
    val coins: Int = 0,
    val corn: Int = 0,
    val cornTarget: Int = 0,
    val goldenEgg: Boolean = false,
    val player: PlayerSnapshot = PlayerSnapshot(0f, Lane.CENTER, 0f, 0f, PlayerPose.RUN),
    val entities: List<EntitySnapshot> = emptyList(),
    val currentPattern: String = "ready",
    val activeRoadAid: RoadAid? = null,
    val roadAidAvailable: Boolean = false,
    val featherGuardActive: Boolean = false,
    val cornMagnetSeconds: Float = 0f,
    val wingBoostActive: Boolean = false,
) {
    val progress: Float get() = (distance / levelLength).coerceIn(0f, 1f)
    val progressPercent: Int get() = (progress * 100f).roundToInt()
}

data class RunResult(
    val levelId: Int,
    val won: Boolean,
    val coins: Int,
    val corn: Int,
    val goldenEgg: Boolean,
    val distance: Float,
    val elapsedSeconds: Float = 0f,
)

sealed interface GameEvent {
    data class Pickup(val kind: EntityKind) : GameEvent
    data class Collision(val kind: EntityKind) : GameEvent
    data class Action(val action: PlayerAction) : GameEvent
    data class TrafficWarning(val kind: EntityKind) : GameEvent
    data class RoadAidActivated(val aid: RoadAid) : GameEvent
    data class RoadAidUsed(val aid: RoadAid) : GameEvent
    data class Finished(val result: RunResult) : GameEvent
}

data class ValidationIssue(val levelId: Int, val message: String)

fun ObstacleRule.defaultCollisionProfile(): CollisionProfile = when (this) {
    ObstacleRule.NONE -> CollisionProfile.PICKUP
    ObstacleRule.BLOCK -> CollisionProfile.TALL_BLOCK
    ObstacleRule.JUMP -> CollisionProfile.JUMPABLE
    ObstacleRule.DUCK -> CollisionProfile.OVERHEAD
    ObstacleRule.MOVING_BLOCK -> CollisionProfile.MOVING_SOLID
    ObstacleRule.FINISH -> CollisionProfile.FINISH
}

fun ObstacleRule.defaultTelegraphKind(): TelegraphKind = when (this) {
    ObstacleRule.BLOCK -> TelegraphKind.BLOCKED
    ObstacleRule.JUMP -> TelegraphKind.JUMP
    ObstacleRule.DUCK -> TelegraphKind.DIVE
    ObstacleRule.MOVING_BLOCK -> TelegraphKind.TRAFFIC
    else -> TelegraphKind.NONE
}
