package com.chickenroadrunner.game.game

data class HazardMotionSample(
    val lanePosition: Float,
    val motionPhase: Float,
    val warningPhase: Float,
    val secondsToContact: Float,
)

object HazardKinematics {
    fun sample(
        spec: SpawnSpec,
        playerDistance: Float,
        speed: Float,
    ): HazardMotionSample {
        val safeSpeed = speed.coerceAtLeast(0.01f)
        val secondsToContact = (spec.worldDistance - playerDistance) / safeSpeed
        val motion = spec.motion
        val rawPhase = if (motion.type == MotionType.STATIC || motion.durationSeconds <= 0f) {
            0f
        } else {
            (motion.startLeadSeconds - secondsToContact) / motion.durationSeconds
        }
        val phase = rawPhase.coerceIn(0f, 1f)
        val eased = phase * phase * (3f - 2f * phase)
        val lane = when (motion.type) {
            MotionType.STATIC -> spec.lane.coordinate.toFloat()
            MotionType.CROSS_LANE, MotionType.TRAFFIC_CROSSING -> motion.fromLane + (motion.toLane - motion.fromLane) * eased
        }
        val lead = spec.telegraph.leadSeconds
        val warning = if (lead <= 0f) 0f else ((lead - secondsToContact) / lead).coerceIn(0f, 1f)
        return HazardMotionSample(lane, phase, warning, secondsToContact)
    }

    fun hazardHalfLane(profile: CollisionProfile): Float = when (profile) {
        CollisionProfile.PICKUP -> 0f
        CollisionProfile.TALL_BLOCK -> 0.29f
        CollisionProfile.JUMPABLE -> 0.26f
        CollisionProfile.OVERHEAD -> 0.30f
        CollisionProfile.MOVING_SOLID -> 0.31f
        CollisionProfile.FINISH -> 1.6f
    }

    fun overlapsPlayer(profile: CollisionProfile, hazardLane: Float, playerLane: Float): Boolean {
        return kotlin.math.abs(hazardLane - playerLane) <= hazardHalfLane(profile) + GameTuning.playerLaneRadius
    }
}
