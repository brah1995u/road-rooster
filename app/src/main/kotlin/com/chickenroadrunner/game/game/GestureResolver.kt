package com.chickenroadrunner.game.game

import kotlin.math.abs

object GestureResolver {
    fun resolve(
        deltaX: Float,
        deltaY: Float,
        threshold: Float,
        dominanceRatio: Float = GameTuning.gestureDominanceRatio,
    ): PlayerAction? {
        if (maxOf(abs(deltaX), abs(deltaY)) < threshold) return null
        val horizontal = abs(deltaX)
        val vertical = abs(deltaY)
        if (horizontal < vertical * dominanceRatio && vertical < horizontal * dominanceRatio) return null
        return if (horizontal > vertical) {
            if (deltaX < 0f) PlayerAction.MOVE_LEFT else PlayerAction.MOVE_RIGHT
        } else {
            if (deltaY < 0f) PlayerAction.JUMP else PlayerAction.DUCK
        }
    }
}
