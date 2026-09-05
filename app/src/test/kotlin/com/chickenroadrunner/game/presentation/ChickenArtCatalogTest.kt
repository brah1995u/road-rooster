package com.chickenroadrunner.game.presentation

import com.chickenroadrunner.game.data.ChickenSkin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChickenArtCatalogTest {
    @Test
    fun everySkinHasACompleteDistinctSpriteSet() {
        val sets = ChickenSkin.entries.map { it.artSet() }

        assertEquals(ChickenSkin.entries.size, sets.map { it.idleRes }.toSet().size)
        sets.forEach { art ->
            val frames = listOf(
                art.runContactLeft,
                art.runPass,
                art.runContactRight,
                art.jump,
                art.duck,
                art.hit,
                art.win,
            )
            assertEquals(frames.size, frames.map { it.drawableRes }.toSet().size)
            assertTrue(frames.all { it.drawableRes != 0 })
            assertTrue(frames.all { it.footPaddingRatio in 0f..0.13f })
        }
    }

    @Test
    fun cosmeticSetsDoNotFallBackToClassicFrames() {
        val classicResources = ChickenSkin.CLASSIC.artSet().frameResources()

        ChickenSkin.entries.filterNot { it == ChickenSkin.CLASSIC }.forEach { skin ->
            assertTrue(skin.artSet().frameResources().intersect(classicResources).isEmpty())
        }
    }

    private fun ChickenArtSet.frameResources(): Set<Int> = setOf(
        idleRes,
        runContactLeft.drawableRes,
        runPass.drawableRes,
        runContactRight.drawableRes,
        jump.drawableRes,
        duck.drawableRes,
        hit.drawableRes,
        win.drawableRes,
    )
}
