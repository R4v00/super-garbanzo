package com.example.batterywallpaper.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.batterywallpaper.data.WallpaperSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class BatteryWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BatteryEngine()

    private inner class BatteryEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val random = Random(System.currentTimeMillis())
        private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 6f
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.GREEN
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
        }
        private val batteryBounds = RectF()

        private var visible = false
        private var batteryLevel = 50f
        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
            }
        }

        private val wallpaperScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val settingsRepository = WallpaperSettingsRepository(this@BatteryWallpaperService)
        private var wallpaperSettings = com.example.batterywallpaper.data.WallpaperSettings(1f, 0.6f, 0.3f, 0, 0)

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level / scale.toFloat()) * 100f
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            wallpaperScope.launch {
                settingsRepository.wallpaperSettings.collect { settings ->
                    wallpaperSettings = settings
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
                scheduleNextFrame()
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
            handler.postDelayed(drawRunnable, 16L)
        }

        private fun drawFrame() {
            if (!visible) return
            val holder = surfaceHolder ?: return

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    canvas.drawColor(wallpaperSettings.backgroundColor)
                    drawBattery(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            scheduleNextFrame()
        }

        private fun drawBattery(canvas: Canvas) {
            val screenWidth = canvas.width.toFloat()
            val screenHeight = canvas.height.toFloat()

            val batteryWidth = screenWidth * wallpaperSettings.batteryWidth
            val batteryHeight = screenWidth * wallpaperSettings.batteryHeight // Use screenWidth to maintain aspect ratio

            val originX = (screenWidth - batteryWidth) / 2f
            val originY = (screenHeight - batteryHeight) / 2f

            outlinePaint.strokeWidth = batteryWidth * 0.04f
            outlinePaint.alpha = 255
            batteryBounds.set(originX, originY, originX + batteryWidth, originY + batteryHeight)

            val wobble = outlinePaint.strokeWidth * 1.2f * wallpaperSettings.animationLevel
            val path = Path().apply {
                moveTo(batteryBounds.left + random.nextFloat() * wobble, batteryBounds.top + random.nextFloat() * wobble)
                lineTo(batteryBounds.right + random.nextFloat() * wobble, batteryBounds.top + random.nextFloat() * wobble)
                lineTo(batteryBounds.right + random.nextFloat() * wobble, batteryBounds.bottom + random.nextFloat() * wobble)
                lineTo(batteryBounds.left + random.nextFloat() * wobble, batteryBounds.bottom + random.nextFloat() * wobble)
                close()
            }
            canvas.drawPath(path, outlinePaint)

            val capWidth = batteryWidth * 0.1f
            val capHeight = batteryHeight * 0.25f
            val capPath = Path().apply {
                val capOriginX = batteryBounds.right
                val capOriginY = batteryBounds.centerY() - capHeight / 2f
                moveTo(capOriginX, capOriginY + random.nextFloat() * wobble)
                lineTo(capOriginX + capWidth, capOriginY + random.nextFloat() * wobble)
                lineTo(capOriginX + capWidth, capOriginY + capHeight - random.nextFloat() * wobble)
                lineTo(capOriginX, capOriginY + capHeight - random.nextFloat() * wobble)
                close()
            }
            canvas.drawPath(capPath, outlinePaint)

            fillPaint.color = wallpaperSettings.batteryColor

            val innerPadding = outlinePaint.strokeWidth * 3f
            val fillWidth = (batteryWidth - innerPadding * 2) * (batteryLevel / 100f)
            val fillHeight = batteryHeight - innerPadding * 2
            val wobbleOffset = sin(nowSeconds() * 2f * wallpaperSettings.animationLevel) * outlinePaint.strokeWidth
            val fillRect = RectF(
                batteryBounds.left + innerPadding,
                batteryBounds.top + innerPadding + wobbleOffset,
                batteryBounds.left + innerPadding + fillWidth,
                batteryBounds.top + innerPadding + wobbleOffset + fillHeight
            )
            canvas.drawRoundRect(fillRect, outlinePaint.strokeWidth * 3, outlinePaint.strokeWidth * 3, fillPaint)

            val tickCount = 4
            val spacing = fillHeight / (tickCount + 1)
            repeat(tickCount) { index ->
                val y = batteryBounds.top + innerPadding + spacing * (index + 1) + cos(nowSeconds() * 3 * wallpaperSettings.animationLevel + index) * 1.5f
                canvas.drawLine(
                    batteryBounds.left + innerPadding,
                    y,
                    batteryBounds.left + innerPadding + outlinePaint.strokeWidth * 2.5f,
                    y,
                    outlinePaint.apply { alpha = 160 }
                )
            }

            val label = "${batteryLevel.roundToInt()}%"
            textPaint.textSize = batteryHeight * 0.3f
            val textWidth = textPaint.measureText(label)
            val textX = batteryBounds.centerX() - textWidth / 2f
            val textY = batteryBounds.centerY() + textPaint.textSize / 2f
            canvas.drawText(label, textX, textY, textPaint)
        }

        private fun nowSeconds(): Float = SystemClock.elapsedRealtime() / 1000f
    }
}
