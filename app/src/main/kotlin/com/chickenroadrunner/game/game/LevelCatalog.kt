package com.chickenroadrunner.game.game

object LevelCatalog {
    val levels: List<LevelDefinition> = listOf(
        firstCrossing(),
        firelineFarm(),
        coopRun(),
        extendedRoad(4, "Sunrise Sprint", 660f, 9.4f, 14, 0),
        extendedRoad(5, "Harvest Highway", 675f, 9.6f, 15, 1),
        extendedRoad(6, "Windmill Way", 690f, 9.8f, 16, 2),
        extendedRoad(7, "Barnyard Bend", 705f, 10.0f, 17, 3),
        extendedRoad(8, "Silo Dash", 720f, 10.2f, 18, 4),
        extendedRoad(9, "Ember Crossing", 735f, 10.4f, 19, 5),
        extendedRoad(10, "Golden Fields", 748f, 10.6f, 20, 6),
        extendedRoad(11, "Tractor Trail", 760f, 10.8f, 21, 7),
        extendedRoad(12, "Rooster Ridge", 772f, 11.0f, 22, 8),
        extendedRoad(13, "Firebreak Road", 782f, 11.2f, 23, 9),
        extendedRoad(14, "Midnight Market", 790f, 11.4f, 24, 10),
        extendedRoad(15, "Grand Coop Run", 800f, 11.6f, 25, 11),
    )

    fun byId(id: Int): LevelDefinition = levels.firstOrNull { it.id == id } ?: levels.first()

    private fun firstCrossing(): LevelDefinition = level(1, "First Crossing", 620f, 8.6f, 12) {
        coinLine(18f, Lane.CENTER, 6, 4f)
        cornLine(48f, Lane.LEFT, 4, 5f)
        coinLine(62f, Lane.RIGHT, 4, 4f)
        obstacle(82f, Lane.CENTER, EntityKind.HAY_BALE, ObstacleRule.JUMP, 2.2f)

        coinLine(124f, Lane.CENTER, 4, 4f)
        Lane.entries.forEach { lane ->
            obstacle(140f, lane, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 2.4f)
        }
        cornLine(143f, Lane.CENTER, 4, 4f)

        coinLine(188f, Lane.LEFT, 4, 4f)
        Lane.entries.forEach { lane ->
            obstacle(205f, lane, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 2.4f)
        }
        cornLine(208f, Lane.LEFT, 4, 4f)

        obstacle(275f, Lane.LEFT, EntityKind.CART, ObstacleRule.BLOCK, 2.1f)
        obstacle(275f, Lane.RIGHT, EntityKind.HAY_BALE, ObstacleRule.JUMP, 2.1f)
        coinLine(260f, Lane.CENTER, 6, 4f)

        rollingTire(330f, fromLane = -1.8f, toLane = 1.8f, duration = 1.8f, warning = 2.4f)
        traffic(390f, EntityKind.TRAFFIC_CAR, direction = 1, duration = 2.0f, warning = 2.5f)
        traffic(420f, EntityKind.TRAFFIC_TRUCK, direction = -1, duration = 2.2f, warning = 2.6f)
        cornLine(445f, Lane.CENTER, 4, 5f)

        coinLine(478f, Lane.RIGHT, 4, 4f)
        obstacle(495f, Lane.CENTER, EntityKind.HAY_BALE, ObstacleRule.JUMP, 2.0f)
        obstacle(495f, Lane.RIGHT, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 2.2f)
        goldenEgg(505f, Lane.RIGHT)
        obstacle(552f, Lane.LEFT, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 2.0f)
        coinLine(570f, Lane.CENTER, 6, 4f)
        finish(612f)

        pattern("runway", 0f, 45f, Lane.entries.toSet(), budget = 2.2f)
        pattern("lane_tutorial", 45f, 102f, setOf(Lane.LEFT, Lane.RIGHT), budget = 2.2f)
        pattern("jump_tutorial", 116f, 164f, Lane.entries.toSet(), PlayerAction.JUMP, 2.4f)
        pattern("dive_tutorial", 180f, 230f, Lane.entries.toSet(), PlayerAction.DUCK, 2.4f)
        pattern("single_safe_lane", 250f, 296f, setOf(Lane.CENTER), budget = 2.1f)
        pattern("moving_tire_intro", 312f, 350f, setOf(Lane.LEFT, Lane.RIGHT), budget = 2.4f)
        pattern("traffic_with_gap", 370f, 438f, setOf(Lane.LEFT, Lane.RIGHT), budget = 2.2f)
        pattern("golden_egg_choice", 470f, 525f, setOf(Lane.LEFT, Lane.RIGHT), PlayerAction.JUMP, 2.0f)
        pattern("finish_runway", 560f, 620f, Lane.entries.toSet(), budget = 2.0f)
    }

    private fun firelineFarm(): LevelDefinition = level(2, "Fireline Farm", 700f, 9.8f, 16) {
        coinLine(18f, Lane.LEFT, 6, 4f)
        obstacle(78f, Lane.LEFT, EntityKind.CONE, ObstacleRule.JUMP, 1.9f)
        obstacle(78f, Lane.CENTER, EntityKind.CONE, ObstacleRule.JUMP, 1.9f)
        cornLine(62f, Lane.RIGHT, 5, 4f)
        obstacle(138f, Lane.RIGHT, EntityKind.MANHOLE, ObstacleRule.JUMP, 1.9f)
        coinLine(122f, Lane.RIGHT, 5, 4f)
        obstacle(196f, Lane.CENTER, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.9f)
        cornLine(180f, Lane.CENTER, 5, 4f)
        obstacle(252f, Lane.LEFT, EntityKind.CART, ObstacleRule.BLOCK, 1.8f)
        obstacle(252f, Lane.RIGHT, EntityKind.HAY_BALE, ObstacleRule.JUMP, 1.8f)
        coinLine(234f, Lane.CENTER, 6, 4f)
        traffic(322f, EntityKind.TRAFFIC_CAR, 1, 1.9f, 2.2f)
        traffic(350f, EntityKind.TRAFFIC_CAR, -1, 1.9f, 2.2f)
        obstacle(400f, Lane.RIGHT, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 1.8f)
        obstacle(440f, Lane.LEFT, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.8f)
        cornLine(450f, Lane.LEFT, 6, 4f)
        rollingTire(500f, 1.8f, -1.8f, 1.6f, 2.0f)
        traffic(558f, EntityKind.TRAFFIC_TRUCK, -1, 2.0f, 2.2f)
        coinLine(590f, Lane.LEFT, 4, 4f)
        obstacle(610f, Lane.CENTER, EntityKind.CART, ObstacleRule.BLOCK, 1.8f)
        obstacle(610f, Lane.LEFT, EntityKind.MANHOLE, ObstacleRule.JUMP, 1.9f)
        goldenEgg(620f, Lane.LEFT)
        coinLine(650f, Lane.RIGHT, 7, 4f)
        finish(692f)

        pattern("double_cone_escape", 58f, 100f, setOf(Lane.RIGHT), budget = 1.9f)
        pattern("jump_then_dive", 120f, 216f, Lane.entries.toSet(), PlayerAction.JUMP, 1.9f)
        pattern("center_route", 232f, 270f, setOf(Lane.CENTER), budget = 1.8f)
        pattern("traffic_wave", 304f, 366f, setOf(Lane.LEFT, Lane.RIGHT), budget = 1.8f)
        pattern("action_alternation", 382f, 462f, Lane.entries.toSet(), budget = 1.8f)
        pattern("moving_tire", 484f, 520f, setOf(Lane.LEFT, Lane.RIGHT), budget = 1.8f)
        pattern("egg_route", 594f, 634f, setOf(Lane.LEFT, Lane.RIGHT), PlayerAction.JUMP, 1.8f)
    }

    private fun coopRun(): LevelDefinition = level(3, "Coop Run", 760f, 10.8f, 20) {
        coinLine(16f, Lane.CENTER, 6, 4f)
        obstacle(84f, Lane.RIGHT, EntityKind.HAY_BALE, ObstacleRule.JUMP, 1.6f)
        obstacle(126f, Lane.CENTER, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 1.6f)
        obstacle(170f, Lane.LEFT, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.6f)
        cornLine(182f, Lane.RIGHT, 6, 4f)
        obstacle(238f, Lane.LEFT, EntityKind.CART, ObstacleRule.BLOCK, 1.55f)
        obstacle(238f, Lane.CENTER, EntityKind.CART, ObstacleRule.BLOCK, 1.55f)
        traffic(304f, EntityKind.TRAFFIC_TRUCK, -1, 2.0f, 2.0f)
        traffic(334f, EntityKind.TRAFFIC_CAR, 1, 1.7f, 1.9f)
        rollingTire(382f, -1.8f, 1.8f, 1.5f, 1.8f)
        coinLine(405f, Lane.LEFT, 6, 4f)
        obstacle(452f, Lane.LEFT, EntityKind.MANHOLE, ObstacleRule.JUMP, 1.55f)
        obstacle(492f, Lane.CENTER, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.55f)
        cornLine(500f, Lane.CENTER, 8, 4f)
        obstacle(568f, Lane.CENTER, EntityKind.HAY_BALE, ObstacleRule.JUMP, 1.5f)
        obstacle(568f, Lane.RIGHT, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 1.6f)
        goldenEgg(578f, Lane.RIGHT)
        traffic(640f, EntityKind.TRAFFIC_TRUCK, 1, 1.9f, 1.9f)
        obstacle(682f, Lane.LEFT, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.5f)
        obstacle(716f, Lane.CENTER, EntityKind.MANHOLE, ObstacleRule.JUMP, 1.5f)
        coinLine(728f, Lane.RIGHT, 6, 4f)
        finish(752f)

        pattern("mixed_opening", 64f, 188f, Lane.entries.toSet(), budget = 1.6f)
        pattern("single_escape_right", 220f, 258f, setOf(Lane.RIGHT), budget = 1.55f)
        pattern("truck_crossing", 286f, 350f, setOf(Lane.LEFT, Lane.RIGHT), budget = 1.5f)
        pattern("rolling_transition", 366f, 400f, setOf(Lane.LEFT, Lane.RIGHT), budget = 1.45f)
        pattern("action_alternation", 434f, 516f, Lane.entries.toSet(), budget = 1.5f)
        pattern("golden_route", 550f, 594f, setOf(Lane.LEFT, Lane.RIGHT), PlayerAction.JUMP, 1.5f)
        pattern("final_gauntlet", 620f, 742f, Lane.entries.toSet(), budget = 1.35f)
    }

    /**
     * Twelve deterministic authored extensions. Each variation keeps generous,
     * individually introduced action beats and a deliberate Golden Egg branch;
     * there is no random spawner and every output is validated by the runtime
     * fairness graph before the engine accepts it.
     */
    private fun extendedRoad(
        id: Int,
        name: String,
        length: Float,
        speed: Float,
        cornTarget: Int,
        variation: Int,
    ): LevelDefinition = level(id, name, length, speed, cornTarget) {
        val lanes = Lane.entries
        val routeLane = lanes[variation % lanes.size]
        val jumpLane = lanes[(variation + 1) % lanes.size]
        val eggLane = lanes[(variation + 2) % lanes.size]
        val firstCorn = cornTarget / 2
        val secondCorn = cornTarget - firstCorn
        val jumpBeat = length * 0.13f
        val diveBeat = length * 0.22f
        val routeBeat = length * 0.31f
        val trafficBeat = length * 0.41f
        val fullJumpBeat = length * 0.50f
        val tireBeat = length * 0.60f
        val fullDiveBeat = length * 0.69f
        val secondTrafficBeat = length * 0.78f
        val eggBarrierBeat = length * 0.87f

        coinLine(18f, Lane.CENTER, 7, 4f)
        cornLine(length * 0.065f, routeLane, firstCorn, 3.5f)

        obstacle(jumpBeat, jumpLane, EntityKind.HAY_BALE, ObstacleRule.JUMP, 2.0f)
        coinLine(jumpBeat - 18f, jumpLane, 4, 4f)

        Lane.entries.forEach { lane ->
            obstacle(diveBeat, lane, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.9f)
        }

        Lane.entries.filter { it != routeLane }.forEach { lane ->
            obstacle(routeBeat, lane, EntityKind.CART, ObstacleRule.BLOCK, 1.9f)
        }
        coinLine(routeBeat - 18f, routeLane, 6, 3.5f)

        traffic(
            trafficBeat,
            if (variation % 2 == 0) EntityKind.TRAFFIC_CAR else EntityKind.TRAFFIC_TRUCK,
            if (variation % 2 == 0) 1 else -1,
            duration = 1.9f,
            warning = 2.2f,
        )

        Lane.entries.forEach { lane ->
            obstacle(fullJumpBeat, lane, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 1.8f)
        }

        rollingTire(
            tireBeat,
            fromLane = if (variation % 2 == 0) -1.45f else 1.45f,
            toLane = if (variation % 2 == 0) 1.45f else -1.45f,
            duration = 1.7f,
            warning = 2.0f,
        )
        cornLine(tireBeat + 16f, lanes[(variation + 1) % lanes.size], secondCorn, 3.5f)

        Lane.entries.forEach { lane ->
            obstacle(fullDiveBeat, lane, EntityKind.DUCK_GATE, ObstacleRule.DUCK, 1.8f)
        }

        traffic(
            secondTrafficBeat,
            if (variation % 3 == 0) EntityKind.TRAFFIC_TRUCK else EntityKind.TRAFFIC_CAR,
            if (variation % 2 == 0) -1 else 1,
            duration = 1.8f,
            warning = 2.1f,
        )

        obstacle(eggBarrierBeat, eggLane, EntityKind.LOW_BARRIER, ObstacleRule.JUMP, 1.8f)
        coinLine(eggBarrierBeat - 16f, eggLane, 4, 4f)
        goldenEgg(eggBarrierBeat + 10f, eggLane)
        coinLine(length * 0.925f, routeLane, 6, 3.5f)
        finish(length - 8f)

        pattern("opening_${id}", 0f, jumpBeat + 18f, Lane.entries.toSet(), PlayerAction.JUMP, 1.8f)
        pattern("dive_${id}", diveBeat - 20f, diveBeat + 20f, Lane.entries.toSet(), PlayerAction.DUCK, 1.7f)
        pattern("route_${id}", routeBeat - 20f, routeBeat + 20f, setOf(routeLane), budget = 1.65f)
        pattern("crossing_${id}", trafficBeat - 22f, trafficBeat + 22f, setOf(Lane.LEFT, Lane.RIGHT), budget = 1.6f)
        pattern("jump_all_${id}", fullJumpBeat - 20f, fullJumpBeat + 20f, Lane.entries.toSet(), PlayerAction.JUMP, 1.55f)
        pattern("tire_${id}", tireBeat - 20f, tireBeat + 20f, setOf(Lane.LEFT, Lane.RIGHT), budget = 1.5f)
        pattern("dive_all_${id}", fullDiveBeat - 20f, fullDiveBeat + 20f, Lane.entries.toSet(), PlayerAction.DUCK, 1.5f)
        pattern("egg_${id}", eggBarrierBeat - 20f, eggBarrierBeat + 24f, Lane.entries.toSet(), PlayerAction.JUMP, 1.45f)
    }

    private fun level(
        id: Int,
        name: String,
        length: Float,
        speed: Float,
        cornTarget: Int,
        block: LevelBuilder.() -> Unit,
    ): LevelDefinition = LevelBuilder(id, name, length, speed, cornTarget).apply(block).build()

    private class LevelBuilder(
        private val id: Int,
        private val name: String,
        private val length: Float,
        private val speed: Float,
        private val cornTarget: Int,
    ) {
        private val entities = mutableListOf<SpawnSpec>()
        private val patterns = mutableListOf<PatternDefinition>()
        private var nextId = 0

        fun obstacle(distance: Float, lane: Lane, kind: EntityKind, rule: ObstacleRule, warning: Float) {
            entities += SpawnSpec(
                id = "l${id}_e${nextId++}",
                worldDistance = distance,
                lane = lane,
                kind = kind,
                rule = rule,
                telegraph = TelegraphDefinition(rule.defaultTelegraphKind(), warning),
            )
        }

        fun traffic(distance: Float, kind: EntityKind, direction: Int, duration: Float, warning: Float) {
            // Vehicles remain visibly on the asphalt shoulders while crossing.
            val from = if (direction >= 0) -GameTuning.trafficLaneExtent else GameTuning.trafficLaneExtent
            val to = -from
            entities += SpawnSpec(
                id = "l${id}_e${nextId++}",
                worldDistance = distance,
                lane = Lane.CENTER,
                kind = kind,
                rule = ObstacleRule.MOVING_BLOCK,
                crossingDirection = direction,
                collisionProfile = CollisionProfile.MOVING_SOLID,
                motion = MotionDefinition(MotionType.TRAFFIC_CROSSING, from, to, duration, duration * 0.5f),
                telegraph = TelegraphDefinition(TelegraphKind.TRAFFIC, warning),
            )
        }

        fun rollingTire(distance: Float, fromLane: Float, toLane: Float, duration: Float, warning: Float) {
            entities += SpawnSpec(
                id = "l${id}_e${nextId++}",
                worldDistance = distance,
                lane = Lane.CENTER,
                kind = EntityKind.ROLLING_TIRE,
                rule = ObstacleRule.MOVING_BLOCK,
                collisionProfile = CollisionProfile.MOVING_SOLID,
                motion = MotionDefinition(MotionType.CROSS_LANE, fromLane, toLane, duration, duration * 0.5f),
                telegraph = TelegraphDefinition(TelegraphKind.TRAFFIC, warning),
            )
        }

        fun coinLine(start: Float, lane: Lane, count: Int, spacing: Float) = repeat(count) {
            entities += SpawnSpec("l${id}_e${nextId++}", start + it * spacing, lane, EntityKind.COIN)
        }

        fun cornLine(start: Float, lane: Lane, count: Int, spacing: Float) = repeat(count) {
            entities += SpawnSpec("l${id}_e${nextId++}", start + it * spacing, lane, EntityKind.CORN)
        }

        fun goldenEgg(distance: Float, lane: Lane) {
            entities += SpawnSpec("l${id}_egg", distance, lane, EntityKind.GOLDEN_EGG)
        }

        fun finish(distance: Float) {
            entities += SpawnSpec(
                id = "l${id}_finish",
                worldDistance = distance,
                lane = Lane.CENTER,
                kind = EntityKind.FINISH_COOP,
                rule = ObstacleRule.FINISH,
                collisionProfile = CollisionProfile.FINISH,
            )
        }

        fun pattern(
            id: String,
            start: Float,
            end: Float,
            safe: Set<Lane>,
            action: PlayerAction? = null,
            budget: Float,
        ) {
            patterns += PatternDefinition(
                id = id,
                startDistance = start,
                endDistance = end,
                safeLanes = safe,
                requiredAction = action,
                entryState = MotionState.GROUNDED,
                exitState = MotionState.GROUNDED,
                entryLanes = Lane.entries.toSet(),
                exitLanes = safe,
                minimumApproachDistance = speed * budget,
                minimumSpeed = speed * GameTuning.runSpeedMultiplier,
                maximumSpeed = speed * GameTuning.runSpeedMultiplier *
                    (1f + GameTuning.runProgressSpeedIncrease),
            )
        }

        fun build() = LevelDefinition(
            id = id,
            name = name,
            length = length,
            baseSpeed = speed,
            cornTarget = cornTarget,
            entities = entities.sortedBy { it.worldDistance },
            patterns = patterns.sortedBy { it.startDistance },
        )
    }
}
