package com.chickenroadrunner.game.data

import com.chickenroadrunner.game.game.RunResult
import com.chickenroadrunner.game.game.LevelCatalog
import com.chickenroadrunner.game.game.RoadAid
import kotlin.math.roundToLong

object ProgressRules {
    fun applyRun(progress: PlayerProgress, token: String, result: RunResult): PlayerProgress {
        if (token == progress.lastCommittedRunToken) return progress
        val eggs = if (result.goldenEgg) progress.goldenEggLevels + result.levelId else progress.goldenEggLevels
        val nextLevel = if (result.won) {
            maxOf(progress.unlockedLevel, (result.levelId + 1).coerceAtMost(LevelCatalog.levels.size))
        } else {
            progress.unlockedLevel
        }
        val skins = if (eggs.containsAll(setOf(1, 2, 3))) progress.unlockedSkins + ChickenSkin.GOLDEN else progress.unlockedSkins
        val records = if (result.won) {
            val previous = progress.levelRecords[result.levelId] ?: LevelRecord(result.levelId)
            val finishMillis = (result.elapsedSeconds.coerceAtLeast(0f) * 1_000f).roundToLong()
            val bestTime = when {
                finishMillis <= 0L -> previous.bestTimeMillis
                previous.bestTimeMillis <= 0L -> finishMillis
                else -> minOf(previous.bestTimeMillis, finishMillis)
            }
            progress.levelRecords + (result.levelId to previous.copy(
                bestTimeMillis = bestTime,
                bestCoins = maxOf(previous.bestCoins, result.coins),
                bestCorn = maxOf(previous.bestCorn, result.corn),
                goldenEgg = previous.goldenEgg || result.goldenEgg,
                clears = previous.clears + 1,
            ))
        } else {
            progress.levelRecords
        }
        return progress.copy(
            unlockedLevel = nextLevel,
            coins = progress.coins + result.coins,
            corn = progress.corn + result.corn,
            goldenEggLevels = eggs,
            unlockedSkins = skins,
            totalRuns = progress.totalRuns + 1,
            totalWins = progress.totalWins + if (result.won) 1 else 0,
            lifetimeCoins = progress.lifetimeCoins + result.coins,
            lifetimeCorn = progress.lifetimeCorn + result.corn,
            levelRecords = records,
            lastCommittedRunToken = token,
        )
    }

    fun purchase(progress: PlayerProgress, skin: ChickenSkin): PlayerProgress {
        if (skin in progress.unlockedSkins || progress.coins < skin.price || skin == ChickenSkin.GOLDEN) return progress
        return progress.copy(coins = progress.coins - skin.price, unlockedSkins = progress.unlockedSkins + skin)
    }

    fun purchaseRoadAid(progress: PlayerProgress, aid: RoadAid): PlayerProgress {
        if (progress.coins < aid.price) return progress
        val nextCount = progress.roadAidInventory.getOrDefault(aid, 0) + 1
        return progress.copy(
            coins = progress.coins - aid.price,
            roadAidInventory = progress.roadAidInventory + (aid to nextCount),
        )
    }

    fun armRoadAid(progress: PlayerProgress, aid: RoadAid?): PlayerProgress {
        if (aid == null) return progress.copy(armedRoadAid = null)
        return if (progress.roadAidInventory.getOrDefault(aid, 0) > 0) progress.copy(armedRoadAid = aid) else progress
    }

    fun consumeRoadAid(progress: PlayerProgress, aid: RoadAid): PlayerProgress {
        val count = progress.roadAidInventory.getOrDefault(aid, 0)
        if (count <= 0) return progress.copy(armedRoadAid = null)
        val nextInventory = if (count == 1) progress.roadAidInventory - aid else progress.roadAidInventory + (aid to count - 1)
        return progress.copy(
            roadAidInventory = nextInventory,
            armedRoadAid = progress.armedRoadAid.takeIf { nextInventory.getOrDefault(it, 0) > 0 },
        )
    }
}
