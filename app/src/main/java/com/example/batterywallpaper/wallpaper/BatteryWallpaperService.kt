package com.example.batterywallpaper.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.toArgb
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import kotlin.random.Random

class BatteryWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BatteryEngine()

    private inner class BatteryEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())

        private var visible = false
        private var batteryLevel = 50f
        private val drawRunnable: () -> Unit = { drawFrame() }

        private val wallpaperScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val settingsRepository = WallpaperSettingsRepository(this@BatteryWallpaperService)
        private var wallpaperSettings: WallpaperSettings

        private val random = Random(System.currentTimeMillis())

        init {
            wallpaperSettings = runBlocking {
                settingsRepository.wallpaperSettings.first()
            }
        }

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
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(batteryReceiver, filter)
            // Get initial battery level
            applicationContext.registerReceiver(null, filter)?.let { sticky ->
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level / scale.toFloat()) * 100f
                }
            }

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

            val canvas = holder.lockCanvas() ?: return
            try {
                // Clear canvas
                canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                // Draw background
                canvas.drawColor(wallpaperSettings.backgroundColor)
                // Draw battery
                drawBattery(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }

            scheduleNextFrame()
        }

        private fun drawBattery(canvas: Canvas) {
            when (wallpaperSettings.batteryStyle) {
                BatteryStyle.Cartoonish -> drawCartoonishBattery(canvas)
                BatteryStyle.Futuristic -> drawFuturisticBattery(canvas)
            }
        }

        private fun drawFuturisticBattery(canvas: Canvas) {
            val screenWidth = canvas.width.toFloat()
            val screenHeight = canvas.height.toFloat()

            val batteryWidth = screenWidth * wallpaperSettings.batteryWidth
            val batteryHeight = screenWidth * wallpaperSettings.batteryHeight // Maintain aspect ratio

            val originX = (screenWidth - batteryWidth) / 2f
            val originY = (screenHeight - batteryHeight) / 2f
            val strokeWidth = batteryWidth * 0.05f

            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = wallpaperSettings.edgesColor
                strokeWidth = this@BatteryEngine.strokeWidth
            }

            val body = Path().apply {
                moveTo(originX + batteryWidth * 0.2f, originY)
                lineTo(originX + batteryWidth * 0.8f, originY)
                lineTo(originX + batteryWidth, originY + batteryHeight * 0.2f)
                lineTo(originX + batteryWidth, originY + batteryHeight * 0.8f)
                lineTo(originX + batteryWidth * 0.8f, originY + batteryHeight)
                lineTo(originX + batteryWidth * 0.2f, originY + batteryHeight)
                lineTo(originX, originY + batteryHeight * 0.8f)
                lineTo(originX, originY + batteryHeight * 0.2f)
                close()
            }
            canvas.drawPath(body, outlinePaint)

            val capWidth = batteryWidth * 0.3f
            val capHeight = batteryHeight * 0.1f
            val capPath = Path().apply {
                moveTo(originX + (batteryWidth - capWidth) / 2f, originY)
                lineTo(originX + (batteryWidth + capWidth) / 2f, originY)
                lineTo(originX + (width + capWidth) / 2f, originY - capHeight)
                lineTo(originX + (width - capWidth) / 2f, originY - capHeight)
                close()
            }
            canvas.drawPath(capPath, outlinePaint)

            canvas.clipPath(body)

            val level = batteryLevel / 100f
            val totalSegments = 10
            val segmentsToDraw = (level * totalSegments).roundToInt()

            val segmentHeight = batteryHeight / totalSegments
            val segmentWidth = batteryWidth * 0.8f
            val segmentSpacing = segmentHeight * 0.2f

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (wallpaperSettings.batteryColor != 0) {
                    wallpaperSettings.batteryColor
                } else {
                    when {
                        level < 0.11f -> BatteryRedValue
                        level < 0.20f -> BatteryAmberValue
                        else -> BatteryGreenValue
                    }
                }.toInt()
            }

            for (i in 0 until segmentsToDraw) {
                val y = originY + batteryHeight - (i + 1) * segmentHeight + segmentSpacing / 2f
                canvas.drawRect(
                    originX + (batteryWidth - segmentWidth) / 2,
                    y,
                    originX + (batteryWidth + segmentWidth) / 2,
                    y + segmentHeight - segmentSpacing, fillPaint
                )
            }
        }

        private fun drawCartoonishBattery(canvas: Canvas) {
            val screenWidth = canvas.width.toFloat()
            val screenHeight = canvas.height.toFloat()

            val batteryWidth = screenWidth * wallpaperSettings.batteryWidth
            val batteryHeight = screenWidth * wallpaperSettings.batteryHeight // Maintain aspect ratio

            val originX = (screenWidth - batteryWidth) / 2f
            val originY = (screenHeight - batteryHeight) / 2f

            val strokeWidth = batteryWidth * 0.04f

            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = wallpaperSettings.edgesColor
                strokeWidth = this@BatteryEngine.strokeWidth
            }

            val cornerRadius = batteryWidth * 0.1f

            val bodyRect = RectF(originX, originY, originX + batteryWidth, originY + batteryHeight)
            canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, outlinePaint)

            val capWidth = batteryWidth * 0.1f
            val capHeight = batteryHeight * 0.25f
            val capOriginX = originX + batteryWidth
            val capOriginY = originY + (batteryHeight - capHeight) / 2f
            val capRect = RectF(capOriginX, capOriginY, capOriginX + capWidth, capOriginY + capHeight)
            canvas.drawRoundRect(capRect, cornerRadius / 2, cornerRadius / 2, outlinePaint)

            val batteryPercent = batteryLevel / 100f

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (wallpaperSettings.batteryColor != 0) {
                    wallpaperSettings.batteryColor
                } else {
                    when {
                        batteryPercent < 0.11f -> BatteryRedValue
                        batteryPercent < 0.20f -> BatteryAmberValue
                        else -> BatteryGreenValue
                    }
                }.toInt()
            }

            val innerPadding = strokeWidth * 1.5f
            if (fillPaint.color != 0) { // Don't draw if transparent
                val fillWidth = (batteryWidth - innerPadding * 2) * batteryPercent
                val fillHeight = batteryHeight - innerPadding * 2

                val fillRect = RectF(
                    originX + innerPadding,
                    originY + innerPadding,
                    originX + innerPadding + fillWidth,
                    originY + innerPadding + fillHeight
                )
                canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)
            }

            val label = "${batteryLevel.roundToInt()}%"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                color = wallpaperSettings.textColor
                textSize = batteryHeight * wallpaperSettings.textSize
            }

            val textX = originX + batteryWidth / 2f
            val textY = originY + batteryHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(label, textX, textY, textPaint)
        }
    }
}