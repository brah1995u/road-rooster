package com.chickenroadrunner.game.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import com.chickenroadrunner.game.BuildConfig
import com.chickenroadrunner.game.R
import com.chickenroadrunner.game.data.ChickenSkin
import com.chickenroadrunner.game.game.EntityKind
import com.chickenroadrunner.game.game.EntitySnapshot
import com.chickenroadrunner.game.game.GameSnapshot
import com.chickenroadrunner.game.game.GameTuning
import com.chickenroadrunner.game.game.GameplayProjection
import com.chickenroadrunner.game.game.GestureResolver
import com.chickenroadrunner.game.game.HazardKinematics
import com.chickenroadrunner.game.game.Lane
import com.chickenroadrunner.game.game.ObstacleRule
import com.chickenroadrunner.game.game.PlayerAction
import com.chickenroadrunner.game.game.PlayerPose
import com.chickenroadrunner.game.game.TelegraphKind
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class GameRenderView(
    context: Context,
    private val session: GameSession,
    private val onAction: (PlayerAction) -> Unit,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private val bitmapCache = mutableMapOf<Int, Bitmap?>()
    private val asphaltMatrix = Matrix()
    private var asphaltShader: BitmapShader? = null
    private val asphaltSourcePoints = FloatArray(8)
    private val asphaltDestinationPoints = FloatArray(8)
    private val roadsideLeftSource = Rect()
    private val roadsideRightSource = Rect()
    private val roadsideLeftDestination = RectF()
    private val roadsideRightDestination = RectF()
    private var downX = 0f
    private var downY = 0f
    private var gestureDispatched = false
    private var density = resources.displayMetrics.density
    private var debugOverlayEnabled = false
    private var fpsWindowStartNanos = 0L
    private var fpsFrameCount = 0
    private var measuredFps = 0f
    var selectedSkin: ChickenSkin = ChickenSkin.CLASSIC

    init {
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        session.setInvalidator { postInvalidateOnAnimation() }
    }

    override fun onDetachedFromWindow() {
        session.setInvalidator(null)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateFps()
        val snapshot = session.latestSnapshot
        drawWorld(canvas, snapshot)
        if (BuildConfig.SHOW_DEBUG_TOOLS && debugOverlayEnabled) drawDebug(canvas, snapshot)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (BuildConfig.SHOW_DEBUG_TOOLS && event.pointerCount >= 3) {
                    debugOverlayEnabled = !debugOverlayEnabled
                    postInvalidateOnAnimation()
                }
                return true
            }
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                gestureDispatched = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gestureDispatched) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val threshold = gestureThresholdPx()
                    GestureResolver.resolve(dx, dy, threshold)?.let { action ->
                        gestureDispatched = true
                        onAction(action)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (gestureDispatched) return true
                val dx = event.x - downX
                val dy = event.y - downY
                val threshold = gestureThresholdPx()
                val action = GestureResolver.resolve(dx, dy, threshold)
                if (action == null) {
                    performClick()
                    return true
                }
                onAction(action)
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun gestureThresholdPx(): Float = maxOf(
        GameTuning.gestureThresholdDp * density,
        minOf(width, height) * GameTuning.gestureThresholdScreenRatio,
    )

    private fun drawWorld(canvas: Canvas, snapshot: GameSnapshot) {
        paint.alpha = 255
        paint.colorFilter = null
        paint.shader = null
        drawSkyAndFarm(canvas, snapshot)
        drawRoad(canvas, snapshot)
        drawRoadsideProps(canvas)
        snapshot.entities.forEach { drawEntity(canvas, it) }
        drawPlayer(canvas, snapshot)
        drawSpeedLines(canvas, snapshot)
    }

    private fun drawSkyAndFarm(canvas: Canvas, snapshot: GameSnapshot) {
        (bitmap("bg_gameplay_horizon_v4") ?: bitmap("bg_gameplay_horizon_v3") ?: bitmap("bg_gameplay_farm"))?.let { background ->
            paint.alpha = 255
            paint.color = Color.WHITE
            // The master keeps extra sky for art reuse. Cropping the top 8.6% aligns
            // its painted horizon with the shared 24.5% gameplay vanishing point.
            val cropTop = if (background.width == 992 && background.height == 1586) {
                (background.height * 0.086f).toInt()
            } else {
                0
            }
            canvas.drawBitmap(
                background,
                Rect(0, cropTop, background.width, background.height),
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                paint,
            )
            return
        }

        val horizon = height * 0.25f
        paint.shader = LinearGradient(0f, 0f, 0f, horizon * 1.5f, Color.rgb(10, 104, 224), Color.rgb(126, 211, 255), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        val sunX = width * 0.52f
        val sunY = horizon * 0.78f
        repeat(4) { i ->
            paint.color = Color.argb(30 - i * 5, 255, 245, 174)
            canvas.drawCircle(sunX, sunY, width * (0.08f + i * 0.035f), paint)
        }
        paint.color = Color.rgb(255, 248, 207)
        canvas.drawCircle(sunX, sunY, width * 0.055f, paint)

        val scroll = (snapshot.distance * 2.2f) % (width * 0.32f)
        paint.color = Color.rgb(51, 135, 57)
        repeat(8) { i ->
            val x = i * width * 0.18f - scroll
            canvas.drawCircle(x, horizon + height * 0.005f, width * 0.09f, paint)
            canvas.drawCircle(x + width * 0.05f, horizon - height * 0.025f, width * 0.065f, paint)
        }
        paint.color = Color.rgb(248, 191, 49)
        canvas.drawRect(0f, horizon, width.toFloat(), horizon + height * 0.06f, paint)

        drawFence(canvas, horizon + height * 0.035f, left = true)
        drawFence(canvas, horizon + height * 0.035f, left = false)
    }

    private fun drawFence(canvas: Canvas, y: Float, left: Boolean) {
        val start = if (left) 0f else width * 0.78f
        val end = if (left) width * 0.22f else width.toFloat()
        paint.color = Color.rgb(244, 222, 176)
        canvas.drawRect(start, y, end, y + height * 0.012f, paint)
        canvas.drawRect(start, y + height * 0.026f, end, y + height * 0.038f, paint)
        var x = start
        while (x < end) {
            canvas.drawRoundRect(RectF(x, y - height * 0.025f, x + width * 0.018f, y + height * 0.052f), 5f, 5f, paint)
            x += width * 0.075f
        }
    }

    private fun drawRoad(canvas: Canvas, snapshot: GameSnapshot) {
        val horizonY = roadHorizonY()
        val roadBottomY = height * GameplayProjection.roadBottomY
        val topHalf = width * GameplayProjection.roadTopHalfWidth
        val bottomHalf = width * GameplayProjection.roadBottomHalfWidth

        // Warm shoulder glow first, then the asphalt. It gives the road physical thickness
        // without drawing a hard horizontal cap at the vanishing point.
        path.reset()
        path.moveTo(width / 2f - topHalf - width * 0.018f, horizonY)
        path.lineTo(width / 2f + topHalf + width * 0.018f, horizonY)
        path.lineTo(width / 2f + bottomHalf + width * 0.025f, roadBottomY)
        path.lineTo(width / 2f - bottomHalf - width * 0.025f, roadBottomY)
        path.close()
        paint.color = Color.rgb(111, 39, 18)
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(width / 2f - topHalf, horizonY)
        path.lineTo(width / 2f + topHalf, horizonY)
        path.lineTo(width / 2f + bottomHalf, roadBottomY)
        path.lineTo(width / 2f - bottomHalf, roadBottomY)
        path.close()
        paint.shader = LinearGradient(0f, horizonY, 0f, roadBottomY, Color.rgb(66, 66, 70), Color.rgb(25, 27, 32), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null
        drawAsphaltTexture(canvas, horizonY, roadBottomY, topHalf, bottomHalf)

        stroke.strokeWidth = width * 0.010f
        stroke.color = Color.rgb(255, 139, 22)
        canvas.drawLine(width / 2f - topHalf, horizonY, width / 2f - bottomHalf, roadBottomY, stroke)
        canvas.drawLine(width / 2f + topHalf, horizonY, width / 2f + bottomHalf, roadBottomY, stroke)

        // Keep both the asphalt and its dashed dividers locked to the guardrails.
        // Moving perspective dashes visibly slid sideways as they approached the
        // player, so world entities now provide the sense of forward motion.
        drawLaneMarks(canvas, -0.5f)
        drawLaneMarks(canvas, 0.5f)
        drawGuardRails(canvas)
        drawRoadFlames(canvas, snapshot)
    }

    private fun drawRoadsideProps(canvas: Canvas) {
        val overlay = bitmap("roadside_props_v3") ?: return
        paint.alpha = 255
        paint.color = Color.WHITE
        paint.shader = null
        val split = overlay.width / 2
        val shift = width * GameplayProjection.roadsideLayerHalfShift
        roadsideLeftSource.set(0, 0, split, overlay.height)
        roadsideRightSource.set(split, 0, overlay.width, overlay.height)
        roadsideLeftDestination.set(-shift, 0f, width / 2f - shift, height.toFloat())
        roadsideRightDestination.set(width / 2f + shift, 0f, width + shift, height.toFloat())
        canvas.drawBitmap(overlay, roadsideLeftSource, roadsideLeftDestination, paint)
        canvas.drawBitmap(overlay, roadsideRightSource, roadsideRightDestination, paint)
    }

    private fun drawAsphaltTexture(
        canvas: Canvas,
        horizonY: Float,
        roadBottomY: Float,
        topHalf: Float,
        bottomHalf: Float,
    ) {
        val texture = bitmap("road_asphalt_texture") ?: return
        val shader = asphaltShader ?: BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).also { asphaltShader = it }
        // Keep the texture transform locked to the road. Forward motion comes from
        // authored lane marks and road specks; translating the perspective source
        // rectangle caused a subtle side-to-side asphalt crawl on some aspect ratios.
        asphaltSourcePoints[0] = 0f; asphaltSourcePoints[1] = 0f
        asphaltSourcePoints[2] = texture.width.toFloat(); asphaltSourcePoints[3] = 0f
        asphaltSourcePoints[4] = texture.width.toFloat(); asphaltSourcePoints[5] = texture.height.toFloat()
        asphaltSourcePoints[6] = 0f; asphaltSourcePoints[7] = texture.height.toFloat()
        asphaltDestinationPoints[0] = width / 2f - topHalf; asphaltDestinationPoints[1] = horizonY
        asphaltDestinationPoints[2] = width / 2f + topHalf; asphaltDestinationPoints[3] = horizonY
        asphaltDestinationPoints[4] = width / 2f + bottomHalf; asphaltDestinationPoints[5] = roadBottomY
        asphaltDestinationPoints[6] = width / 2f - bottomHalf; asphaltDestinationPoints[7] = roadBottomY
        asphaltMatrix.reset()
        asphaltMatrix.setPolyToPoly(asphaltSourcePoints, 0, asphaltDestinationPoints, 0, 4)
        shader.setLocalMatrix(asphaltMatrix)
        paint.shader = shader
        paint.alpha = 138
        canvas.drawPath(path, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun drawGuardRails(canvas: Canvas) {
        val horizonY = roadHorizonY()
        val bottomY = height * 0.93f
        val railShoulderGap = 0.012f
        val topOffset = width * (GameplayProjection.roadHalfWidthAt(GameplayProjection.horizonY) + railShoulderGap)
        val bottomOffset = width * (GameplayProjection.roadHalfWidthAt(0.93f) + railShoulderGap)
        stroke.strokeWidth = width * 0.012f
        stroke.color = Color.rgb(91, 73, 65)
        canvas.drawLine(width / 2f - topOffset, horizonY, width / 2f - bottomOffset, bottomY, stroke)
        canvas.drawLine(width / 2f + topOffset, horizonY, width / 2f + bottomOffset, bottomY, stroke)
        stroke.strokeWidth = width * 0.0045f
        stroke.color = Color.rgb(224, 205, 179)
        canvas.drawLine(width / 2f - topOffset, horizonY, width / 2f - bottomOffset, bottomY, stroke)
        canvas.drawLine(width / 2f + topOffset, horizonY, width / 2f + bottomOffset, bottomY, stroke)
    }

    private fun drawLaneMarks(canvas: Canvas, divider: Float) {
        paint.color = Color.argb(190, 236, 232, 221)
        var d = -GameTuning.visibleBehindDistance + 4f
        while (d < GameTuning.visibleAheadDistance) {
            if (d + 7f > -GameTuning.visibleBehindDistance) {
                val near = project(d, divider)
                val far = project(d + 7f, divider)
                val w = width * (0.003f + near.scale * 0.009f)
                path.reset()
                path.moveTo(near.x - w, near.y)
                path.lineTo(near.x + w, near.y)
                path.lineTo(far.x + w * 0.45f, far.y)
                path.lineTo(far.x - w * 0.45f, far.y)
                path.close()
                canvas.drawPath(path, paint)
            }
            d += 18f
        }
    }

    private fun drawRoadFlames(canvas: Canvas, snapshot: GameSnapshot) {
        val phase = snapshot.distance * 0.08f
        repeat(7) { index ->
            val t = index / 6f
            val y = roadHorizonY() + t.pow(1.55f) * height * 0.72f
            val edge = width * (GameplayProjection.roadHalfWidthAt(y / height) + 0.02f).coerceAtMost(0.51f)
            val size = width * (0.013f + t * 0.032f)
            val flicker = sin(phase + index * 1.7f) * size * 0.18f
            drawFlame(canvas, width / 2f - edge, y, size, flicker, mirror = index % 2 == 0)
            drawFlame(canvas, width / 2f + edge, y, size, -flicker, mirror = index % 2 != 0)
        }
    }

    private fun drawFlame(canvas: Canvas, x: Float, y: Float, size: Float, flicker: Float, mirror: Boolean) {
        bitmap("roadside_flame_accent")?.let { flame ->
            val h = size * 3.4f
            val w = h * flame.width / flame.height.toFloat()
            canvas.save()
            canvas.rotate(flicker / size * 4.2f, x, y)
            canvas.scale(if (mirror) -1f else 1f, 0.96f + flicker / size * 0.08f, x, y)
            paint.alpha = 225
            canvas.drawBitmap(flame, null, RectF(x - w / 2f, y - h, x + w / 2f, y), paint)
            paint.alpha = 255
            canvas.restore()
            return
        }
        path.reset()
        path.moveTo(x, y)
        path.cubicTo(x - size, y - size * 0.35f, x - size * 0.38f + flicker, y - size * 1.4f, x + flicker, y - size * 2.0f)
        path.cubicTo(x + size * 0.28f, y - size * 1.2f, x + size, y - size * 0.5f, x, y)
        paint.color = Color.rgb(242, 67, 16)
        canvas.drawPath(path, paint)
        path.reset()
        path.moveTo(x, y)
        path.cubicTo(x - size * 0.4f, y - size * 0.4f, x + flicker, y - size * 1.1f, x + size * 0.1f, y - size * 1.35f)
        path.cubicTo(x + size * 0.5f, y - size * 0.7f, x + size * 0.4f, y - size * 0.3f, x, y)
        paint.color = Color.rgb(255, 185, 23)
        canvas.drawPath(path, paint)
    }

    private fun drawEntity(canvas: Canvas, entity: EntitySnapshot) {
        val p = project(entity.relativeDistance, entity.lanePosition)
        if (p.y < height * 0.24f || p.y > height * 1.05f) return
        if (entity.telegraph > 0f && entity.contactDistance > 0f) drawRoadTelegraph(canvas, entity, p)
        val base = width * 0.18f * p.scale
        val shadowBase = if (entity.kind == EntityKind.FINISH_COOP) {
            base * GameTuning.finishGateShadowScale
        } else {
            base
        }
        paint.color = Color.argb((110 * p.scale).toInt().coerceIn(20, 110), 0, 0, 0)
        canvas.drawOval(RectF(p.x - shadowBase * 0.7f, p.y - base * 0.08f, p.x + shadowBase * 0.7f, p.y + base * 0.22f), paint)

        val bitmapName = when (entity.kind) {
            EntityKind.COIN -> "pickup_coin"
            EntityKind.CORN -> "pickup_corn"
            EntityKind.GOLDEN_EGG -> "pickup_golden_egg"
            EntityKind.HAY_BALE -> "obstacle_hay_bale"
            EntityKind.LOW_BARRIER -> "obstacle_low_barrier"
            EntityKind.DUCK_GATE -> "obstacle_duck_gate"
            EntityKind.CONE -> "obstacle_cone"
            EntityKind.MANHOLE -> "obstacle_manhole"
            EntityKind.CART -> "obstacle_cart"
            EntityKind.ROLLING_TIRE -> "obstacle_rolling_tire"
            EntityKind.TRAFFIC_CAR -> "traffic_car"
            EntityKind.TRAFFIC_TRUCK -> "traffic_truck"
            EntityKind.FINISH_COOP -> "finish_coop"
        }
        val bitmap = bitmap(bitmapName)
        if (bitmap != null) {
            paint.alpha = 255
            paint.color = Color.WHITE
            val aspect = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
            val h = base * when (entity.kind) {
                EntityKind.FINISH_COOP -> GameTuning.finishGateHeightFactor
                EntityKind.DUCK_GATE -> 1.55f
                EntityKind.TRAFFIC_CAR, EntityKind.TRAFFIC_TRUCK -> 1.0f
                else -> 1.35f
            }
            val w = h * aspect
            val groundedBottom = p.y + h * entityBottomPaddingRatio(entity.kind)
            canvas.save()
            if ((entity.kind == EntityKind.TRAFFIC_CAR || entity.kind == EntityKind.TRAFFIC_TRUCK) && entity.motionDirection < 0) {
                canvas.scale(-1f, 1f, p.x, p.y)
            }
            canvas.drawBitmap(bitmap, null, RectF(p.x - w / 2f, groundedBottom - h, p.x + w / 2f, groundedBottom), paint)
            canvas.restore()
        } else {
            drawProceduralEntity(canvas, entity, p.x, p.y, base)
        }
        if (entity.telegraph > 0f && (entity.kind == EntityKind.TRAFFIC_CAR || entity.kind == EntityKind.TRAFFIC_TRUCK)) {
            drawTrafficWarning(canvas, p.x, p.y - base * 1.45f, base * 0.34f, entity.telegraph)
        }
    }

    private fun entityBottomPaddingRatio(kind: EntityKind): Float = when (kind) {
        EntityKind.HAY_BALE -> 0.078f
        EntityKind.LOW_BARRIER -> 0.104f
        EntityKind.DUCK_GATE -> 0.145f
        EntityKind.CONE -> 0.039f
        EntityKind.MANHOLE -> 0.047f
        EntityKind.CART -> 0.066f
        EntityKind.ROLLING_TIRE -> 0.052f
        EntityKind.TRAFFIC_CAR -> 0.031f
        EntityKind.TRAFFIC_TRUCK, EntityKind.FINISH_COOP -> 0.023f
        EntityKind.COIN, EntityKind.CORN, EntityKind.GOLDEN_EGG -> 0f
    }

    private fun drawRoadTelegraph(canvas: Canvas, entity: EntitySnapshot, p: Projection) {
        val pulse = 0.72f + 0.28f * sin(entity.telegraph * 18f)
        val size = width * (0.025f + p.scale * 0.055f)
        when (entity.warningKind) {
            TelegraphKind.BLOCKED -> {
                stroke.color = Color.argb((185 * pulse).toInt(), 239, 58, 25)
                stroke.strokeWidth = size * 0.18f
                repeat(3) { index ->
                    val y = p.y + size * (0.35f + index * 0.42f)
                    canvas.drawLine(p.x - size * 0.55f, y - size * 0.24f, p.x, y, stroke)
                    canvas.drawLine(p.x, y, p.x + size * 0.55f, y - size * 0.24f, stroke)
                }
            }
            TelegraphKind.JUMP, TelegraphKind.DIVE -> {
                paint.color = Color.argb((190 * pulse).toInt(), 255, 184, 24)
                canvas.drawOval(RectF(p.x - size, p.y + size * 0.22f, p.x + size, p.y + size * 1.25f), paint)
                stroke.color = Color.rgb(90, 42, 14)
                stroke.strokeWidth = size * 0.10f
                canvas.drawOval(RectF(p.x - size, p.y + size * 0.22f, p.x + size, p.y + size * 1.25f), stroke)
                paint.color = Color.rgb(75, 31, 12)
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                paint.textSize = size * 1.35f
                val arrow = if (entity.warningKind == TelegraphKind.JUMP) "↑" else "↓"
                canvas.drawText(arrow, p.x, p.y + size * 1.08f, paint)
                paint.textAlign = Paint.Align.LEFT
                paint.isFakeBoldText = false
            }
            TelegraphKind.TRAFFIC -> {
                stroke.color = Color.argb((180 * pulse).toInt(), 255, 184, 24)
                stroke.strokeWidth = size * 0.12f
                repeat(3) { index ->
                    val y = p.y + size * (0.35f + index * 0.35f)
                    canvas.drawLine(p.x - size * 1.35f, y, p.x + size * 1.35f, y, stroke)
                }
            }
            TelegraphKind.NONE -> Unit
        }
    }

    private fun drawTrafficWarning(canvas: Canvas, x: Float, y: Float, radius: Float, intensity: Float) {
        val pulse = (0.72f + 0.28f * sin(intensity * 18f)).coerceIn(0.45f, 1f)
        paint.color = Color.argb((235 * pulse).toInt(), 255, 190, 28)
        canvas.drawCircle(x, y, radius, paint)
        stroke.color = Color.rgb(78, 35, 15)
        stroke.strokeWidth = radius * 0.16f
        canvas.drawCircle(x, y, radius, stroke)
        paint.color = Color.rgb(78, 35, 15)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = radius * 1.45f
        paint.isFakeBoldText = true
        canvas.drawText("!", x, y + radius * 0.48f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false
    }

    private fun drawProceduralEntity(canvas: Canvas, entity: EntitySnapshot, x: Float, y: Float, size: Float) {
        when (entity.kind) {
            EntityKind.COIN -> {
                paint.color = Color.rgb(255, 183, 18)
                canvas.drawCircle(x, y - size * 0.65f, size * 0.43f, paint)
                stroke.color = Color.rgb(129, 67, 5)
                stroke.strokeWidth = size * 0.08f
                canvas.drawCircle(x, y - size * 0.65f, size * 0.43f, stroke)
            }
            EntityKind.CORN -> drawCorn(canvas, x, y, size)
            EntityKind.GOLDEN_EGG -> {
                paint.shader = LinearGradient(x - size, y - size, x + size, y, Color.WHITE, Color.rgb(255, 174, 0), Shader.TileMode.CLAMP)
                canvas.drawOval(RectF(x - size * 0.46f, y - size * 1.4f, x + size * 0.46f, y), paint)
                paint.shader = null
            }
            EntityKind.HAY_BALE -> {
                paint.color = Color.rgb(207, 137, 30)
                canvas.drawRoundRect(RectF(x - size * 0.7f, y - size * 0.8f, x + size * 0.7f, y), size * 0.12f, size * 0.12f, paint)
                stroke.color = Color.rgb(102, 64, 18)
                stroke.strokeWidth = size * 0.08f
                canvas.drawLine(x, y - size * 0.78f, x, y, stroke)
            }
            EntityKind.LOW_BARRIER -> drawBarrier(canvas, x, y, size, false)
            EntityKind.DUCK_GATE -> drawBarrier(canvas, x, y, size, true)
            EntityKind.CONE -> {
                paint.color = Color.rgb(243, 76, 23)
                path.reset(); path.moveTo(x, y - size); path.lineTo(x - size * 0.45f, y); path.lineTo(x + size * 0.45f, y); path.close(); canvas.drawPath(path, paint)
                paint.color = Color.WHITE; canvas.drawRect(x - size * 0.28f, y - size * 0.48f, x + size * 0.28f, y - size * 0.34f, paint)
            }
            EntityKind.MANHOLE -> {
                paint.color = Color.rgb(72, 73, 80)
                canvas.drawOval(RectF(x - size * 0.6f, y - size * 0.35f, x + size * 0.6f, y), paint)
                stroke.color = Color.rgb(190, 191, 199); stroke.strokeWidth = size * 0.08f
                canvas.drawOval(RectF(x - size * 0.6f, y - size * 0.35f, x + size * 0.6f, y), stroke)
            }
            EntityKind.CART -> {
                paint.color = Color.rgb(126, 73, 28)
                canvas.drawRect(x - size * 0.7f, y - size * 0.75f, x + size * 0.7f, y - size * 0.15f, paint)
                paint.color = Color.rgb(35, 40, 43)
                canvas.drawCircle(x - size * 0.45f, y, size * 0.2f, paint); canvas.drawCircle(x + size * 0.45f, y, size * 0.2f, paint)
            }
            EntityKind.ROLLING_TIRE -> {
                paint.color = Color.rgb(30, 31, 35); canvas.drawCircle(x, y - size * 0.48f, size * 0.45f, paint)
                paint.color = Color.rgb(111, 111, 118); canvas.drawCircle(x, y - size * 0.48f, size * 0.19f, paint)
            }
            EntityKind.TRAFFIC_CAR, EntityKind.TRAFFIC_TRUCK -> drawVehicle(canvas, x, y, size, entity.kind == EntityKind.TRAFFIC_TRUCK)
            EntityKind.FINISH_COOP -> drawFinish(canvas, x, y, size)
        }
    }

    private fun drawCorn(canvas: Canvas, x: Float, y: Float, size: Float) {
        paint.color = Color.rgb(255, 191, 24)
        canvas.drawOval(RectF(x - size * 0.24f, y - size * 1.1f, x + size * 0.24f, y - size * 0.2f), paint)
        paint.color = Color.rgb(37, 144, 52)
        path.reset(); path.moveTo(x, y); path.quadTo(x - size * 0.8f, y - size * 0.55f, x - size * 0.22f, y - size * 0.8f); path.close(); canvas.drawPath(path, paint)
        path.reset(); path.moveTo(x, y); path.quadTo(x + size * 0.8f, y - size * 0.55f, x + size * 0.22f, y - size * 0.8f); path.close(); canvas.drawPath(path, paint)
    }

    private fun drawBarrier(canvas: Canvas, x: Float, y: Float, size: Float, overhead: Boolean) {
        paint.color = Color.rgb(112, 62, 28)
        val postTop = if (overhead) y - size * 1.7f else y - size * 0.82f
        canvas.drawRect(x - size * 0.75f, postTop, x - size * 0.58f, y, paint)
        canvas.drawRect(x + size * 0.58f, postTop, x + size * 0.75f, y, paint)
        val boardTop = if (overhead) y - size * 1.65f else y - size * 0.78f
        paint.color = Color.rgb(250, 194, 24)
        canvas.drawRoundRect(RectF(x - size, boardTop, x + size, boardTop + size * 0.34f), size * 0.08f, size * 0.08f, paint)
        stroke.color = Color.rgb(49, 42, 30); stroke.strokeWidth = size * 0.14f
        repeat(4) { i ->
            val sx = x - size * 0.9f + i * size * 0.5f
            canvas.drawLine(sx, boardTop + size * 0.31f, sx + size * 0.28f, boardTop + size * 0.03f, stroke)
        }
    }

    private fun drawVehicle(canvas: Canvas, x: Float, y: Float, size: Float, truck: Boolean) {
        val w = size * if (truck) 2.2f else 1.6f
        val h = size * if (truck) 0.85f else 0.65f
        paint.color = if (truck) Color.rgb(34, 124, 171) else Color.rgb(219, 52, 34)
        canvas.drawRoundRect(RectF(x - w / 2f, y - h, x + w / 2f, y - size * 0.16f), size * 0.12f, size * 0.12f, paint)
        paint.color = Color.rgb(198, 234, 245)
        canvas.drawRect(x - w * 0.24f, y - h * 0.9f, x + w * 0.2f, y - h * 0.55f, paint)
        paint.color = Color.rgb(31, 32, 36)
        canvas.drawCircle(x - w * 0.28f, y - size * 0.12f, size * 0.16f, paint)
        canvas.drawCircle(x + w * 0.28f, y - size * 0.12f, size * 0.16f, paint)
    }

    private fun drawFinish(canvas: Canvas, x: Float, y: Float, size: Float) {
        paint.color = Color.rgb(118, 63, 27)
        canvas.drawRect(x - size * 1.25f, y - size * 2.0f, x - size, y, paint)
        canvas.drawRect(x + size, y - size * 2.0f, x + size * 1.25f, y, paint)
        paint.color = Color.rgb(247, 228, 171)
        canvas.drawRect(x - size * 1.35f, y - size * 2.0f, x + size * 1.35f, y - size * 1.65f, paint)
        paint.color = Color.rgb(207, 54, 31)
        val roof = Path().apply { moveTo(x - size * 1.4f, y - size * 2f); lineTo(x, y - size * 2.75f); lineTo(x + size * 1.4f, y - size * 2f); close() }
        canvas.drawPath(roof, paint)
    }

    private fun drawPlayer(canvas: Canvas, snapshot: GameSnapshot) {
        val player = snapshot.player
        val runPose = player.pose == PlayerPose.RUN || player.pose == PlayerPose.HOP_LEFT || player.pose == PlayerPose.HOP_RIGHT
        val runFrame = ((snapshot.distance * 0.86f).toInt() % 4 + 4) % 4
        val runPhase = snapshot.distance * 1.35f
        val centerX = width * (0.5f + player.lanePosition * GameplayProjection.laneContactSpacing)
        val groundY = height * GameplayProjection.contactY
        val lift = player.verticalOffset * height * 0.135f
        val size = width * 0.26f
        val duckScaleY = 1f - player.duckAmount * 0.38f
        val hopLean = when (player.pose) { PlayerPose.HOP_LEFT -> -10f; PlayerPose.HOP_RIGHT -> 10f; else -> 0f }
        val runLean = if (player.pose == PlayerPose.RUN) sin(runPhase) * 1.2f else 0f
        val shadowLift = (player.height / GameTuning.jumpHeight).coerceIn(0f, 1f)

        paint.color = Color.argb((112 * (1f - shadowLift * 0.58f)).toInt(), 0, 0, 0)
        val shadowWidth = size * (0.76f - shadowLift * 0.20f)
        val contactY = groundY + GameTuning.playerGroundEmbedDp * density
        canvas.drawOval(RectF(centerX - shadowWidth, contactY - 1f * density, centerX + shadowWidth, contactY + 10f * density), paint)
        drawRunDust(canvas, snapshot, centerX, size, runFrame, contactY)

        val art = selectedSkin.artSet()
        val frame = when (player.pose) {
            PlayerPose.JUMP -> art.jump
            PlayerPose.DUCK -> art.duck
            PlayerPose.HIT -> art.hit
            PlayerPose.WIN -> art.win
            PlayerPose.RUN, PlayerPose.HOP_LEFT, PlayerPose.HOP_RIGHT -> when (runFrame) {
                0 -> art.runContactLeft
                1, 3 -> art.runPass
                else -> art.runContactRight
            }
        }
        val bitmap = bitmap(frame.drawableRes) ?: bitmap(R.drawable.chicken_runner)
        val spriteHeight = width * if (player.pose == PlayerPose.DUCK) {
            GameTuning.playerDuckSpriteHeightScreenWidth
        } else {
            GameTuning.playerRunSpriteHeightScreenWidth
        }
        val visualContactY = contactY - lift
        val bitmapBottomY = visualContactY + spriteHeight * frame.footPaddingRatio
        canvas.save()
        canvas.rotate(hopLean + runLean, centerX, bitmapBottomY)
        val strideScaleY = when {
            !runPose || !player.grounded -> 1f
            runFrame == 0 || runFrame == 2 -> 0.992f
            else -> 1.008f
        }
        // Scale around the physics contact point: body motion never pulls the planted foot up.
        canvas.scale(1f, duckScaleY * strideScaleY, centerX, visualContactY)
        if (bitmap != null) {
            paint.alpha = 255
            paint.color = Color.WHITE
            val h = spriteHeight
            val w = h * bitmap.width / bitmap.height.toFloat()
            canvas.drawBitmap(bitmap, null, RectF(centerX - w / 2f, bitmapBottomY - h, centerX + w / 2f, bitmapBottomY), paint)
        } else {
            drawProceduralChicken(canvas, centerX, bitmapBottomY, size, snapshot)
        }
        canvas.restore()
    }

    private fun drawRunDust(canvas: Canvas, snapshot: GameSnapshot, centerX: Float, size: Float, runFrame: Int, groundY: Float) {
        if (snapshot.player.pose != PlayerPose.RUN && snapshot.player.pose != PlayerPose.HOP_LEFT && snapshot.player.pose != PlayerPose.HOP_RIGHT) return
        if (runFrame != 0 && runFrame != 2) return
        repeat(3) { index ->
            val life = 0.18f + index * 0.24f
            val side = if (index % 2 == 0) -1f else 1f
            val x = centerX + side * size * (0.18f + life * 0.72f)
            val y = groundY - life * size * 0.22f
            val radius = size * (0.10f - life * 0.065f)
            paint.color = Color.argb((92 * (1f - life)).toInt(), 230, 211, 176)
            canvas.drawCircle(x, y, radius.coerceAtLeast(1f), paint)
        }
    }

    private fun drawProceduralChicken(canvas: Canvas, x: Float, y: Float, size: Float, snapshot: GameSnapshot) {
        val bodyColor = when (selectedSkin) {
            ChickenSkin.CLASSIC, ChickenSkin.FARMER, ChickenSkin.RACER -> Color.rgb(250, 247, 235)
            ChickenSkin.GOLDEN -> Color.rgb(255, 205, 54)
        }
        paint.color = bodyColor
        canvas.drawOval(RectF(x - size * 0.66f, y - size * 1.42f, x + size * 0.66f, y), paint)
        canvas.drawCircle(x, y - size * 1.45f, size * 0.46f, paint)
        stroke.color = Color.rgb(115, 69, 38); stroke.strokeWidth = size * 0.045f
        canvas.drawOval(RectF(x - size * 0.66f, y - size * 1.42f, x + size * 0.66f, y), stroke)

        paint.color = Color.rgb(218, 43, 31)
        repeat(3) { i -> canvas.drawCircle(x - size * 0.18f + i * size * 0.18f, y - size * (1.87f + if (i == 1) 0.12f else 0f), size * 0.16f, paint) }
        paint.color = Color.rgb(255, 159, 18)
        val beak = Path().apply { moveTo(x - size * 0.16f, y - size * 1.47f); lineTo(x + size * 0.16f, y - size * 1.47f); lineTo(x, y - size * 1.18f); close() }
        canvas.drawPath(beak, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(x - size * 0.18f, y - size * 1.6f, size * 0.11f, paint)
        canvas.drawCircle(x + size * 0.18f, y - size * 1.6f, size * 0.11f, paint)
        paint.color = Color.rgb(35, 30, 27)
        canvas.drawCircle(x - size * 0.18f, y - size * 1.6f, size * 0.045f, paint)
        canvas.drawCircle(x + size * 0.18f, y - size * 1.6f, size * 0.045f, paint)

        paint.color = bodyColor
        val flap = sin(snapshot.distance * 1.9f) * size * 0.12f
        canvas.drawOval(RectF(x - size * 0.88f, y - size * 1.18f + flap, x - size * 0.35f, y - size * 0.45f + flap), paint)
        canvas.drawOval(RectF(x + size * 0.35f, y - size * 1.18f - flap, x + size * 0.88f, y - size * 0.45f - flap), paint)

        paint.color = Color.rgb(239, 132, 12)
        val step = sin(snapshot.distance * 2.6f) * size * 0.14f
        canvas.drawRoundRect(RectF(x - size * 0.28f, y - size * 0.08f, x - size * 0.14f, y + size * (0.42f + step / size)), size * 0.08f, size * 0.08f, paint)
        canvas.drawRoundRect(RectF(x + size * 0.14f, y - size * 0.08f, x + size * 0.28f, y + size * (0.42f - step / size)), size * 0.08f, size * 0.08f, paint)

        if (selectedSkin == ChickenSkin.FARMER) {
            paint.color = Color.rgb(174, 105, 26)
            canvas.drawOval(RectF(x - size * 0.58f, y - size * 2.03f, x + size * 0.58f, y - size * 1.82f), paint)
            canvas.drawRoundRect(RectF(x - size * 0.38f, y - size * 2.38f, x + size * 0.38f, y - size * 1.85f), size * 0.12f, size * 0.12f, paint)
        } else if (selectedSkin == ChickenSkin.RACER) {
            paint.color = Color.rgb(27, 119, 205)
            canvas.drawRoundRect(RectF(x - size * 0.38f, y - size * 1.76f, x + size * 0.38f, y - size * 1.48f), size * 0.1f, size * 0.1f, paint)
        }
    }

    private fun drawSpeedLines(canvas: Canvas, snapshot: GameSnapshot) {
        if (snapshot.player.pose != PlayerPose.HOP_LEFT && snapshot.player.pose != PlayerPose.HOP_RIGHT) return
        paint.color = Color.argb(100, 255, 241, 193)
        repeat(4) { i ->
            val y = height * (0.72f + i * 0.055f)
            val direction = if (snapshot.player.pose == PlayerPose.HOP_LEFT) 1f else -1f
            val start = width * 0.34f
            val end = start + direction * width * 0.12f
            canvas.drawRoundRect(RectF(minOf(start, end), y, maxOf(start, end), y + 4f), 3f, 3f, paint)
        }
    }

    private fun drawDebug(canvas: Canvas, snapshot: GameSnapshot) {
        stroke.color = Color.argb(220, 43, 230, 255)
        stroke.strokeWidth = 2f * density
        Lane.entries.forEach { lane ->
            val x = width * (0.5f + lane.coordinate * GameplayProjection.laneContactSpacing)
            canvas.drawLine(x, height * 0.70f, x, height * 0.94f, stroke)
        }
        stroke.color = Color.argb(235, 255, 70, 65)
        canvas.drawLine(0f, height * GameplayProjection.contactY, width.toFloat(), height * GameplayProjection.contactY, stroke)
        stroke.color = Color.argb(220, 43, 230, 255)
        val playerX = width * (0.5f + snapshot.player.lanePosition * GameplayProjection.laneContactSpacing)
        canvas.drawRect(
            RectF(
                playerX - width * GameTuning.playerLaneRadius * GameplayProjection.laneContactSpacing,
                height * 0.77f,
                playerX + width * GameTuning.playerLaneRadius * GameplayProjection.laneContactSpacing,
                height * GameplayProjection.contactY,
            ),
            stroke,
        )
        snapshot.entities.filter { it.rule != ObstacleRule.NONE }.forEach { entity ->
            val projection = project(entity.relativeDistance, entity.lanePosition)
            if (projection.y in height * 0.27f..height * 1.02f) {
                val base = width * 0.18f * projection.scale
                val bitmapName = when (entity.kind) {
                    EntityKind.HAY_BALE -> "obstacle_hay_bale"
                    EntityKind.LOW_BARRIER -> "obstacle_low_barrier"
                    EntityKind.DUCK_GATE -> "obstacle_duck_gate"
                    EntityKind.CONE -> "obstacle_cone"
                    EntityKind.MANHOLE -> "obstacle_manhole"
                    EntityKind.CART -> "obstacle_cart"
                    EntityKind.ROLLING_TIRE -> "obstacle_rolling_tire"
                    EntityKind.TRAFFIC_CAR -> "traffic_car"
                    EntityKind.TRAFFIC_TRUCK -> "traffic_truck"
                    EntityKind.FINISH_COOP -> "finish_coop"
                    else -> null
                }
                val artHeight = base * when (entity.kind) {
                    EntityKind.FINISH_COOP -> GameTuning.finishGateHeightFactor
                    EntityKind.DUCK_GATE -> 1.55f
                    EntityKind.TRAFFIC_CAR, EntityKind.TRAFFIC_TRUCK -> 1.0f
                    else -> 1.35f
                }
                val artAspect = bitmapName?.let(::bitmap)?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1f
                val artWidth = artHeight * artAspect
                stroke.color = Color.argb(225, 70, 255, 120)
                canvas.drawRect(
                    RectF(projection.x - artWidth / 2f, projection.y - artHeight, projection.x + artWidth / 2f, projection.y),
                    stroke,
                )

                val spacing = abs(project(entity.relativeDistance, entity.lanePosition + 1f).x - projection.x)
                val physicsHalfWidth = HazardKinematics.hazardHalfLane(entity.collisionProfile) * spacing
                stroke.color = Color.argb(235, 255, 65, 210)
                canvas.drawRect(
                    RectF(projection.x - physicsHalfWidth, projection.y - base * 0.48f, projection.x + physicsHalfWidth, projection.y),
                    stroke,
                )
            }
        }
        paint.color = Color.argb(170, 0, 0, 0)
        canvas.drawRect(8f, height * 0.48f, width * 0.58f, height * 0.61f, paint)
        paint.color = Color.WHITE
        paint.textSize = 13f * density
        canvas.drawText("lane=${snapshot.player.lane} x=${"%.2f".format(snapshot.player.lanePosition)}", 16f, height * 0.52f, paint)
        canvas.drawText("speed=${"%.2f".format(snapshot.speed)} pattern=${snapshot.currentPattern}", 16f, height * 0.56f, paint)
        canvas.drawText("entities=${snapshot.entities.size} progress=${snapshot.progressPercent}% fps=${measuredFps.toInt()}", 16f, height * 0.60f, paint)
    }

    private fun updateFps() {
        val now = System.nanoTime()
        if (fpsWindowStartNanos == 0L) fpsWindowStartNanos = now
        fpsFrameCount++
        val elapsedNanos = now - fpsWindowStartNanos
        if (elapsedNanos >= 1_000_000_000L) {
            measuredFps = fpsFrameCount * 1_000_000_000f / elapsedNanos
            fpsFrameCount = 0
            fpsWindowStartNanos = now
        }
    }

    private data class Projection(val x: Float, val y: Float, val scale: Float)

    private fun project(relativeDistance: Float, lanePosition: Float): Projection {
        val p = GameplayProjection.project(relativeDistance, lanePosition)
        return Projection(width * p.x, height * p.y, p.scale)
    }

    private fun roadHorizonY(): Float = height * GameplayProjection.horizonY

    private fun bitmap(name: String): Bitmap? = drawableIds[name]?.let(::bitmap)

    private fun bitmap(resourceId: Int): Bitmap? = bitmapCache.getOrPut(resourceId) {
        BitmapFactory.decodeResource(resources, resourceId)
    }

    private val drawableIds = mapOf(
        "bg_gameplay_horizon_v4" to R.drawable.bg_gameplay_horizon_v4,
        "bg_gameplay_horizon_v3" to R.drawable.bg_gameplay_horizon_v3,
        "bg_gameplay_farm" to R.drawable.bg_gameplay_farm,
        "roadside_props_v3" to R.drawable.roadside_props_v3,
        "road_asphalt_texture" to R.drawable.road_asphalt_texture,
        "roadside_flame_accent" to R.drawable.roadside_flame_accent,
        "chicken_run_contact_left" to R.drawable.chicken_run_contact_left,
        "chicken_run_pass" to R.drawable.chicken_run_pass,
        "chicken_run_contact_right" to R.drawable.chicken_run_contact_right,
        "chicken_runner" to R.drawable.chicken_runner,
        "chicken_jump" to R.drawable.chicken_jump,
        "chicken_duck" to R.drawable.chicken_duck,
        "chicken_hit" to R.drawable.chicken_hit,
        "chicken_win" to R.drawable.chicken_win,
        "pickup_coin" to R.drawable.pickup_coin,
        "pickup_corn" to R.drawable.pickup_corn,
        "pickup_golden_egg" to R.drawable.pickup_golden_egg,
        "obstacle_hay_bale" to R.drawable.obstacle_hay_bale,
        "obstacle_low_barrier" to R.drawable.obstacle_low_barrier,
        "obstacle_duck_gate" to R.drawable.obstacle_duck_gate,
        "obstacle_cone" to R.drawable.obstacle_cone,
        "obstacle_manhole" to R.drawable.obstacle_manhole,
        "obstacle_cart" to R.drawable.obstacle_cart,
        "obstacle_rolling_tire" to R.drawable.obstacle_rolling_tire,
        "traffic_car" to R.drawable.traffic_car,
        "traffic_truck" to R.drawable.traffic_truck,
        "finish_coop" to R.drawable.finish_coop,
    )

}
