package com.chickenroadrunner.game.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import com.chickenroadrunner.game.R

/** Owns the original looping score and keeps it aligned with app/audio lifecycle. */
class MusicController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val player = MediaPlayer.create(
        context.applicationContext,
        R.raw.road_rooster_theme,
        attributes,
        AudioManager.AUDIO_SESSION_ID_GENERATE,
    )?.apply {
        isLooping = true
        setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
    }
    private var enabled = true
    private var foreground = false
    private var hasAudioFocus = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                player?.setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
                if (enabled && foreground && player?.isPlaying == false) player.start()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player?.setVolume(DUCKED_VOLUME, DUCKED_VOLUME)
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> {
                hasAudioFocus = false
                if (player?.isPlaying == true) player.pause()
            }
        }
    }

    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(false)
            .build()
    } else {
        null
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        syncPlayback()
    }

    fun onForeground() {
        foreground = true
        syncPlayback()
    }

    fun onBackground() {
        foreground = false
        pauseAndAbandonFocus()
    }

    private fun syncPlayback() {
        val currentPlayer = player ?: return
        if (!enabled || !foreground) {
            pauseAndAbandonFocus()
            return
        }
        if (!hasAudioFocus) hasAudioFocus = requestFocus()
        if (hasAudioFocus && !currentPlayer.isPlaying) {
            currentPlayer.setVolume(NORMAL_VOLUME, NORMAL_VOLUME)
            currentPlayer.start()
        }
    }

    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(requireNotNull(focusRequest))
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun pauseAndAbandonFocus() {
        if (player?.isPlaying == true) player.pause()
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        hasAudioFocus = false
    }

    fun release() {
        pauseAndAbandonFocus()
        player?.release()
    }

    private companion object {
        const val NORMAL_VOLUME = 0.28f
        const val DUCKED_VOLUME = 0.08f
    }
}
