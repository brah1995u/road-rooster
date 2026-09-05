package com.chickenroadrunner.game.presentation

import androidx.annotation.DrawableRes
import com.chickenroadrunner.game.R
import com.chickenroadrunner.game.data.ChickenSkin

internal data class ChickenFrame(
    @DrawableRes val drawableRes: Int,
    val footPaddingRatio: Float,
)

internal data class ChickenArtSet(
    @DrawableRes val idleRes: Int,
    val runContactLeft: ChickenFrame,
    val runPass: ChickenFrame,
    val runContactRight: ChickenFrame,
    val jump: ChickenFrame,
    val duck: ChickenFrame,
    val hit: ChickenFrame,
    val win: ChickenFrame,
)

private val classicArt = ChickenArtSet(
    idleRes = R.drawable.chicken_idle,
    runContactLeft = ChickenFrame(R.drawable.chicken_run_contact_left, 0.091f),
    runPass = ChickenFrame(R.drawable.chicken_run_pass, 0.061f),
    runContactRight = ChickenFrame(R.drawable.chicken_run_contact_right, 0.064f),
    jump = ChickenFrame(R.drawable.chicken_jump, 0.114f),
    duck = ChickenFrame(R.drawable.chicken_duck, 0.078f),
    hit = ChickenFrame(R.drawable.chicken_hit, 0.034f),
    win = ChickenFrame(R.drawable.chicken_win, 0.057f),
)

private val farmerArt = ChickenArtSet(
    idleRes = R.drawable.chicken_farmer_idle,
    runContactLeft = ChickenFrame(R.drawable.chicken_farmer_run_contact_left, 0.092f),
    runPass = ChickenFrame(R.drawable.chicken_farmer_run_pass, 0.052f),
    runContactRight = ChickenFrame(R.drawable.chicken_farmer_run_contact_right, 0.010f),
    jump = ChickenFrame(R.drawable.chicken_farmer_jump, 0.116f),
    duck = ChickenFrame(R.drawable.chicken_farmer_duck, 0.060f),
    hit = ChickenFrame(R.drawable.chicken_farmer_hit, 0.030f),
    win = ChickenFrame(R.drawable.chicken_farmer_win, 0.047f),
)

private val racerArt = ChickenArtSet(
    idleRes = R.drawable.chicken_racer_idle,
    runContactLeft = ChickenFrame(R.drawable.chicken_racer_run_contact_left, 0.012f),
    runPass = ChickenFrame(R.drawable.chicken_racer_run_pass, 0.031f),
    runContactRight = ChickenFrame(R.drawable.chicken_racer_run_contact_right, 0.025f),
    jump = ChickenFrame(R.drawable.chicken_racer_jump, 0.116f),
    duck = ChickenFrame(R.drawable.chicken_racer_duck, 0.045f),
    hit = ChickenFrame(R.drawable.chicken_racer_hit, 0.028f),
    win = ChickenFrame(R.drawable.chicken_racer_win, 0.059f),
)

private val goldenArt = ChickenArtSet(
    idleRes = R.drawable.chicken_golden_idle,
    runContactLeft = ChickenFrame(R.drawable.chicken_golden_run_contact_left, 0.088f),
    runPass = ChickenFrame(R.drawable.chicken_golden_run_pass, 0.041f),
    runContactRight = ChickenFrame(R.drawable.chicken_golden_run_contact_right, 0.060f),
    jump = ChickenFrame(R.drawable.chicken_golden_jump, 0.115f),
    duck = ChickenFrame(R.drawable.chicken_golden_duck, 0.047f),
    hit = ChickenFrame(R.drawable.chicken_golden_hit, 0.046f),
    win = ChickenFrame(R.drawable.chicken_golden_win, 0.058f),
)

internal fun ChickenSkin.artSet(): ChickenArtSet = when (this) {
    ChickenSkin.CLASSIC -> classicArt
    ChickenSkin.FARMER -> farmerArt
    ChickenSkin.RACER -> racerArt
    ChickenSkin.GOLDEN -> goldenArt
}
