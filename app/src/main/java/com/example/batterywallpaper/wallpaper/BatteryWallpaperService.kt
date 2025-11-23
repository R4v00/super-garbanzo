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
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
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
import kotlin.math.sin

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

        private var visible = false
        private var batteryLevel = 50f
        private val drawRunnable: () -> Unit = { drawFrame() }

        private val wallpaperScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val settingsRepository = WallpaperSettingsRepository(this@BatteryWallpaperService)
        private var wallpaperSettings: WallpaperSettings

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
            val screenWidth = canvas.width.toFloat()
            val screenHeight = canvas.height.toFloat()

            val batteryWidth = screenWidth * wallpaperSettings.batteryWidth
            val batteryHeight = screenWidth * wallpaperSettings.batteryHeight // Maintain aspect ratio

            val originX = (screenWidth - batteryWidth) / 2f
            val originY = (screenHeight - batteryHeight) / 2f

            val strokeWidth = batteryWidth * 0.04f
            outlinePaint.strokeWidth = strokeWidth
            outlinePaint.color = wallpaperSettings.edgesColor

            // --- Draw clean battery outline --- //
            val bodyPath = Path().apply {
                addRect(originX, originY, originX + batteryWidth, originY + batteryHeight, Path.Direction.CW)
            }
            canvas.drawPath(bodyPath, outlinePaint)

            val capWidth = batteryWidth * 0.1f
            val capHeight = batteryHeight * 0.25f
            val capPath = Path().apply {
                val capOriginX = originX + batteryWidth
                val capOriginY = originY + (batteryHeight - capHeight) / 2f
                addRect(capOriginX, capOriginY, capOriginX + capWidth, capOriginY + capHeight, Path.Direction.CW)
            }
            canvas.drawPath(capPath, outlinePaint)
            // --- End drawing clean outline --- //

            val batteryPercent = batteryLevel / 100f

            fillPaint.color = if (wallpaperSettings.batteryColor != 0) {
                wallpaperSettings.batteryColor
            } else {
                when {
                    batteryPercent < 0.11f -> BatteryRedValue
                    batteryPercent < 0.20f -> BatteryAmberValue
                    else -> BatteryGreenValue
                }
            }.toInt()

            val wobbleOffset = sin(nowSeconds() * 2f * wallpaperSettings.animationLevel) * strokeWidth
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
                canvas.drawRoundRect(fillRect, strokeWidth, strokeWidth, fillPaint)
            }

            val label = "${batteryLevel.roundToInt()}%"
            textPaint.color = wallpaperSettings.textColor
            val textTargetWidth = batteryWidth * 0.7f
            textPaint.textSize = getTextSizeForWidth(textPaint, label, textTargetWidth) * wallpaperSettings.textSize

            val textX = originX + batteryWidth / 2f
            val textY = originY + batteryHeight / 2f - (textPaint.ascent() + textPaint.descent()) / 2f + wobbleOffset
            canvas.drawText(label, textX, textY, textPaint)
        }

        private fun getTextSizeForWidth(paint: Paint, text: String, width: Float): Float {
            val testTextSize = 48f
            paint.textSize = testTextSize
            val textWidth = paint.measureText(text)
            return if (textWidth > 0) testTextSize * width / textWidth else testTextSize
        }

        private fun nowSeconds(): Float = SystemClock.elapsedRealtime() / 1000f
    }
}
