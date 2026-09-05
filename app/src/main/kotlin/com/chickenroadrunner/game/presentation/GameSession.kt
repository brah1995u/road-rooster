package com.chickenroadrunner.game.presentation

import android.view.Choreographer
import com.chickenroadrunner.game.game.GameEngine
import com.chickenroadrunner.game.game.GameEvent
import com.chickenroadrunner.game.game.GameSnapshot
import com.chickenroadrunner.game.game.GameTuning
import com.chickenroadrunner.game.game.LevelDefinition
import com.chickenroadrunner.game.game.PlayerAction
import com.chickenroadrunner.game.game.RunPhase
import com.chickenroadrunner.game.game.RoadAid

class GameSession(
    private val onHudSnapshot: (GameSnapshot) -> Unit,
    private val onEvents: (List<GameEvent>) -> Unit,
) : Choreographer.FrameCallback {
    private val engine = GameEngine()
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var lastFrameNanos = 0L
    private var accumulator = 0f
    private var hudElapsed = 0f
    private var invalidateView: (() -> Unit)? = null

    @Volatile
    var latestSnapshot: GameSnapshot = engine.snapshot()
        private set

    fun start(level: LevelDefinition, roadAid: RoadAid? = null) {
        engine.start(level, roadAid)
        latestSnapshot = engine.snapshot()
        onHudSnapshot(latestSnapshot)
        lastFrameNanos = 0L
        accumulator = 0f
        ensureLoop()
    }

    fun restart() {
        engine.restart()
        latestSnapshot = engine.snapshot()
        onHudSnapshot(latestSnapshot)
        lastFrameNanos = 0L
        accumulator = 0f
        ensureLoop()
    }

    fun submit(action: PlayerAction) = engine.submit(action)

    fun activateRoadAid(aid: RoadAid): Boolean {
        val activated = engine.activateRoadAid(aid)
        if (activated) publishImmediately()
        return activated
    }

    fun pause() {
        engine.pause()
        haltLoop()
        publishImmediately()
    }

    fun resume() {
        engine.resume()
        lastFrameNanos = 0L
        publishImmediately()
        ensureLoop()
    }

    fun setInvalidator(callback: (() -> Unit)?) {
        invalidateView = callback
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
        val frameDelta = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, GameTuning.maxFrameDeltaSeconds)
        lastFrameNanos = frameTimeNanos
        accumulator += frameDelta
        hudElapsed += frameDelta
        var steps = 0
        while (accumulator >= GameTuning.fixedStepSeconds && steps < 4) {
            engine.update(GameTuning.fixedStepSeconds)
            accumulator -= GameTuning.fixedStepSeconds
            steps++
        }
        latestSnapshot = engine.snapshot()
        invalidateView?.invoke()
        val events = engine.drainEvents()
        if (events.isNotEmpty()) onEvents(events)
        if (hudElapsed >= 0.1f || events.any { it is GameEvent.Finished }) {
            hudElapsed = 0f
            onHudSnapshot(latestSnapshot)
        }
        if (latestSnapshot.phase == RunPhase.RUNNING) {
            choreographer.postFrameCallback(this)
        } else {
            running = false
        }
    }

    fun stop() {
        haltLoop()
        invalidateView = null
    }

    private fun haltLoop() {
        running = false
        choreographer.removeFrameCallback(this)
        lastFrameNanos = 0L
        accumulator = 0f
    }

    private fun ensureLoop() {
        if (running) return
        running = true
        choreographer.postFrameCallback(this)
    }

    private fun publishImmediately() {
        latestSnapshot = engine.snapshot()
        onHudSnapshot(latestSnapshot)
        invalidateView?.invoke()
    }
}
