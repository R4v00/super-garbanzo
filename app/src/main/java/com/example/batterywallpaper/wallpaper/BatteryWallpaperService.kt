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
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterReceiver(batteryReceiver)
            handler.removeCallbacks(drawRunnable)
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
                    canvas.drawColor(Color.parseColor("#101010"))
                    drawBattery(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            scheduleNextFrame()
        }

        private fun drawBattery(canvas: Canvas) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            val batteryWidth = width * 0.6f
            val batteryHeight = height * 0.3f
            val originX = (width - batteryWidth) / 2f
            val originY = (height - batteryHeight) / 2f

            outlinePaint.strokeWidth = batteryWidth * 0.02f
            outlinePaint.alpha = 255
            batteryBounds.set(originX, originY, originX + batteryWidth, originY + batteryHeight)

            val wobble = outlinePaint.strokeWidth * 1.2f
            val path = Path().apply {
                moveTo(batteryBounds.left + random.nextFloat() * wobble, batteryBounds.top + random.nextFloat() * wobble)
                lineTo(batteryBounds.right + random.nextFloat() * wobble, batteryBounds.top + random.nextFloat() * wobble)
                lineTo(batteryBounds.right + random.nextFloat() * wobble, batteryBounds.bottom + random.nextFloat() * wobble)
                lineTo(batteryBounds.left + random.nextFloat() * wobble, batteryBounds.bottom + random.nextFloat() * wobble)
                close()
            }
            canvas.drawPath(path, outlinePaint)

            val capWidth = batteryWidth * 0.08f
            val capHeight = batteryHeight * 0.22f
            val capPath = Path().apply {
                moveTo(
                    batteryBounds.right + random.nextFloat() * wobble,
                    batteryBounds.centerY() - capHeight / 2f + random.nextFloat() * wobble
                )
                lineTo(
                    batteryBounds.right + capWidth + random.nextFloat() * wobble,
                    batteryBounds.centerY() - capHeight / 2f + random.nextFloat() * wobble
                )
                lineTo(
                    batteryBounds.right + capWidth + random.nextFloat() * wobble,
                    batteryBounds.centerY() + capHeight / 2f + random.nextFloat() * wobble
                )
                lineTo(
                    batteryBounds.right + random.nextFloat() * wobble,
                    batteryBounds.centerY() + capHeight / 2f + random.nextFloat() * wobble
                )
                close()
            }
            canvas.drawPath(capPath, outlinePaint)

            val gaugeColor = when {
                batteryLevel <= 10f -> Color.parseColor("#E53935")
                batteryLevel <= 20f -> Color.parseColor("#FFB300")
                else -> Color.parseColor("#4CAF50")
            }
            fillPaint.color = gaugeColor

            val innerPadding = outlinePaint.strokeWidth * 3f
            val fillWidth = (batteryWidth - innerPadding * 2) * (batteryLevel / 100f)
            val fillHeight = batteryHeight - innerPadding * 2
            val wobbleOffset = sin(nowSeconds() * 2f) * outlinePaint.strokeWidth
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
                val y = batteryBounds.top + innerPadding + spacing * (index + 1) + cos(nowSeconds() * 3 + index) * 1.5f
                canvas.drawLine(
                    batteryBounds.left + innerPadding,
                    y,
                    batteryBounds.left + innerPadding + outlinePaint.strokeWidth * 2.5f,
                    y,
                    outlinePaint.apply { alpha = 160 }
                )
            }

            val label = "${batteryLevel.roundToInt()}%"
            val textWidth = textPaint.measureText(label)
            val textX = batteryBounds.centerX() - textWidth / 2f
            val textY = batteryBounds.bottom + textPaint.textSize + outlinePaint.strokeWidth
            canvas.drawText(label, textX, textY, textPaint)
        }

        private fun nowSeconds(): Float = SystemClock.elapsedRealtime() / 1000f
    }
}
