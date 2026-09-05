package com.chickenroadrunner.game.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chickenroadrunner.game.data.ChickenSkin
import com.chickenroadrunner.game.BuildConfig
import com.chickenroadrunner.game.data.PlayerProgress
import com.chickenroadrunner.game.data.ProgressRepository
import com.chickenroadrunner.game.feedback.FeedbackController
import com.chickenroadrunner.game.game.GameEvent
import com.chickenroadrunner.game.game.GameSnapshot
import com.chickenroadrunner.game.game.LevelCatalog
import com.chickenroadrunner.game.game.PlayerAction
import com.chickenroadrunner.game.game.RunResult
import com.chickenroadrunner.game.game.RoadAid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen { SPLASH, MENU, LEVELS, GAME, ACHIEVEMENTS, LEADERBOARD, SHOP, SETTINGS, ABOUT, PRIVACY, RESULT }

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProgressRepository(application)
    private val feedback = FeedbackController(application)
    private val _screen = MutableStateFlow(AppScreen.SPLASH)
    private val _progress = MutableStateFlow(PlayerProgress())
    private val _hud = MutableStateFlow(GameSnapshot())
    private val _result = MutableStateFlow<RunResult?>(null)
    private var currentLevelId = 1
    private var resultTransitionPending = false

    val screen: StateFlow<AppScreen> = _screen.asStateFlow()
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()
    val hud: StateFlow<GameSnapshot> = _hud.asStateFlow()
    val result: StateFlow<RunResult?> = _result.asStateFlow()

    val session = GameSession(
        onHudSnapshot = { _hud.value = it },
        onEvents = ::handleEvents,
    )

    init {
        viewModelScope.launch {
            repository.progress.collect {
                _progress.value = it
                feedback.updateSettings(it)
            }
        }
        viewModelScope.launch {
            delay(1_500)
            if (_screen.value == AppScreen.SPLASH) _screen.value = AppScreen.MENU
        }
    }

    fun navigate(screen: AppScreen) {
        feedback.button()
        if (_screen.value == AppScreen.GAME && screen != AppScreen.GAME) session.pause()
        _screen.value = screen
    }

    fun startLevel(levelId: Int) {
        if (levelId > _progress.value.unlockedLevel) return
        feedback.button()
        currentLevelId = levelId
        _result.value = null
        resultTransitionPending = false
        session.start(LevelCatalog.byId(levelId))
        _screen.value = AppScreen.GAME
    }

    fun retryLevel() {
        feedback.button()
        _result.value = null
        resultTransitionPending = false
        session.restart()
        _screen.value = AppScreen.GAME
    }

    fun nextLevel() {
        val next = (currentLevelId + 1).coerceAtMost(LevelCatalog.levels.size)
        if (next == currentLevelId) navigate(AppScreen.LEVELS) else startLevel(next)
    }

    fun submit(action: PlayerAction) {
        if (BuildConfig.DEBUG) Log.d("ChickenRoadInput", "$action at distance=${_hud.value.distance}")
        session.submit(action)
    }

    fun pauseGame() = session.pause()
    fun resumeGame() = session.resume()
    fun onAppForeground() = feedback.onForeground()
    fun onAppBackground() = feedback.onBackground()

    fun purchase(skin: ChickenSkin) {
        feedback.button()
        viewModelScope.launch { repository.purchase(skin) }
    }

    fun select(skin: ChickenSkin) {
        feedback.button()
        viewModelScope.launch { repository.select(skin) }
    }

    fun purchaseRoadAid(aid: RoadAid) {
        feedback.button()
        viewModelScope.launch { repository.purchaseRoadAid(aid) }
    }

    fun activateRoadAid(aid: RoadAid) {
        val current = _progress.value
        if (current.roadAidInventory.getOrDefault(aid, 0) <= 0) return
        if (!session.activateRoadAid(aid)) return
        feedback.button()
        _progress.value = com.chickenroadrunner.game.data.ProgressRules.consumeRoadAid(current, aid)
        viewModelScope.launch { repository.consumeRoadAid(aid) }
    }

    fun armRoadAid(aid: RoadAid?) {
        feedback.button()
        viewModelScope.launch { repository.armRoadAid(aid) }
    }

    fun setSound(enabled: Boolean) = viewModelScope.launch { repository.setSound(enabled) }
    fun setMusic(enabled: Boolean) = viewModelScope.launch { repository.setMusic(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { repository.setHaptics(enabled) }

    private fun handleEvents(events: List<GameEvent>) {
        if (BuildConfig.DEBUG) events.filterIsInstance<GameEvent.Collision>().forEach {
            Log.d("ChickenRoadInput", "collision=${it.kind} at distance=${_hud.value.distance}")
        }
        events.forEach(feedback::handle)
        val finished = events.filterIsInstance<GameEvent.Finished>().lastOrNull() ?: return
        if (resultTransitionPending) return
        resultTransitionPending = true
        _result.value = finished.result
        val token = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.commitRun(token, finished.result)
            delay(if (finished.result.won) 650 else 500)
            _screen.value = AppScreen.RESULT
        }
    }

    override fun onCleared() {
        session.stop()
        feedback.release()
    }
}
