package com.chickenroadrunner.game.game

import com.chickenroadrunner.game.data.LevelRecord
import com.chickenroadrunner.game.data.LevelRecordsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRecordsCodecTest {
    @Test
    fun recordMapRoundTripsWithoutLosingBestValues() {
        val records = mapOf(
            1 to LevelRecord(1, 65_432L, 12, 7, true, 3),
            8 to LevelRecord(8, 70_125L, 18, 11, false, 1),
        )
        assertEquals(records, LevelRecordsCodec.decode(LevelRecordsCodec.encode(records)))
    }

    @Test
    fun malformedAndZeroClearEntriesAreIgnored() {
        val decoded = LevelRecordsCodec.decode("bad;2:62000:4:3:1:0;3:71000:8:5:0:2")
        assertEquals(setOf(3), decoded.keys)
        assertTrue(decoded.getValue(3).bestTimeMillis > 0L)
    }
}
