package com.chickenroadrunner.game.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureResolverTest {
    @Test
    fun ignoresMovementBelowThreshold() {
        assertNull(GestureResolver.resolve(20f, -22f, 34f))
    }

    @Test
    fun dominantAxisChoosesLaneOrVerticalAction() {
        assertEquals(PlayerAction.MOVE_LEFT, GestureResolver.resolve(-80f, 35f, 34f))
        assertEquals(PlayerAction.MOVE_RIGHT, GestureResolver.resolve(80f, -35f, 34f))
        assertEquals(PlayerAction.JUMP, GestureResolver.resolve(35f, -80f, 34f))
        assertEquals(PlayerAction.DUCK, GestureResolver.resolve(-35f, 80f, 34f))
    }
}
