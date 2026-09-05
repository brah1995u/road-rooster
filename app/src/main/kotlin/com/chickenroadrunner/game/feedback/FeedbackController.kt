package com.chickenroadrunner.game.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.chickenroadrunner.game.data.PlayerProgress
import com.chickenroadrunner.game.game.EntityKind
import com.chickenroadrunner.game.game.GameEvent

class FeedbackController(context: Context) {
    private val appContext = context.applicationContext
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    private val music = MusicController(appContext)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var settings = PlayerProgress()

    fun updateSettings(progress: PlayerProgress) {
        settings = progress
        music.updateEnabled(progress.musicEnabled)
    }

    fun onForeground() = music.onForeground()

    fun onBackground() = music.onBackground()

    fun handle(event: GameEvent) {
        when (event) {
            is GameEvent.Pickup -> {
                if (settings.soundEnabled) {
                    val toneId = if (event.kind == EntityKind.GOLDEN_EGG) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP
                    tone.startTone(toneId, if (event.kind == EntityKind.GOLDEN_EGG) 180 else 55)
                }
                if (event.kind == EntityKind.GOLDEN_EGG) vibrate(45, 180)
            }
            is GameEvent.Collision -> {
                if (settings.soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_NACK, 180)
                vibrate(80, 220)
            }
            is GameEvent.Action -> if (event.action.name.startsWith("MOVE")) vibrate(12, 45)
            is GameEvent.TrafficWarning -> if (settings.soundEnabled) {
                tone.startTone(ToneGenerator.TONE_PROP_PROMPT, if (event.kind == EntityKind.TRAFFIC_TRUCK) 150 else 95)
            }
            is GameEvent.RoadAidActivated -> {
                if (settings.soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                vibrate(28, 110)
            }
            is GameEvent.RoadAidUsed -> {
                if (settings.soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_ACK, 140)
                vibrate(45, 150)
            }
            is GameEvent.Finished -> {
                if (settings.soundEnabled) tone.startTone(if (event.result.won) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 220)
                if (event.result.won) vibrate(35, 130)
            }
        }
    }

    fun button() {
        if (settings.soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        vibrate(10, 35)
    }

    private fun vibrate(milliseconds: Long, amplitude: Int) {
        if (!settings.hapticsEnabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    fun release() {
        music.release()
        tone.release()
    }
}
