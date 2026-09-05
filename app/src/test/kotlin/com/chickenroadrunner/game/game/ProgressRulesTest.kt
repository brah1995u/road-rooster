package com.chickenroadrunner.game.game

import com.chickenroadrunner.game.data.ChickenSkin
import com.chickenroadrunner.game.data.PlayerProgress
import com.chickenroadrunner.game.data.ProgressRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {
    @Test
    fun runCommitIsIdempotentAndUnlocksNextLevel() {
        val result = RunResult(1, true, 7, 4, true, 20f, 65.432f)
        val first = ProgressRules.applyRun(PlayerProgress(), "run-a", result)
        val second = ProgressRules.applyRun(first, "run-a", result)
        assertEquals(first, second)
        assertEquals(2, first.unlockedLevel)
        assertEquals(7, first.coins)
        assertEquals(7, first.lifetimeCoins)
        assertEquals(4, first.lifetimeCorn)
        assertEquals(1, first.totalRuns)
        assertEquals(1, first.totalWins)
        assertEquals(65_432L, first.levelRecords.getValue(1).bestTimeMillis)
        assertTrue(1 in first.goldenEggLevels)
    }

    @Test
    fun collectingAllEggsUnlocksGoldenChicken() {
        var progress = PlayerProgress(goldenEggLevels = setOf(1, 2))
        progress = ProgressRules.applyRun(progress, "run-c", RunResult(3, true, 0, 0, true, 10f))
        assertTrue(ChickenSkin.GOLDEN in progress.unlockedSkins)
    }

    @Test
    fun cosmeticPurchaseSpendsCoinsAndNeverChangesStats() {
        val progress = PlayerProgress(coins = 100)
        val bought = ProgressRules.purchase(progress, ChickenSkin.FARMER)
        assertEquals(20, bought.coins)
        assertTrue(ChickenSkin.FARMER in bought.unlockedSkins)
        assertEquals(progress.unlockedLevel, bought.unlockedLevel)
    }

    @Test
    fun roadAidPurchaseArmAndConsumptionAreAtomicRules() {
        val bought = ProgressRules.purchaseRoadAid(PlayerProgress(coins = 100), RoadAid.FEATHER_GUARD)
        assertEquals(55, bought.coins)
        assertEquals(1, bought.roadAidInventory[RoadAid.FEATHER_GUARD])

        val armed = ProgressRules.armRoadAid(bought, RoadAid.FEATHER_GUARD)
        assertEquals(RoadAid.FEATHER_GUARD, armed.armedRoadAid)

        val consumed = ProgressRules.consumeRoadAid(armed, RoadAid.FEATHER_GUARD)
        assertEquals(0, consumed.roadAidInventory.getOrDefault(RoadAid.FEATHER_GUARD, 0))
        assertEquals(null, consumed.armedRoadAid)
    }

    @Test
    fun roadAidCannotBeBoughtWithoutCoinsOrArmedWithoutInventory() {
        val progress = PlayerProgress(coins = 10)
        assertEquals(progress, ProgressRules.purchaseRoadAid(progress, RoadAid.CORN_MAGNET))
        assertEquals(progress, ProgressRules.armRoadAid(progress, RoadAid.CORN_MAGNET))
    }

    @Test
    fun everyInRunBoostCanBePurchasedAndConsumedIndependently() {
        var progress = PlayerProgress(coins = RoadAid.entries.sumOf { it.price })
        RoadAid.entries.forEach { aid -> progress = ProgressRules.purchaseRoadAid(progress, aid) }
        assertEquals(0, progress.coins)
        assertTrue(RoadAid.entries.all { progress.roadAidInventory.getOrDefault(it, 0) == 1 })

        RoadAid.entries.forEach { aid -> progress = ProgressRules.consumeRoadAid(progress, aid) }
        assertTrue(RoadAid.entries.all { progress.roadAidInventory.getOrDefault(it, 0) == 0 })
    }

    @Test
    fun leaderboardKeepsFastestTimeAndBestPickupsPerLevel() {
        val first = ProgressRules.applyRun(
            PlayerProgress(),
            "record-a",
            RunResult(2, true, 5, 8, false, 700f, 72.5f),
        )
        val second = ProgressRules.applyRun(
            first,
            "record-b",
            RunResult(2, true, 9, 4, true, 700f, 68.25f),
        )
        val record = second.levelRecords.getValue(2)
        assertEquals(68_250L, record.bestTimeMillis)
        assertEquals(9, record.bestCoins)
        assertEquals(8, record.bestCorn)
        assertTrue(record.goldenEgg)
        assertEquals(2, record.clears)
        assertEquals(2, second.totalRuns)
        assertEquals(2, second.totalWins)
    }

    @Test
    fun lossesCountAsRunsButNeverCreateFinishRecords() {
        val progress = ProgressRules.applyRun(
            PlayerProgress(),
            "loss-a",
            RunResult(1, false, 2, 1, false, 120f, 14f),
        )
        assertEquals(1, progress.totalRuns)
        assertEquals(0, progress.totalWins)
        assertTrue(progress.levelRecords.isEmpty())
        assertEquals(2, progress.lifetimeCoins)
        assertEquals(1, progress.lifetimeCorn)
    }
}
