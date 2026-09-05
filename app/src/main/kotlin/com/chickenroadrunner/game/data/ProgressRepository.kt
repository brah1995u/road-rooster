package com.chickenroadrunner.game.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chickenroadrunner.game.game.RunResult
import com.chickenroadrunner.game.game.RoadAid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chickenRoadDataStore by preferencesDataStore(name = "chicken_road_progress")

class ProgressRepository(private val context: Context) {
    private object Keys {
        val unlockedLevel = intPreferencesKey("unlocked_level")
        val coins = intPreferencesKey("coins")
        val corn = intPreferencesKey("corn")
        val eggLevels = stringPreferencesKey("egg_levels")
        val unlockedSkins = stringPreferencesKey("unlocked_skins")
        val selectedSkin = stringPreferencesKey("selected_skin")
        val sound = booleanPreferencesKey("sound_enabled")
        val music = booleanPreferencesKey("music_enabled")
        val haptics = booleanPreferencesKey("haptics_enabled")
        val lastRun = stringPreferencesKey("last_run_token")
        val roadAids = stringPreferencesKey("road_aids")
        val armedRoadAid = stringPreferencesKey("armed_road_aid")
        val totalRuns = intPreferencesKey("total_runs")
        val totalWins = intPreferencesKey("total_wins")
        val lifetimeCoins = intPreferencesKey("lifetime_coins")
        val lifetimeCorn = intPreferencesKey("lifetime_corn")
        val levelRecords = stringPreferencesKey("level_records")
    }

    val progress: Flow<PlayerProgress> = context.chickenRoadDataStore.data.map(::decode)

    suspend fun commitRun(token: String, result: RunResult) {
        context.chickenRoadDataStore.edit { prefs ->
            val updated = ProgressRules.applyRun(decode(prefs), token, result)
            encode(prefs, updated)
        }
    }

    suspend fun purchase(skin: ChickenSkin) {
        context.chickenRoadDataStore.edit { prefs -> encode(prefs, ProgressRules.purchase(decode(prefs), skin)) }
    }

    suspend fun select(skin: ChickenSkin) {
        context.chickenRoadDataStore.edit { prefs ->
            val current = decode(prefs)
            if (skin in current.unlockedSkins) encode(prefs, current.copy(selectedSkin = skin))
        }
    }

    suspend fun purchaseRoadAid(aid: RoadAid) = update { ProgressRules.purchaseRoadAid(it, aid) }
    suspend fun armRoadAid(aid: RoadAid?) = update { ProgressRules.armRoadAid(it, aid) }
    suspend fun consumeRoadAid(aid: RoadAid) = update { ProgressRules.consumeRoadAid(it, aid) }

    suspend fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    suspend fun setMusic(enabled: Boolean) = update { it.copy(musicEnabled = enabled) }
    suspend fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }

    private suspend fun update(transform: (PlayerProgress) -> PlayerProgress) {
        context.chickenRoadDataStore.edit { prefs -> encode(prefs, transform(decode(prefs))) }
    }

    private fun decode(prefs: Preferences): PlayerProgress {
        val eggs = prefs[Keys.eggLevels].orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
        val skins = prefs[Keys.unlockedSkins].orEmpty().split(',').mapNotNull {
            runCatching { ChickenSkin.valueOf(it) }.getOrNull()
        }.toSet().ifEmpty { setOf(ChickenSkin.CLASSIC) }
        val selected = runCatching { ChickenSkin.valueOf(prefs[Keys.selectedSkin].orEmpty()) }
            .getOrDefault(ChickenSkin.CLASSIC)
            .takeIf { it in skins } ?: ChickenSkin.CLASSIC
        val roadAids = prefs[Keys.roadAids].orEmpty().split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            val aid = parts.getOrNull(0)?.let { runCatching { RoadAid.valueOf(it) }.getOrNull() }
            val count = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0)
            if (aid != null && count != null && count > 0) aid to count else null
        }.toMap()
        val armedRoadAid = prefs[Keys.armedRoadAid].orEmpty()
            .let { runCatching { RoadAid.valueOf(it) }.getOrNull() }
            ?.takeIf { roadAids.getOrDefault(it, 0) > 0 }
        val unlockedLevel = prefs[Keys.unlockedLevel] ?: 1
        val coins = prefs[Keys.coins] ?: 0
        val corn = prefs[Keys.corn] ?: 0
        val inferredWins = (unlockedLevel - 1).coerceAtLeast(0)
        val totalWins = maxOf(prefs[Keys.totalWins] ?: 0, inferredWins)
        val storedRecords = LevelRecordsCodec.decode(prefs[Keys.levelRecords].orEmpty())
        val legacyRecords = if (storedRecords.isEmpty() && unlockedLevel > 1) {
            (1 until unlockedLevel).associateWith { levelId ->
                LevelRecord(levelId = levelId, goldenEgg = levelId in eggs, clears = 1)
            }
        } else {
            storedRecords
        }
        return PlayerProgress(
            unlockedLevel = unlockedLevel,
            coins = coins,
            corn = corn,
            goldenEggLevels = eggs,
            unlockedSkins = skins,
            selectedSkin = selected,
            soundEnabled = prefs[Keys.sound] ?: true,
            musicEnabled = prefs[Keys.music] ?: true,
            hapticsEnabled = prefs[Keys.haptics] ?: true,
            roadAidInventory = roadAids,
            armedRoadAid = armedRoadAid,
            totalRuns = maxOf(prefs[Keys.totalRuns] ?: 0, totalWins),
            totalWins = totalWins,
            lifetimeCoins = maxOf(prefs[Keys.lifetimeCoins] ?: coins, coins),
            lifetimeCorn = maxOf(prefs[Keys.lifetimeCorn] ?: corn, corn),
            levelRecords = legacyRecords,
            lastCommittedRunToken = prefs[Keys.lastRun].orEmpty(),
        )
    }

    private fun encode(prefs: androidx.datastore.preferences.core.MutablePreferences, progress: PlayerProgress) {
        prefs[Keys.unlockedLevel] = progress.unlockedLevel
        prefs[Keys.coins] = progress.coins
        prefs[Keys.corn] = progress.corn
        prefs[Keys.eggLevels] = progress.goldenEggLevels.sorted().joinToString(",")
        prefs[Keys.unlockedSkins] = progress.unlockedSkins.sortedBy { it.ordinal }.joinToString(",") { it.name }
        prefs[Keys.selectedSkin] = progress.selectedSkin.name
        prefs[Keys.sound] = progress.soundEnabled
        prefs[Keys.music] = progress.musicEnabled
        prefs[Keys.haptics] = progress.hapticsEnabled
        prefs[Keys.roadAids] = progress.roadAidInventory.entries
            .filter { it.value > 0 }
            .sortedBy { it.key.ordinal }
            .joinToString(",") { "${it.key.name}:${it.value}" }
        prefs[Keys.armedRoadAid] = progress.armedRoadAid?.name.orEmpty()
        prefs[Keys.totalRuns] = progress.totalRuns
        prefs[Keys.totalWins] = progress.totalWins
        prefs[Keys.lifetimeCoins] = progress.lifetimeCoins
        prefs[Keys.lifetimeCorn] = progress.lifetimeCorn
        prefs[Keys.levelRecords] = LevelRecordsCodec.encode(progress.levelRecords)
        prefs[Keys.lastRun] = progress.lastCommittedRunToken
    }
}
