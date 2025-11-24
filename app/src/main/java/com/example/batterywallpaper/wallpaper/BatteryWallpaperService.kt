package com.example.batterywallpaper.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.batterywallpaper.data.BatteryStyle
import com.example.batterywallpaper.data.WallpaperSettings
import com.example.batterywallpaper.data.WallpaperSettingsRepository
import com.example.batterywallpaper.ui.theme.BatteryAmberValue
import com.example.batterywallpaper.ui.theme.BatteryGreenValue
import com.example.batterywallpaper.ui.theme.BatteryRedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class BatteryWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BatteryEngine()

    private inner class BatteryEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }

        // Paths for drawing, reused to avoid allocations
        private val futuristicBodyPath = Path()
        private val futuristicCapPath = Path()
        private val futuristicClipPath = Path()
        private val cartoonishBodyPath = Path()
        private val cartoonishCapPath = Path()

        private var visible = false
        private var batteryLevel = 50f
        private val drawRunnable: Runnable = Runnable { drawFrame() }

        private val wallpaperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val settingsRepository = WallpaperSettingsRepository(this@BatteryWallpaperService)
        private var wallpaperSettings: WallpaperSettings? = null

        private val random = Random(System.currentTimeMillis())

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val newBatteryLevel = (level / scale.toFloat()) * 100f
                    // Redraw only if the battery level has changed
                    if (batteryLevel.roundToInt() != newBatteryLevel.roundToInt()) {
                        batteryLevel = newBatteryLevel
                        if (visible) {
                            handler.post(drawRunnable)
                        }
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)

            applicationContext.registerReceiver(null, filter)?.let { sticky ->
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level / scale.toFloat()) * 100f
                }
            }

            wallpaperScope.launch {
                settingsRepository.wallpaperSettings.collect { settings ->
                    val isFirstLoad = wallpaperSettings == null
                    wallpaperSettings = settings
                    if (isFirstLoad && visible) {
                        handler.post(drawRunnable)
                    }
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterReceiver(batteryReceiver)
            handler.removeCallbacks(drawRunnable)
            wallpaperScope.coroutineContext.cancelChildren()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun scheduleNextFrame() {
            handler.removeCallbacks(drawRunnable)
            // ~30 fps for cartoonish style
            handler.postDelayed(drawRunnable, 33L)
        }

        private fun nowSeconds(): Float = SystemClock.elapsedRealtime() / 1000f

        private fun drawFrame() {
            if (!visible) return

            val currentHolder = surfaceHolder
            val currentSettings = wallpaperSettings

            if (currentHolder != null && currentSettings != null) {
                val canvas = currentHolder.lockCanvas() ?: return
                try {
                    canvas.drawColor(currentSettings.backgroundColor)
                    drawBattery(canvas, currentSettings)
                } finally {
                    currentHolder.unlockCanvasAndPost(canvas)
                }

                // Cartoonish style has animations, so we need to keep drawing.
                // Futuristic style only needs to be redrawn on battery level change.
                if (currentSettings.batteryStyle == BatteryStyle.Cartoonish) {
                    scheduleNextFrame()
                }
            }
        }

        private fun drawBattery(canvas: Canvas, settings: WallpaperSettings) {
            when (settings.batteryStyle) {
                BatteryStyle.Cartoonish -> drawCartoonishBattery(canvas, settings)
                BatteryStyle.Futuristic -> drawFuturisticBattery(canvas, settings)
            }
        }

        private fun drawFuturisticBattery(canvas: Canvas, settings: WallpaperSettings) {
            val screenWidth = canvas.width.toFloat()
            val screenHeight = canvas.height.toFloat()

            val batteryWidth = screenWidth * settings.batteryWidth
            val batteryHeight = screenWidth * settings.batteryHeight

            val originX = (screenWidth - batteryWidth) / 2f
            val originY = (screenHeight - batteryHeight) / 2f
            val strokeWidth = batteryWidth * 0.05f

            outlinePaint.strokeWidth = strokeWidth
            outlinePaint.color = settings.edgesColor
            outlinePaint.pathEffect = null

            futuristicBodyPath.rewind()
            futuristicBodyPath.apply {
                moveTo(originX + batteryWidth * 0.2f, originY)
                lineTo(originX + batteryWidth * 0.8f, originY)
                lineTo(originX + batteryWidth, originY + batteryHeight * 0.1f)
                lineTo(originX + batteryWidth, originY + batteryHeight * 0.9f)
                lineTo(originX + batteryWidth * 0.8f, originY + batteryHeight)
                lineTo(originX + batteryWidth * 0.2f, originY + batteryHeight)
                lineTo(originX, originY + batteryHeight * 0.9f)
                lineTo(originX, originY + batteryHeight * 0.1f)
                close()
            }
            canvas.drawPath(futuristicBodyPath, outlinePaint)

            val capWidth = batteryWidth * 0.3f
            val capHeight = batteryHeight * 0.05f
            futuristicCapPath.rewind()
            futuristicCapPath.apply {
                val capOriginX = originX + (batteryWidth - capWidth) / 2f
                val capOriginY = originY - capHeight
                addRect(capOriginX, capOriginY, capOriginX + capWidth, originY, Path.Direction.CW)
                close()
            }
            canvas.drawPath(futuristicCapPath, outlinePaint)

            futuristicClipPath.rewind()
            futuristicClipPath.addPath(futuristicBodyPath)

            canvas.save()
            canvas.clipPath(futuristicClipPath)

            val batteryPercent = batteryLevel / 100f

            fillPaint.color = if (settings.batteryColor != 0) {
                settings.batteryColor
            } else {
                when {
                    batteryPercent < 0.11f -> BatteryRedValue
                    batteryPercent < 0.20f -> BatteryAmberValue
                    else -> BatteryGreenValue
                }
            }

            if (fillPaint.color != 0) {
                val fillHeight = batteryHeight * batteryPercent
                val fillY = originY + batteryHeight - fillHeight
                canvas.drawRect(
                    originX,
                    fillY,
                    originX + batteryWidth,
                    originY + batteryHeight,
                    fillPaint
                )
            }

            canvas.restore()

            val label = "${batteryLevel.roundToInt()}%"
            textPaint.color = settings.textColor
            textPaint.textSize = batteryHeight * settings.textSize * 0.3f

            val textX = originX + batteryWidth / 2f
            val textY = originY + batteryHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(label, textX, textY, textPaint)
        }

        private fun drawCartoonishBattery(canvas: Canvas, settings: WallpaperSettings) {
            val screenWidth = canvas.width.toFloat()
            val screenHeight = canvas.height.toFloat()

            val batteryWidth = screenWidth * settings.batteryWidth
            val batteryHeight = screenWidth * settings.batteryHeight

            val originX = (screenWidth - batteryWidth) / 2f
            val originY = (screenHeight - batteryHeight) / 2f

            val strokeWidth = batteryWidth * 0.04f
            outlinePaint.strokeWidth = strokeWidth
            outlinePaint.color = settings.edgesColor

            val cornerRadius = batteryWidth * 0.1f
            outlinePaint.pathEffect = CornerPathEffect(cornerRadius)

            val wobble = strokeWidth * 1.2f * settings.edgeAnimationLevel
            fun r(w: Float): Float = (random.nextFloat() - 0.5f) * w * 2f

            cartoonishBodyPath.rewind()
            cartoonishBodyPath.apply {
                moveTo(originX + r(wobble), originY + r(wobble))
                lineTo(originX + batteryWidth + r(wobble), originY + r(wobble))
                lineTo(originX + batteryWidth + r(wobble), originY + batteryHeight + r(wobble))
                lineTo(originX + r(wobble), originY + batteryHeight + r(wobble))
                close()
            }
            canvas.drawPath(cartoonishBodyPath, outlinePaint)

            val capWidth = batteryWidth * 0.1f
            val capHeight = batteryHeight * 0.25f
            cartoonishCapPath.rewind()
            cartoonishCapPath.apply {
                val capOriginX = originX + batteryWidth
                val capOriginY = originY + (batteryHeight - capHeight) / 2f
                moveTo(capOriginX + r(wobble), capOriginY + r(wobble))
                lineTo(capOriginX + capWidth + r(wobble), capOriginY + r(wobble))
                lineTo(capOriginX + capWidth + r(wobble), capOriginY + capHeight + r(wobble))
                lineTo(capOriginX + r(wobble), capOriginY + capHeight + r(wobble))
                close()
            }
            canvas.drawPath(cartoonishCapPath, outlinePaint)
            outlinePaint.pathEffect = null

            val batteryPercent = batteryLevel / 100f

            fillPaint.color = if (settings.batteryColor != 0) {
                settings.batteryColor
            } else {
                when {
                    batteryPercent < 0.11f -> BatteryRedValue
                    batteryPercent < 0.20f -> BatteryAmberValue
                    else -> BatteryGreenValue
                }
            }

            val wobbleOffset = sin(nowSeconds() * 2f * settings.gaugeAnimationLevel) * strokeWidth
            val innerPadding = strokeWidth * 1.5f
            if (fillPaint.color != 0) { // Don't draw if transparent
                val fillWidth = (batteryWidth - innerPadding * 2) * batteryPercent
                val fillHeight = batteryHeight - innerPadding * 2

                val fillRect = RectF(
                    originX + innerPadding,
                    originY + innerPadding + wobbleOffset,
                    originX + innerPadding + fillWidth,
                    originY + innerPadding + fillHeight + wobbleOffset
                )
                canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)
            }

            val label = "${batteryLevel.roundToInt()}%"
            textPaint.color = settings.textColor
            textPaint.textSize = batteryHeight * settings.textSize

            val textX = originX + batteryWidth / 2f + r(wobble * 0.2f)
            val textY = originY + batteryHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2f + wobbleOffset
            canvas.drawText(label, textX, textY, textPaint)
        }
    }
}