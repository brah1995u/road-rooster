package com.chickenroadrunner.game.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class GameEngine {
    private data class RuntimeEntity(
        val spec: SpawnSpec,
        var consumed: Boolean = false,
        var collisionResolved: Boolean = false,
        var pendingImpact: Boolean = false,
    )

    private var level: LevelDefinition = LevelCatalog.levels.first()
    private val entities = mutableListOf<RuntimeEntity>()
    private val events = ArrayDeque<GameEvent>()
    private val warnedHazards = mutableSetOf<String>()
    private var phase = RunPhase.READY
    private var elapsed = 0f
    private var distance = 0f
    private var coins = 0
    private var corn = 0
    private var goldenEgg = false
    private var lane = Lane.CENTER
    private var lanePosition = 0f
    private var hopFrom = 0f
    private var hopTo = 0f
    private var hopElapsed = GameTuning.laneHopSeconds
    private var hopping = false
    private var bufferedLaneDelta = 0
    private var jumpElapsed = GameTuning.jumpSeconds
    private var duckElapsed = GameTuning.duckSeconds
    private var bufferedVerticalAction: PlayerAction? = null
    private var verticalBufferRemaining = 0f
    private var result: RunResult? = null
    private var featherGuardAvailable = false
    private var cornMagnetRemaining = 0f
    private var wingBoostActive = false
    private var jumpIntentStartDistance = Float.NEGATIVE_INFINITY
    private var jumpIntentLanePosition = 0f

    fun start(level: LevelDefinition, roadAid: RoadAid? = null) {
        require(FairnessValidator.validate(level).isEmpty()) { "Level ${level.id} failed fairness validation" }
        this.level = level
        entities.clear()
        entities += level.entities.map(::RuntimeEntity)
        events.clear()
        warnedHazards.clear()
        phase = RunPhase.RUNNING
        elapsed = 0f
        distance = 0f
        coins = 0
        corn = 0
        goldenEgg = false
        lane = Lane.CENTER
        lanePosition = 0f
        hopFrom = 0f
        hopTo = 0f
        hopElapsed = GameTuning.laneHopSeconds
        hopping = false
        bufferedLaneDelta = 0
        jumpElapsed = GameTuning.jumpSeconds
        duckElapsed = GameTuning.duckSeconds
        bufferedVerticalAction = null
        verticalBufferRemaining = 0f
        result = null
        featherGuardAvailable = false
        cornMagnetRemaining = 0f
        wingBoostActive = false
        roadAid?.let { activateRoadAidInternal(it, emitEvent = false) }
        jumpIntentStartDistance = Float.NEGATIVE_INFINITY
        jumpIntentLanePosition = 0f
    }

    fun restart() = start(level)

    fun pause() {
        if (phase == RunPhase.RUNNING) phase = RunPhase.PAUSED
    }

    fun resume() {
        if (phase == RunPhase.PAUSED) phase = RunPhase.RUNNING
    }

    fun submit(action: PlayerAction) {
        if (phase != RunPhase.RUNNING) return
        when (action) {
            PlayerAction.MOVE_LEFT -> requestLane(-1)
            PlayerAction.MOVE_RIGHT -> requestLane(1)
            PlayerAction.JUMP, PlayerAction.DUCK -> {
                if (isGrounded()) startVerticalAction(action)
                else {
                    bufferedVerticalAction = action
                    verticalBufferRemaining = GameTuning.verticalInputBufferSeconds
                }
            }
        }
    }

    fun activateRoadAid(aid: RoadAid): Boolean {
        if (phase != RunPhase.RUNNING) return false
        return activateRoadAidInternal(aid, emitEvent = true)
    }

    fun update(deltaSeconds: Float) {
        if (phase != RunPhase.RUNNING) return
        val dt = deltaSeconds.coerceIn(0f, GameTuning.maxFrameDeltaSeconds)
        elapsed += dt
        distance += currentSpeed() * dt
        if (cornMagnetRemaining > 0f) cornMagnetRemaining = (cornMagnetRemaining - dt).coerceAtLeast(0f)
        updatePlayer(dt)
        emitWarnings()
        collectPickups()
        checkCollisions()
        if (phase == RunPhase.RUNNING && distance >= level.length) finish(won = true)
    }

    fun snapshot(): GameSnapshot {
        val speed = currentSpeed()
        val visible = entities.asSequence()
            .filterNot { it.consumed }
            .map { runtime ->
                val spec = runtime.spec
                val motion = HazardKinematics.sample(spec, distance, speed)
                EntitySnapshot(
                    id = spec.id,
                    kind = spec.kind,
                    rule = spec.rule,
                    relativeDistance = spec.worldDistance - distance,
                    lanePosition = motion.lanePosition,
                    telegraph = motion.warningPhase,
                    collisionProfile = spec.collisionProfile,
                    motionPhase = motion.motionPhase,
                    warningKind = spec.telegraph.kind,
                    contactDistance = spec.worldDistance - distance,
                    motionDirection = spec.crossingDirection,
                )
            }
            .filter { it.relativeDistance in -GameTuning.visibleBehindDistance..GameTuning.visibleAheadDistance }
            .sortedByDescending { it.relativeDistance }
            .toList()

        val height = jumpHeight()
        return GameSnapshot(
            phase = phase,
            levelId = level.id,
            levelName = level.name,
            distance = distance,
            levelLength = level.length,
            speed = speed,
            coins = coins,
            corn = corn,
            cornTarget = level.cornTarget,
            goldenEgg = goldenEgg,
            player = PlayerSnapshot(
                lanePosition = lanePosition,
                lane = lane,
                height = height,
                duckAmount = duckAmount(),
                pose = playerPose(),
                grounded = isGrounded(),
                verticalOffset = height,
                laneHopProgress = hopProgress(),
                actionWindow = actionWindow(),
            ),
            entities = visible,
            currentPattern = level.patterns.lastOrNull { distance >= it.startDistance }?.id ?: "runway",
            activeRoadAid = when {
                featherGuardAvailable -> RoadAid.FEATHER_GUARD
                cornMagnetRemaining > 0f -> RoadAid.CORN_MAGNET
                wingBoostActive -> RoadAid.WING_BOOST
                else -> null
            },
            roadAidAvailable = featherGuardAvailable || cornMagnetRemaining > 0f || wingBoostActive,
            featherGuardActive = featherGuardAvailable,
            cornMagnetSeconds = cornMagnetRemaining,
            wingBoostActive = wingBoostActive,
        )
    }

    fun drainEvents(): List<GameEvent> = buildList {
        while (events.isNotEmpty()) add(events.removeFirst())
    }

    fun runResult(): RunResult? = result

    private fun requestLane(delta: Int) {
        if (hopping) {
            bufferedLaneDelta = delta
            return
        }
        val next = lane.shifted(delta)
        if (next == lane) return
        hopFrom = lanePosition
        hopTo = next.coordinate.toFloat()
        lane = next
        hopElapsed = 0f
        hopping = true
        events += GameEvent.Action(if (delta < 0) PlayerAction.MOVE_LEFT else PlayerAction.MOVE_RIGHT)
    }

    private fun updatePlayer(dt: Float) {
        if (hopping) {
            hopElapsed += dt
            val t = hopProgress()
            val eased = t * t * (3f - 2f * t)
            lanePosition = hopFrom + (hopTo - hopFrom) * eased
            if (t >= 1f) {
                lanePosition = hopTo
                hopping = false
                val buffered = bufferedLaneDelta
                bufferedLaneDelta = 0
                if (buffered != 0) requestLane(buffered)
            }
        }
        if (isJumping()) jumpElapsed += dt
        if (isDucking()) duckElapsed += dt
        if (!isJumping()) wingBoostActive = false

        if (bufferedVerticalAction != null) {
            verticalBufferRemaining -= dt
            if (isGrounded() && verticalBufferRemaining > 0f) {
                val action = bufferedVerticalAction
                bufferedVerticalAction = null
                verticalBufferRemaining = 0f
                action?.let(::startVerticalAction)
            } else if (verticalBufferRemaining <= 0f) {
                bufferedVerticalAction = null
            }
        }
    }

    private fun startVerticalAction(action: PlayerAction) {
        when (action) {
            PlayerAction.JUMP -> {
                jumpElapsed = 0f
                jumpIntentStartDistance = distance
                jumpIntentLanePosition = lanePosition
            }
            PlayerAction.DUCK -> duckElapsed = 0f
            else -> return
        }
        events += GameEvent.Action(action)
    }

    private fun collectPickups() {
        val speed = currentSpeed()
        entities.forEach { runtime ->
            val spec = runtime.spec
            if (runtime.consumed || spec.collisionProfile != CollisionProfile.PICKUP) return@forEach
            val longitudinal = abs(spec.worldDistance - distance)
            val motion = HazardKinematics.sample(spec, distance, speed)
            val lateral = abs(motion.lanePosition - lanePosition)
            val magnetized = cornMagnetRemaining > 0f &&
                (spec.kind == EntityKind.COIN || spec.kind == EntityKind.CORN)
            val longitudinalRadius = if (magnetized) GameTuning.pickupLongitudinalRadius * 1.55f else GameTuning.pickupLongitudinalRadius
            val laneRadius = if (magnetized) 1.05f else GameTuning.pickupLaneRadius
            if (longitudinal <= longitudinalRadius && lateral <= laneRadius) {
                runtime.consumed = true
                when (spec.kind) {
                    EntityKind.COIN -> coins += 1
                    EntityKind.CORN -> corn += 1
                    EntityKind.GOLDEN_EGG -> goldenEgg = true
                    else -> Unit
                }
                events += GameEvent.Pickup(spec.kind)
            }
        }
    }

    private fun emitWarnings() {
        val speed = currentSpeed()
        entities.forEach { runtime ->
            val spec = runtime.spec
            if (runtime.consumed || spec.id in warnedHazards || spec.telegraph.kind == TelegraphKind.NONE) return@forEach
            val warning = HazardKinematics.sample(spec, distance, speed).warningPhase
            if (warning > 0f) {
                warnedHazards += spec.id
                if (spec.collisionProfile == CollisionProfile.MOVING_SOLID) events += GameEvent.TrafficWarning(spec.kind)
            }
        }
    }

    private fun checkCollisions() {
        val speed = currentSpeed()
        entities.forEach { runtime ->
            if (runtime.consumed || runtime.collisionResolved || phase != RunPhase.RUNNING) return@forEach
            val spec = runtime.spec
            if (spec.collisionProfile == CollisionProfile.PICKUP) return@forEach
            val approach = spec.worldDistance - distance
            if (approach > 0f) return@forEach
            if (spec.collisionProfile == CollisionProfile.FINISH) {
                runtime.collisionResolved = true
                finish(won = true)
                return@forEach
            }

            val motion = HazardKinematics.sample(spec, distance, speed)
            val overlaps = HazardKinematics.overlapsPlayer(spec.collisionProfile, motion.lanePosition, lanePosition)
            val jumpClears = actionWindow() == ActionWindow.JUMP_CLEAR ||
                jumpIntentClears(spec, motion.lanePosition)
            val diveClears = spec.collisionProfile == CollisionProfile.OVERHEAD &&
                actionWindow() == ActionWindow.DIVE_CLEAR
            // Every physical road hazard is jumpable. Profile-specific actions
            // remain useful alternatives (lane change and dive), never traps.
            val avoided = !overlaps || jumpClears || diveClears
            if (avoided) {
                runtime.collisionResolved = true
                runtime.pendingImpact = false
                return@forEach
            }

            if (featherGuardAvailable) {
                featherGuardAvailable = false
                runtime.collisionResolved = true
                runtime.pendingImpact = false
                events += GameEvent.RoadAidUsed(RoadAid.FEATHER_GUARD)
                return@forEach
            }

            runtime.pendingImpact = true
            if (approach <= -speed * GameTuning.impactGraceSeconds) {
                runtime.collisionResolved = true
                events += GameEvent.Collision(spec.kind)
                finish(won = false)
            }
        }
    }

    private fun finish(won: Boolean) {
        if (phase == RunPhase.WON || phase == RunPhase.LOST) return
        phase = if (won) RunPhase.WON else RunPhase.LOST
        result = RunResult(level.id, won, coins, corn, goldenEgg, distance.coerceAtMost(level.length), elapsed)
        events += GameEvent.Finished(result!!)
    }

    private fun currentSpeed(): Float {
        val progress = (distance / level.length).coerceIn(0f, 1f)
        return level.baseSpeed * GameTuning.runSpeedMultiplier *
            (1f + progress * GameTuning.runProgressSpeedIncrease)
    }

    private fun activateRoadAidInternal(aid: RoadAid, emitEvent: Boolean): Boolean {
        val activated = when (aid) {
            RoadAid.FEATHER_GUARD -> {
                if (featherGuardAvailable) false else {
                    featherGuardAvailable = true
                    true
                }
            }
            RoadAid.CORN_MAGNET -> {
                if (cornMagnetRemaining > 0f) false else {
                    cornMagnetRemaining = GameTuning.cornMagnetDurationSeconds
                    true
                }
            }
            RoadAid.WING_BOOST -> {
                if (!isGrounded()) false else {
                    wingBoostActive = true
                    startVerticalAction(PlayerAction.JUMP)
                    true
                }
            }
        }
        if (activated && emitEvent) events += GameEvent.RoadAidActivated(aid)
        return activated
    }

    private fun jumpIntentClears(spec: SpawnSpec, hazardLane: Float): Boolean {
        val approachAtInput = spec.worldDistance - jumpIntentStartDistance
        if (approachAtInput !in 0f..GameTuning.jumpIntentWindowDistance) return false
        return HazardKinematics.overlapsPlayer(spec.collisionProfile, hazardLane, jumpIntentLanePosition)
    }

    private fun isJumping() = jumpElapsed < GameTuning.jumpSeconds
    private fun isDucking() = duckElapsed < GameTuning.duckSeconds
    private fun isGrounded() = !isJumping() && !isDucking()
    private fun hopProgress() = if (!hopping) 0f else (hopElapsed / GameTuning.laneHopSeconds).coerceIn(0f, 1f)

    private fun jumpHeight(): Float {
        if (!isJumping()) {
            return if (hopping) sin(hopProgress() * PI).toFloat() * GameTuning.laneHopHeight else 0f
        }
        val t = (jumpElapsed / GameTuning.jumpSeconds).coerceIn(0f, 1f)
        val heightMultiplier = if (wingBoostActive) GameTuning.wingBoostJumpHeightMultiplier else 1f
        return 4f * GameTuning.jumpHeight * heightMultiplier * t * (1f - t)
    }

    private fun duckAmount(): Float {
        if (!isDucking()) return 0f
        val exitStart = GameTuning.duckEnterSeconds + GameTuning.duckHoldSeconds
        return when {
            duckElapsed < GameTuning.duckEnterSeconds -> duckElapsed / GameTuning.duckEnterSeconds
            duckElapsed < exitStart -> 1f
            else -> 1f - ((duckElapsed - exitStart) / (GameTuning.duckSeconds - exitStart)).coerceIn(0f, 1f)
        }
    }

    private fun actionWindow(): ActionWindow = when {
        isJumping() &&
            jumpElapsed <= GameTuning.jumpClearEndSeconds &&
            jumpHeight() >= GameTuning.jumpClearMinHeight -> ActionWindow.JUMP_CLEAR
        isDucking() && duckElapsed <= (GameTuning.duckEnterSeconds + GameTuning.duckHoldSeconds + 0.05f) -> ActionWindow.DIVE_CLEAR
        else -> ActionWindow.NONE
    }

    private fun playerPose(): PlayerPose = when {
        phase == RunPhase.LOST -> PlayerPose.HIT
        phase == RunPhase.WON -> PlayerPose.WIN
        isJumping() -> PlayerPose.JUMP
        isDucking() -> PlayerPose.DUCK
        hopping && hopTo < hopFrom -> PlayerPose.HOP_LEFT
        hopping -> PlayerPose.HOP_RIGHT
        else -> PlayerPose.RUN
    }
}
