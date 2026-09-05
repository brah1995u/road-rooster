package com.chickenroadrunner.game.data

object LevelRecordsCodec {
    fun encode(records: Map<Int, LevelRecord>): String = records.values
        .filter { it.levelId > 0 && it.clears > 0 }
        .sortedBy { it.levelId }
        .joinToString(";") { record ->
            listOf(
                record.levelId,
                record.bestTimeMillis.coerceAtLeast(0L),
                record.bestCoins.coerceAtLeast(0),
                record.bestCorn.coerceAtLeast(0),
                if (record.goldenEgg) 1 else 0,
                record.clears.coerceAtLeast(0),
            ).joinToString(":")
        }

    fun decode(value: String): Map<Int, LevelRecord> = value.split(';')
        .mapNotNull { entry ->
            val parts = entry.split(':')
            val levelId = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val clears = parts.getOrNull(5)?.toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
            if (clears == 0) return@mapNotNull null
            levelId to LevelRecord(
                levelId = levelId,
                bestTimeMillis = parts.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                bestCoins = parts.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                bestCorn = parts.getOrNull(3)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                goldenEgg = parts.getOrNull(4) == "1",
                clears = clears,
            )
        }
        .toMap()
}
