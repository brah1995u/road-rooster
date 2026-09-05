package com.chickenroadrunner.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickenroadrunner.game.data.ChickenSkin
import com.chickenroadrunner.game.data.PlayerProgress
import com.chickenroadrunner.game.game.GameSnapshot
import com.chickenroadrunner.game.game.LevelCatalog
import com.chickenroadrunner.game.game.ObstacleRule
import com.chickenroadrunner.game.game.PlayerAction
import com.chickenroadrunner.game.game.RunPhase
import com.chickenroadrunner.game.game.RunResult
import com.chickenroadrunner.game.game.RoadAid
import com.chickenroadrunner.game.presentation.AppScreen
import com.chickenroadrunner.game.presentation.AppViewModel
import com.chickenroadrunner.game.presentation.GameRenderView
import com.chickenroadrunner.game.presentation.artSet
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ChickenRoadTheme { ChickenRoadApp(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForeground()
    }

    override fun onPause() {
        viewModel.onAppBackground()
        if (viewModel.screen.value == AppScreen.GAME) viewModel.pauseGame()
        super.onPause()
    }
}

private val Ink = Color(0xFF2B160D)
private val Gold = Color(0xFFFFC52E)
private val GoldBright = Color(0xFFFFE98B)
private val Orange = Color(0xFFF47B16)
private val Red = Color(0xFFC73B24)
private val Cream = Color(0xFFFFF1C8)
private val Asphalt = Color(0xFF1E2028)

@Composable
private fun ChickenRoadTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun ChickenRoadApp(viewModel: AppViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    BackHandler(enabled = screen != AppScreen.MENU && screen != AppScreen.SPLASH) {
        when {
            screen == AppScreen.GAME && hud.phase == RunPhase.RUNNING -> viewModel.pauseGame()
            screen == AppScreen.GAME && hud.phase == RunPhase.PAUSED -> viewModel.resumeGame()
            screen == AppScreen.ABOUT || screen == AppScreen.PRIVACY -> viewModel.navigate(AppScreen.SETTINGS)
            screen == AppScreen.RESULT -> viewModel.navigate(AppScreen.LEVELS)
            else -> viewModel.navigate(AppScreen.MENU)
        }
    }

    when (screen) {
        AppScreen.SPLASH -> SplashScreen()
        AppScreen.MENU -> MainMenu(progress, viewModel)
        AppScreen.LEVELS -> LevelSelect(progress, viewModel)
        AppScreen.GAME -> GameplayScreen(progress, hud, viewModel)
        AppScreen.ACHIEVEMENTS -> AchievementsScreen(progress, viewModel)
        AppScreen.LEADERBOARD -> LeaderboardScreen(progress, viewModel)
        AppScreen.SHOP -> ShopScreen(progress, viewModel)
        AppScreen.SETTINGS -> SettingsScreen(progress, viewModel)
        AppScreen.ABOUT -> AboutScreen(viewModel)
        AppScreen.PRIVACY -> PrivacyScreen(viewModel)
        AppScreen.RESULT -> ResultScreen(result, progress, viewModel)
    }
}

@Composable
private fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse",
    )
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_splash_road_rooster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent, Color(0x330D1019)),
                ),
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 14.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArcadeTitle(
                modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 250.dp),
                scale = pulse,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "RULE THE ROAD.",
                color = Cream,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 2.2.sp,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(Color(0xCC341800), Offset(0f, 3f), 5f),
                ),
            )
        }
    }
}

@Composable
private fun MainMenu(progress: PlayerProgress, viewModel: AppViewModel) {
    RoadBackground(R.drawable.bg_menu_road) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CurrencyPill("COINS", progress.coins.toString(), Gold)
                Spacer(Modifier.width(8.dp))
                CurrencyPill("CORN", progress.corn.toString(), Color(0xFFFFB51B))
            }
            Spacer(Modifier.height(8.dp))
            ArcadeTitle(Modifier.fillMaxWidth().heightIn(min = 128.dp, max = 190.dp))
            Spacer(Modifier.weight(0.12f))
            MascotBadge(progress.selectedSkin, Modifier.fillMaxWidth(0.43f).widthIn(max = 184.dp).aspectRatio(1f))
            Spacer(Modifier.weight(0.08f))
            ArcadeButton("PLAY", onClick = { viewModel.navigate(AppScreen.LEVELS) }, modifier = Modifier.fillMaxWidth().height(72.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MenuIconAction("GOALS", R.drawable.ui_icon_star) { viewModel.navigate(AppScreen.ACHIEVEMENTS) }
                MenuIconAction("RANKS", R.drawable.ui_icon_leaderboard) { viewModel.navigate(AppScreen.LEADERBOARD) }
                MenuIconAction("SHOP", R.drawable.ui_icon_shop) { viewModel.navigate(AppScreen.SHOP) }
                MenuIconAction("SETTINGS", R.drawable.ui_icon_settings) { viewModel.navigate(AppScreen.SETTINGS) }
            }
            Spacer(Modifier.height(8.dp))
            Text("15 HANDCRAFTED ROAD CHALLENGES", color = Cream, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        }
    }
}

@Composable
private fun MenuIconAction(label: String, icon: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoundAssetButton(onClick = onClick, icon = icon, modifier = Modifier.size(58.dp))
        Text(label, color = Cream, fontWeight = FontWeight.Black, fontSize = 9.sp)
    }
}

@Composable
private fun LevelSelect(progress: PlayerProgress, viewModel: AppViewModel) {
    FeatherBackground {
        ScreenScaffold(
            title = "CHOOSE YOUR ROAD",
            onBack = { viewModel.navigate(AppScreen.MENU) },
            onHome = { viewModel.navigate(AppScreen.MENU) },
        ) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(LevelCatalog.levels) { level ->
                    val unlocked = level.id <= progress.unlockedLevel
                    GoldPanel(
                        modifier = Modifier.fillMaxWidth().height(132.dp).clickable(enabled = unlocked) { viewModel.startLevel(level.id) },
                        color = if (unlocked) Gold else Color(0xFF8D8378),
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            LevelMedal(level.id, unlocked, level.id in progress.goldenEggLevels)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(level.name.uppercase(), color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${(level.length / level.baseSpeed).toInt()} sec • Corn target ${level.cornTarget}", color = Ink.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (unlocked) "TAP TO RUN" else "COMPLETE LEVEL ${level.id - 1}", color = if (unlocked) Red else Ink.copy(alpha = 0.55f), fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            if (unlocked) Image(painterResource(R.drawable.ui_icon_next), null, Modifier.size(34.dp))
                            else Text("LOCKED", color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameplayScreen(progress: PlayerProgress, hud: GameSnapshot, viewModel: AppViewModel) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                GameRenderView(context, viewModel.session, viewModel::submit).also { it.selectedSkin = progress.selectedSkin }
            },
            update = { it.selectedSkin = progress.selectedSkin },
            modifier = Modifier.fillMaxSize(),
        )
        GameplayHud(progress, hud, onPause = viewModel::pauseGame, onRoadAid = viewModel::activateRoadAid)
        AnimatedVisibility(
            visible = hud.phase == RunPhase.PAUSED,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            PauseOverlay(
                onResume = viewModel::resumeGame,
                onRestart = viewModel::retryLevel,
                onHome = { viewModel.navigate(AppScreen.MENU) },
            )
        }
        GestureHints(hud, viewModel::submit)
    }
}

@Composable
private fun GameplayHud(
    progress: PlayerProgress,
    hud: GameSnapshot,
    onPause: () -> Unit,
    onRoadAid: (RoadAid) -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PauseButton(onPause)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(hud.levelName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                ProgressBar(hud.progress)
            }
            Spacer(Modifier.width(8.dp))
            PickupPill("●", hud.coins.toString(), Gold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PickupPill("C", "${hud.corn}/${hud.cornTarget}", Color(0xFFFFB51B))
            Spacer(Modifier.width(8.dp))
            PickupPill("EGG", if (hud.goldenEgg) "✓" else "—", if (hud.goldenEgg) GoldBright else Color(0xFF8A7C70))
        }
        Spacer(Modifier.height(7.dp))
        RoadAidToolbar(progress, hud, onRoadAid)
    }
}

@Composable
private fun GestureHints(hud: GameSnapshot, onAction: (PlayerAction) -> Unit) {
    val nextJump = hud.entities.asSequence().filter { it.rule == ObstacleRule.JUMP && it.contactDistance > -1f }.minByOrNull { it.contactDistance }
    val nextDive = hud.entities.asSequence().filter { it.rule == ObstacleRule.DUCK && it.contactDistance > -1f }.minByOrNull { it.contactDistance }
    val jumpNow = nextJump != null && nextJump.contactDistance <= hud.speed * 2.0f
    val diveNow = nextDive != null && nextDive.contactDistance <= hud.speed * 0.85f
    val suggestedAction = when {
        hud.levelId == 1 && jumpNow -> PlayerAction.JUMP
        hud.levelId == 1 && diveNow -> PlayerAction.DUCK
        else -> null
    }
    val cue = if (hud.levelId != 1) null else when {
        jumpNow -> "JUMP NOW! • SWIPE UP OR TAP THE ARROW"
        diveNow -> "DIVE NOW! • SWIPE DOWN OR TAP THE ARROW"
        else -> when (hud.currentPattern) {
        "runway", "lane_tutorial" -> "SWIPE  ←  →   MOVE BETWEEN LANES"
        "jump_tutorial" -> if (nextJump == null) "NICE JUMP! • KEEP RUNNING" else "GET READY • THE JUMP WINDOW STARTS EARLY"
        "dive_tutorial" -> if (nextDive == null) "GREAT DIVE! • KEEP RUNNING" else "GET READY • DIVE UNDER THE HIGH GATE"
        "moving_tire_intro" -> "WATCH THE WARNING • MOVE AFTER THE TIRE"
        "traffic_with_gap" -> "TRAFFIC CROSSES ONCE • USE THE OPEN GAP"
        "golden_egg_choice" -> "OPTIONAL EGG ROUTE • JUMP WHEN THE FEET REACH THE BAR"
        else -> null
        }
    }
    AnimatedVisibility(visible = cue != null && hud.phase == RunPhase.RUNNING, modifier = Modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            GoldPanel(Modifier.padding(bottom = 24.dp).fillMaxWidth(0.88f), color = Cream) {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cue.orEmpty(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = if (suggestedAction != null) Red else Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    suggestedAction?.let { action ->
                        Spacer(Modifier.width(8.dp))
                        RoundAssetButton(
                            onClick = { onAction(action) },
                            icon = R.drawable.ui_icon_next,
                            modifier = Modifier.size(54.dp),
                            rotation = if (action == PlayerAction.JUMP) -90f else 90f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.76f)).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundAssetButton(
                    onClick = onResume,
                    icon = R.drawable.ui_icon_next,
                    modifier = Modifier.size(58.dp),
                    mirror = true,
                )
                Text(
                    text = "ROAD PAUSED",
                    modifier = Modifier.weight(1f),
                    color = GoldBright,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(shadow = Shadow(Color(0xDD2B160D), Offset(0f, 3f), 4f)),
                )
                RoundAssetButton(
                    onClick = onHome,
                    icon = R.drawable.ui_icon_home,
                    modifier = Modifier.size(58.dp),
                )
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                GoldPanel(Modifier.fillMaxWidth(0.88f).height(250.dp), color = Cream) {
                    Column(
                        Modifier.fillMaxSize().padding(start = 26.dp, top = 60.dp, end = 26.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ArcadeButton("RESUME", onResume, Modifier.fillMaxWidth().height(60.dp))
                        Spacer(Modifier.height(8.dp))
                        ArcadeButton(
                            "RESTART",
                            onRestart,
                            Modifier.fillMaxWidth().height(60.dp),
                            red = true,
                            icon = R.drawable.ui_icon_restart,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(result: RunResult?, progress: PlayerProgress, viewModel: AppViewModel) {
    val won = result?.won == true
    val levelName = result?.let { LevelCatalog.byId(it.levelId).name.uppercase() } ?: "ROAD RUN"
    val runProgress = if (result == null) 0f else (result.distance / LevelCatalog.byId(result.levelId).length).coerceIn(0f, 1f)
    RoadBackground(if (won) R.drawable.bg_menu_road else R.drawable.bg_fire_road) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundAssetButton({ viewModel.navigate(AppScreen.LEVELS) }, R.drawable.ui_icon_next, Modifier.size(54.dp), mirror = true)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (won) "VICTORY!" else "ROADBLOCK!",
                        color = if (won) GoldBright else Color(0xFFFF9A76),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                        style = LocalTextStyle.current.copy(shadow = Shadow(Color(0xCC2B160D), Offset(0f, 3f), 4f)),
                    )
                    Text(
                        "LEVEL ${result?.levelId ?: 1}  •  $levelName",
                        color = Cream,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RoundAssetButton({ viewModel.navigate(AppScreen.MENU) }, R.drawable.ui_icon_home, Modifier.size(54.dp))
            }
            Spacer(Modifier.height(6.dp))

            GoldPanel(Modifier.fillMaxWidth().weight(1f), color = Cream) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ResultCharacterStage(
                        won = won,
                        skin = progress.selectedSkin,
                        goldenEgg = result?.goldenEgg == true,
                        modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 130.dp, max = 270.dp),
                    )
                    Text(
                        if (won) "THE ROAD IS YOURS!" else "SO CLOSE — HIT THE ROAD AGAIN!",
                        color = if (won) Red else Ink,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        fontSize = if (won) 18.sp else 15.sp,
                        lineHeight = if (won) 21.sp else 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (won) "Clean crossing. Rewards are safely banked." else "You reached ${(runProgress * 100).toInt()}% of the route.",
                        color = Ink.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    ResultRewardTray(result)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.pickup_coin), null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(5.dp))
                        Text("COIN BANK  ${progress.coins}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))

            if (won && (result?.levelId ?: LevelCatalog.levels.size) < LevelCatalog.levels.size) {
                ArcadeButton("NEXT ROAD", viewModel::nextLevel, Modifier.fillMaxWidth().height(64.dp), icon = R.drawable.ui_icon_next)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ArcadeButton("REPLAY", viewModel::retryLevel, Modifier.weight(1f), compact = true, red = true, icon = R.drawable.ui_icon_restart)
                    ArcadeButton("LEVELS", { viewModel.navigate(AppScreen.LEVELS) }, Modifier.weight(1f), compact = true, icon = R.drawable.ui_icon_home)
                }
            } else {
                ArcadeButton(if (won) "RACE AGAIN" else "TRY AGAIN", viewModel::retryLevel, Modifier.fillMaxWidth().height(64.dp), red = !won, icon = R.drawable.ui_icon_restart)
                Spacer(Modifier.height(5.dp))
                ArcadeButton("CHOOSE ROAD", { viewModel.navigate(AppScreen.LEVELS) }, Modifier.fillMaxWidth(), compact = true, icon = R.drawable.ui_icon_home)
            }
        }
    }
}

@Composable
private fun ResultCharacterStage(won: Boolean, skin: ChickenSkin, goldenEgg: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(if (won) R.drawable.ui_icon_star else R.drawable.obstacle_low_barrier),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight(if (won) 0.84f else 0.56f).aspectRatio(1f).alpha(if (won) 0.78f else 0.90f),
            contentScale = ContentScale.Fit,
        )
        Image(
            painterResource(if (won) skin.artSet().win.drawableRes else skin.artSet().hit.drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight(0.98f).fillMaxWidth(0.64f),
            contentScale = ContentScale.Fit,
        )
        if (goldenEgg) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(68.dp).background(Asphalt.copy(alpha = 0.94f), CircleShape).border(3.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.pickup_golden_egg), null, Modifier.fillMaxSize(0.76f), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun ResultRewardTray(result: RunResult?) {
    Box(Modifier.fillMaxWidth().height(94.dp), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ui_panel_reward), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResultReward(R.drawable.pickup_coin, (result?.coins ?: 0).toString(), "COINS", Modifier.weight(1f))
            ResultReward(R.drawable.pickup_corn, (result?.corn ?: 0).toString(), "CORN", Modifier.weight(1f))
            ResultReward(
                R.drawable.pickup_golden_egg,
                if (result?.goldenEgg == true) "YES" else "—",
                "GOLD EGG",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ResultReward(iconRes: Int, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Asphalt.copy(alpha = 0.94f), RoundedCornerShape(14.dp)).border(2.dp, Gold, RoundedCornerShape(14.dp)).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(iconRes), null, Modifier.size(30.dp), contentScale = ContentScale.Fit)
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
        Text(label, color = GoldBright, fontWeight = FontWeight.Black, fontSize = 7.sp, maxLines = 1)
    }
}

private data class AchievementUi(
    val title: String,
    val detail: String,
    val current: Int,
    val target: Int,
    val iconRes: Int,
)

@Composable
private fun AchievementsScreen(progress: PlayerProgress, viewModel: AppViewModel) {
    val goals = listOf(
        AchievementUi("FIRST CROSSING", "Win your first road", progress.totalWins.coerceAtMost(1), 1, R.drawable.ui_achievement_first_crossing),
        AchievementUi("ROAD TESTED", "Win three roads", progress.totalWins.coerceAtMost(3), 3, R.drawable.ui_achievement_road_tested),
        AchievementUi("MIDWAY ROOSTER", "Reach Road 8", if (progress.unlockedLevel >= 8) 1 else 0, 1, R.drawable.ui_achievement_midway_rooster),
        AchievementUi("GRAND COOP", "Clear Road 15", (progress.levelRecords[15]?.clears ?: 0).coerceAtMost(1), 1, R.drawable.ui_achievement_grand_coop),
        AchievementUi("GOLDEN TRAIL", "Find 3 Golden Eggs", progress.goldenEggLevels.size.coerceAtMost(3), 3, R.drawable.ui_achievement_golden_trail),
        AchievementUi("EGG CARTON", "Find 8 Golden Eggs", progress.goldenEggLevels.size.coerceAtMost(8), 8, R.drawable.ui_achievement_egg_carton),
        AchievementUi("CROWNED ROOSTER", "Find all 15 Golden Eggs", progress.goldenEggLevels.size.coerceAtMost(15), 15, R.drawable.ui_achievement_crowned_rooster),
        AchievementUi("CORN KEEPER", "Collect 100 Corn total", progress.lifetimeCorn.coerceAtMost(100), 100, R.drawable.ui_achievement_corn_keeper),
        AchievementUi("HARVEST HERO", "Collect 500 Corn total", progress.lifetimeCorn.coerceAtMost(500), 500, R.drawable.ui_achievement_harvest_hero),
        AchievementUi("COIN CLUCKER", "Collect 500 Coins total", progress.lifetimeCoins.coerceAtMost(500), 500, R.drawable.ui_achievement_coin_clucker),
        AchievementUi("ROAD VETERAN", "Start 25 road runs", progress.totalRuns.coerceAtMost(25), 25, R.drawable.ui_achievement_road_veteran),
        AchievementUi("FASHION FLOCK", "Unlock every look", progress.unlockedSkins.size.coerceAtMost(4), 4, R.drawable.ui_achievement_fashion_flock),
    )
    val completed = goals.count { it.current >= it.target }
    FeatherBackground {
        ScreenScaffold(
            title = "ACHIEVEMENTS",
            onBack = { viewModel.navigate(AppScreen.MENU) },
            onHome = { viewModel.navigate(AppScreen.MENU) },
        ) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    GoldPanel(Modifier.fillMaxWidth().height(70.dp), color = Color(0xFFFFE4A3)) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("ROAD TROPHY CASE", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("$completed / ${goals.size}", color = Red, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
                items(goals) { goal ->
                    val done = goal.current >= goal.target
                    GoldPanel(Modifier.fillMaxWidth().height(92.dp), color = Cream) {
                        Row(Modifier.fillMaxSize().padding(start = 20.dp, top = 20.dp, end = 36.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(goal.iconRes), null, Modifier.size(36.dp).alpha(if (done) 1f else 0.42f), contentScale = ContentScale.Fit)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(goal.title, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text(goal.detail, color = Ink.copy(alpha = 0.68f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(Modifier.height(5.dp))
                                GoalProgress(goal.current / goal.target.toFloat())
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (done) "DONE" else "${goal.current}/${goal.target}",
                                color = if (done) Red else Ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardScreen(progress: PlayerProgress, viewModel: AppViewModel) {
    val records = progress.levelRecords.values.sortedWith(
        compareBy<com.chickenroadrunner.game.data.LevelRecord> { if (it.bestTimeMillis > 0L) 0 else 1 }
            .thenBy { if (it.bestTimeMillis > 0L) it.bestTimeMillis else Long.MAX_VALUE }
            .thenBy { it.levelId },
    )
    FeatherBackground {
        ScreenScaffold(
            title = "LEADERBOARD",
            onBack = { viewModel.navigate(AppScreen.MENU) },
            onHome = { viewModel.navigate(AppScreen.MENU) },
        ) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    GoldPanel(Modifier.fillMaxWidth().height(96.dp), color = Color(0xFFFFE4A3)) {
                        Row(
                            Modifier.fillMaxSize().padding(start = 28.dp, top = 24.dp, end = 32.dp, bottom = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(painterResource(R.drawable.ui_icon_leaderboard), null, Modifier.size(42.dp))
                            Spacer(Modifier.width(10.dp))
                            LeaderboardStat("RUNS", progress.totalRuns.toString(), Modifier.weight(1f))
                            LeaderboardStat("WINS", progress.totalWins.toString(), Modifier.weight(1f))
                            LeaderboardStat("EGGS", "${progress.goldenEggLevels.size}/15", Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("LOCAL BEST FINISHES", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Personal records stored only on this device", color = Ink.copy(alpha = 0.62f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                if (records.isEmpty()) {
                    item {
                        GoldPanel(Modifier.fillMaxWidth().height(118.dp), color = Cream) {
                            Column(
                                Modifier.padding(horizontal = 32.dp, vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("NO FINISHES YET", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("Finish any road to set your first record.", color = Ink.copy(alpha = 0.68f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    itemsIndexed(records) { index, record ->
                        val level = LevelCatalog.byId(record.levelId)
                        GoldPanel(Modifier.fillMaxWidth().height(94.dp), color = if (index == 0) GoldBright else Cream) {
                            Row(
                                Modifier.fillMaxSize().padding(start = 22.dp, top = 20.dp, end = 34.dp, bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("#${index + 1}", color = if (index == 0) Red else Ink, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.width(38.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(level.name.uppercase(), color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("ROAD ${record.levelId} • ${record.clears} CLEAR${if (record.clears == 1) "" else "S"}", color = Ink.copy(alpha = 0.62f), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatRecordTime(record.bestTimeMillis), color = Red, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Text(
                                        "${record.bestCoins} COINS • ${record.bestCorn} CORN${if (record.goldenEgg) " • EGG" else ""}",
                                        color = Ink.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Red, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
        Text(label, color = Ink.copy(alpha = 0.68f), fontWeight = FontWeight.Black, fontSize = 8.sp)
    }
}

private fun formatRecordTime(milliseconds: Long): String {
    if (milliseconds <= 0L) return "LEGACY"
    val hundredths = milliseconds / 10L
    val minutes = hundredths / 6_000L
    val seconds = (hundredths / 100L) % 60L
    val fraction = hundredths % 100L
    return "$minutes:${seconds.toString().padStart(2, '0')}.${fraction.toString().padStart(2, '0')}"
}

@Composable
private fun GoalProgress(progress: Float) {
    Box(Modifier.fillMaxWidth().height(10.dp).background(Ink.copy(alpha = 0.18f), CircleShape)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(Orange, Gold)), CircleShape))
    }
}

@Composable
private fun ShopScreen(progress: PlayerProgress, viewModel: AppViewModel) {
    FeatherBackground {
        ScreenScaffold(
            title = "FARM SHOP",
            onBack = { viewModel.navigate(AppScreen.MENU) },
            onHome = { viewModel.navigate(AppScreen.MENU) },
        ) {
            Row(Modifier.fillMaxWidth().padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.ui_icon_shop), null, Modifier.size(54.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("STYLES & ROAD AIDS", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("No speed or score multipliers", color = Ink.copy(alpha = 0.68f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Spacer(Modifier.width(8.dp))
                CurrencyPill("COINS", progress.coins.toString(), Gold)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { ShopSectionTitle("CHICKEN STYLES", "Cosmetic only • identical physics") }
                items(ChickenSkin.entries) { skin ->
                    val unlocked = skin in progress.unlockedSkins
                    GoldPanel(Modifier.fillMaxWidth().heightIn(min = 88.dp), color = if (skin == progress.selectedSkin) GoldBright else Cream) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 32.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            MiniChicken(skin, Modifier.size(54.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    skin.displayName.uppercase(),
                                    color = Ink,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    when {
                                        skin == ChickenSkin.GOLDEN && !unlocked -> "Collect all 3 Golden Eggs"
                                        unlocked -> "UNLOCKED"
                                        else -> "${skin.price} COINS"
                                    },
                                    color = Ink.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                )
                            }
                            ArcadeButton(
                                text = when { skin == progress.selectedSkin -> "WORN"; unlocked -> "WEAR"; else -> "BUY" },
                                onClick = { if (unlocked) viewModel.select(skin) else viewModel.purchase(skin) },
                                modifier = Modifier.width(70.dp),
                                enabled = skin != progress.selectedSkin && (unlocked || progress.coins >= skin.price) && (skin != ChickenSkin.GOLDEN || unlocked),
                                compact = true,
                            )
                        }
                    }
                }
                item { ShopSectionTitle("IN-RUN BOOSTS", "Buy here • tap one of the 3 buttons during a run") }
                items(RoadAid.entries) { aid ->
                    val count = progress.roadAidInventory.getOrDefault(aid, 0)
                    GoldPanel(Modifier.fillMaxWidth().heightIn(min = 96.dp), color = Cream) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 18.dp, end = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(aid.iconRes()),
                                null,
                                Modifier.size(46.dp).padding(4.dp).graphicsLayer(rotationZ = if (aid == RoadAid.WING_BOOST) -90f else 0f),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                                Text(aid.displayName(), color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(aid.description(), color = Ink.copy(alpha = 0.68f), fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp)
                                Text("OWNED $count  •  ${aid.price} COINS", color = Red, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                            ArcadeButton(
                                text = "BUY",
                                onClick = { viewModel.purchaseRoadAid(aid) },
                                modifier = Modifier.width(70.dp),
                                enabled = progress.coins >= aid.price,
                                compact = true,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable
private fun ShopSectionTitle(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(detail, color = Ink.copy(alpha = 0.62f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

private fun RoadAid.displayName(): String = when (this) {
    RoadAid.FEATHER_GUARD -> "FEATHER GUARD"
    RoadAid.CORN_MAGNET -> "CORN MAGNET"
    RoadAid.WING_BOOST -> "SUPER JUMP"
}

private fun RoadAid.description(): String = when (this) {
    RoadAid.FEATHER_GUARD -> "Tap in a run: blocks the next collision"
    RoadAid.CORN_MAGNET -> "Tap in a run: attracts Coins and Corn for 12 seconds"
    RoadAid.WING_BOOST -> "Tap in a run: launches one extra-high jump"
}

private fun RoadAid.iconRes(): Int = when (this) {
    RoadAid.FEATHER_GUARD -> R.drawable.ui_icon_star
    RoadAid.CORN_MAGNET -> R.drawable.pickup_corn
    RoadAid.WING_BOOST -> R.drawable.ui_icon_next
}

@Composable
private fun SettingsScreen(progress: PlayerProgress, viewModel: AppViewModel) {
    FeatherBackground {
        ScreenScaffold(
            title = "SETTINGS",
            onBack = { viewModel.navigate(AppScreen.MENU) },
            onHome = { viewModel.navigate(AppScreen.MENU) },
        ) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Image(painterResource(R.drawable.ui_icon_settings), null, Modifier.size(46.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AUDIO & FEEL", color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text("Saved automatically", color = Ink.copy(alpha = 0.62f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
                item {
                    GoldPanel(Modifier.fillMaxWidth(), color = Cream) {
                        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 68.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingRow("SOUND EFFECTS", "Pickups, steps and impacts", progress.soundEnabled) { viewModel.setSound(it) }
                            SettingRow("MUSIC", "Farm-road background track", progress.musicEnabled) { viewModel.setMusic(it) }
                            SettingRow("VIBRATION", "Short feedback on actions", progress.hapticsEnabled) { viewModel.setHaptics(it) }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArcadeButton("ABOUT", { viewModel.navigate(AppScreen.ABOUT) }, Modifier.weight(1f), compact = true)
                        ArcadeButton("PRIVACY", { viewModel.navigate(AppScreen.PRIVACY) }, Modifier.weight(1f), compact = true)
                    }
                }
                item {
                    GoldPanel(Modifier.fillMaxWidth(), color = Color(0xFFFFE4A3)) {
                        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 34.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FAIR ROAD RULES", color = Ink, fontWeight = FontWeight.Black)
                            Text("Portrait • Offline • No ads\nNo multipliers, combo chains or paid currency", color = Ink.copy(alpha = 0.72f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(viewModel: AppViewModel) {
    InformationScreen(
        title = stringResource(R.string.about_title),
        onBack = { viewModel.navigate(AppScreen.SETTINGS) },
        onHome = { viewModel.navigate(AppScreen.MENU) },
        sections = listOf(
            stringResource(R.string.about_gameplay_heading) to stringResource(R.string.about_gameplay_body),
            stringResource(R.string.about_fairness_heading) to stringResource(R.string.about_fairness_body),
            stringResource(R.string.about_credits_heading) to stringResource(R.string.about_credits_body),
            stringResource(R.string.about_version_heading) to BuildConfig.VERSION_NAME,
        ),
    )
}

@Composable
private fun PrivacyScreen(viewModel: AppViewModel) {
    InformationScreen(
        title = stringResource(R.string.privacy_title),
        onBack = { viewModel.navigate(AppScreen.SETTINGS) },
        onHome = { viewModel.navigate(AppScreen.MENU) },
        sections = listOf(
            stringResource(R.string.privacy_summary_heading) to stringResource(R.string.privacy_summary_body),
            stringResource(R.string.privacy_storage_heading) to stringResource(R.string.privacy_storage_body),
            stringResource(R.string.privacy_permissions_heading) to stringResource(R.string.privacy_permissions_body),
            stringResource(R.string.privacy_control_heading) to stringResource(R.string.privacy_control_body),
        ),
    )
}

@Composable
private fun InformationScreen(
    title: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    sections: List<Pair<String, String>>,
) {
    FeatherBackground {
        ScreenScaffold(title = title, onBack = onBack, onHome = onHome) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sections) { (heading, body) ->
                    GoldPanel(Modifier.fillMaxWidth(), color = Cream) {
                        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 42.dp, bottom = 30.dp)) {
                            Text(heading, color = Red, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(body, color = Ink.copy(alpha = 0.78f), fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, detail: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontWeight = FontWeight.Black)
            Text(detail, color = Ink.copy(alpha = 0.58f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        ArcadeToggle(enabled, onToggle)
    }
}

@Composable
private fun ArcadeToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val position by animateFloatAsState(if (enabled) 1f else 0f, label = "toggle")
    Canvas(
        modifier = Modifier.size(66.dp, 36.dp).clip(CircleShape).clickable { onToggle(!enabled) },
    ) {
        drawRoundRect(color = if (enabled) Color(0xFF63B83E) else Color(0xFF82766B), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        drawRoundRect(color = Ink, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2), style = Stroke(width = 3.dp.toPx()))
        val radius = size.height * 0.36f
        val x = radius + size.height * 0.14f + position * (size.width - radius * 2 - size.height * 0.28f)
        drawCircle(color = Cream, radius = radius, center = Offset(x, size.height / 2))
    }
}

@Composable
private fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    titleColor: Color = Ink,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundAssetButton(onBack, R.drawable.ui_icon_next, Modifier.size(58.dp), mirror = true)
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = titleColor, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            RoundAssetButton(onHome, R.drawable.ui_icon_home, Modifier.size(58.dp))
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun RoadBackground(drawable: Int, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(drawable), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0x550D1019), Color(0xAA151015)))))
        content()
    }
}

@Composable
private fun FeatherBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_feathers), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0x22FFF1C8)))
        content()
    }
}

@Composable
private fun ArcadeTitle(modifier: Modifier = Modifier, scale: Float = 1f) {
    Image(
        painterResource(R.drawable.ui_title_logo),
        contentDescription = "Road Rooster",
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun ArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    red: Boolean = false,
    icon: Int? = null,
) {
    val labelSize = when {
        compact && text.length >= 11 -> 11.sp
        compact && text.length >= 8 -> 12.sp
        compact -> 14.sp
        text.length >= 11 -> 16.sp
        text.length >= 9 -> 18.sp
        else -> 20.sp
    }
    Box(
        modifier = modifier
            .heightIn(min = if (compact) 48.dp else 58.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.52f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(if (red) R.drawable.ui_button_red_wide else R.drawable.ui_button_gold_wide),
            null,
            Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
        BoxWithConstraints(Modifier.matchParentSize()) {
            // The supplied wide-button art has more transparent padding above than below.
            // Center content on the visible embossed frame, not on the raw bitmap canvas.
            val opticalY = maxHeight * 0.098f
            val opticalX = maxWidth * if (red) -0.0063f else 0.0042f
            val sidePadding = if (compact) 10.dp else 22.dp
            val iconSize = if (compact) 25.dp else 31.dp

            icon?.let {
                Image(
                    painterResource(it),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = sidePadding + opticalX, y = opticalY)
                        .size(iconSize),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = opticalX, y = opticalY)
                    .fillMaxWidth()
                    .padding(horizontal = if (icon == null) sidePadding else sidePadding + iconSize + 4.dp),
                color = if (red) Color.White else Ink,
                fontWeight = FontWeight.Black,
                fontSize = labelSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GoldPanel(modifier: Modifier = Modifier, color: Color = Cream, content: @Composable BoxScope.() -> Unit) {
    val panel = if (color == Red) R.drawable.ui_panel_red else R.drawable.ui_panel_large
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(panel),
            null,
            Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = if (color == Color(0xFF8D8378)) ColorFilter.tint(color, BlendMode.Modulate) else null,
        )
        content()
    }
}

@Composable
private fun CurrencyPill(label: String, value: String, color: Color) {
    Row(Modifier.shadow(5.dp, RoundedCornerShape(18.dp)).background(Asphalt, RoundedCornerShape(18.dp)).border(2.dp, color, RoundedCornerShape(18.dp)).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(if (label == "COINS") R.drawable.pickup_coin else R.drawable.pickup_corn), null, Modifier.size(25.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(7.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
    }
}

@Composable
private fun PickupPill(icon: String, value: String, color: Color) {
    Row(Modifier.background(Asphalt.copy(alpha = 0.93f), RoundedCornerShape(14.dp)).border(2.dp, color, RoundedCornerShape(14.dp)).padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val iconRes = when (icon) { "●" -> R.drawable.pickup_coin; "C" -> R.drawable.pickup_corn; else -> R.drawable.pickup_golden_egg }
        Image(painterResource(iconRes), null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(5.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
private fun RoadAidToolbar(progress: PlayerProgress, hud: GameSnapshot, onRoadAid: (RoadAid) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        RoadAid.entries.forEach { aid ->
            val count = progress.roadAidInventory.getOrDefault(aid, 0)
            val active = hud.isRoadAidActive(aid)
            val enabled = hud.phase == RunPhase.RUNNING && count > 0 && !active &&
                (aid != RoadAid.WING_BOOST || hud.player.grounded)
            Box(
                Modifier
                    .width(88.dp)
                    .height(48.dp)
                    .alpha(if (enabled || active) 1f else 0.62f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Asphalt.copy(alpha = 0.94f))
                    .border(2.dp, if (active) Color(0xFF78E06B) else Gold, RoundedCornerShape(15.dp))
                    .clickable(enabled = enabled) { onRoadAid(aid) }
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(aid.iconRes()),
                        contentDescription = aid.displayName(),
                        modifier = Modifier
                            .size(27.dp)
                            .graphicsLayer(rotationZ = if (aid == RoadAid.WING_BOOST) -90f else 0f),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(5.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (active) hud.roadAidActiveLabel(aid) else "×$count",
                            color = if (active) Color(0xFF9AFF8B) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                        Text(
                            when (aid) {
                                RoadAid.FEATHER_GUARD -> "SHIELD"
                                RoadAid.CORN_MAGNET -> "MAGNET"
                                RoadAid.WING_BOOST -> "JUMP"
                            },
                            color = GoldBright,
                            fontWeight = FontWeight.Black,
                            fontSize = 7.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun GameSnapshot.isRoadAidActive(aid: RoadAid): Boolean = when (aid) {
    RoadAid.FEATHER_GUARD -> featherGuardActive
    RoadAid.CORN_MAGNET -> cornMagnetSeconds > 0f
    RoadAid.WING_BOOST -> wingBoostActive
}

private fun GameSnapshot.roadAidActiveLabel(aid: RoadAid): String = when (aid) {
    RoadAid.FEATHER_GUARD -> "ON"
    RoadAid.CORN_MAGNET -> "${cornMagnetSeconds.toInt().coerceAtLeast(1)}s"
    RoadAid.WING_BOOST -> "UP"
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(Modifier.fillMaxWidth().height(24.dp)) {
        Image(painterResource(R.drawable.ui_progress_frame), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        Box(Modifier.fillMaxSize().padding(start = 30.dp, end = 9.dp, top = 7.dp, bottom = 7.dp)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(Orange, GoldBright)), RoundedCornerShape(6.dp)))
        }
        Text("${(progress * 100).toInt()}%", modifier = Modifier.align(Alignment.Center), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PauseButton(onClick: () -> Unit) {
    PauseGlyph(Modifier.size(58.dp), onClick)
}

@Composable
private fun RoundAssetButton(onClick: () -> Unit, icon: Int, modifier: Modifier = Modifier, mirror: Boolean = false, rotation: Float = 0f) {
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ui_button_gold_round), null, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
        Image(
            painterResource(icon),
            null,
            Modifier.fillMaxSize(0.47f).graphicsLayer(scaleX = if (mirror) -1f else 1f, rotationZ = rotation),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun PauseGlyph(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ui_button_gold_round), null, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.fillMaxSize(0.34f)) {
            drawRoundRect(Ink, topLeft = Offset(size.width * 0.08f, 0f), size = Size(size.width * 0.28f, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawRoundRect(Ink, topLeft = Offset(size.width * 0.64f, 0f), size = Size(size.width * 0.28f, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
    }
}

@Composable
private fun LevelMedal(level: Int, unlocked: Boolean, egg: Boolean) {
    Box(Modifier.size(46.dp).background(if (unlocked) Gold else Color.Gray, CircleShape).border(2.dp, Ink, CircleShape), contentAlignment = Alignment.Center) {
        Text(level.toString(), color = Ink, fontSize = if (level < 10) 19.sp else 16.sp, fontWeight = FontWeight.Black)
        if (egg) Text("EGG", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), color = Red, fontSize = 6.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RoadSignIcon(modifier: Modifier) {
    Image(painterResource(R.drawable.launcher_source), null, modifier.clip(RoundedCornerShape(26.dp)), contentScale = ContentScale.Crop)
}

@Composable
private fun MascotBadge(skin: ChickenSkin, modifier: Modifier) {
    GoldPanel(modifier.aspectRatio(1f), color = Color(0xFFEAF7FF)) {
        Image(
            painter = painterResource(skin.artSet().idleRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(5.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MiniChicken(skin: ChickenSkin, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(skin.artSet().idleRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.82f),
            contentScale = ContentScale.Fit,
        )
    }
}

private fun Modifier.sizeInAdaptive(): Modifier = this.fillMaxWidth(0.48f).widthIn(max = 210.dp).aspectRatio(1f)
