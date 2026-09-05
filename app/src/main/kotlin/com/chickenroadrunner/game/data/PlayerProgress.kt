package com.chickenroadrunner.game.data

import com.chickenroadrunner.game.game.RoadAid

enum class ChickenSkin(val displayName: String, val price: Int) {
    CLASSIC("Classic Chicken", 0),
    FARMER("Farmer Chicken", 80),
    RACER("Racer Chicken", 120),
    GOLDEN("Golden Chicken", 0),
}

data class LevelRecord(
    val levelId: Int,
    val bestTimeMillis: Long = 0L,
    val bestCoins: Int = 0,
    val bestCorn: Int = 0,
    val goldenEgg: Boolean = false,
    val clears: Int = 0,
)

data class PlayerProgress(
    val unlockedLevel: Int = 1,
    val coins: Int = 0,
    val corn: Int = 0,
    val goldenEggLevels: Set<Int> = emptySet(),
    val unlockedSkins: Set<ChickenSkin> = setOf(ChickenSkin.CLASSIC),
    val selectedSkin: ChickenSkin = ChickenSkin.CLASSIC,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val roadAidInventory: Map<RoadAid, Int> = emptyMap(),
    val armedRoadAid: RoadAid? = null,
    val totalRuns: Int = 0,
    val totalWins: Int = 0,
    val lifetimeCoins: Int = 0,
    val lifetimeCorn: Int = 0,
    val levelRecords: Map<Int, LevelRecord> = emptyMap(),
    val lastCommittedRunToken: String = "",
) {
    val allGoldenEggs: Boolean get() = goldenEggLevels.containsAll(setOf(1, 2, 3))
}
