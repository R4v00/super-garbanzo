package com.example.batterywallpaper.drawing

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.batterywallpaper.data.BatteryStyle
import com.example.batterywallpaper.data.WallpaperSettings
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

fun DrawScope.drawBattery(
    settings: WallpaperSettings,
    level: Float,
    gaugeColor: Color,
    strokeColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
    random: Random
) {
    when (settings.batteryStyle) {
        BatteryStyle.Cartoonish -> drawCartoonishBattery(
            level = level,
            gaugeColor = gaugeColor,
            strokeColor = strokeColor,
            textColor = textColor,
            random = random,
            textMeasurer = textMeasurer,
            gaugeAnimationLevel = settings.gaugeAnimationLevel,
            edgeAnimationLevel = settings.edgeAnimationLevel,
            batteryWidth = settings.batteryWidth,
            batteryHeight = settings.batteryHeight,
            textSize = settings.textSize
        )
        BatteryStyle.Futuristic -> drawFuturisticBattery(
            level = level,
            gaugeColor = gaugeColor,
            strokeColor = strokeColor,
            textColor = textColor,
            textMeasurer = textMeasurer,
            settings = settings
        )
    }
}

private fun DrawScope.drawFuturisticBattery(
    level: Float,
    gaugeColor: Color,
    strokeColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
    settings: WallpaperSettings
) {
    val width = size.width * settings.batteryWidth
    val height = size.width * settings.batteryHeight // Use size.width to maintain aspect ratio
    val origin = Offset(x = (size.width - width) / 2f, y = (size.height - height) / 2f)
    val strokeWidth = width * 0.05f

    // Define the futuristic shape
    val body = Path().apply {
        moveTo(origin.x + width * 0.2f, origin.y)
        lineTo(origin.x + width * 0.8f, origin.y)
        lineTo(origin.x + width, origin.y + height * 0.2f)
        lineTo(origin.x + width, origin.y + height * 0.8f)
        lineTo(origin.x + width * 0.8f, origin.y + height)
        lineTo(origin.x + width * 0.2f, origin.y + height)
        lineTo(origin.x, origin.y + height * 0.8f)
        lineTo(origin.x, origin.y + height * 0.2f)
        close()
    }

    // Draw the outline
    drawPath(body, color = strokeColor, style = Stroke(width = strokeWidth))

    // Draw the cap
    val capWidth = width * 0.3f
    val capHeight = height * 0.1f
    val capPath = Path().apply {
        moveTo(origin.x + (width - capWidth) / 2f, origin.y)
        lineTo(origin.x + (width + capWidth) / 2f, origin.y)
        lineTo(origin.x + (width + capWidth) / 2f, origin.y - capHeight)
        lineTo(origin.x + (width - capWidth) / 2f, origin.y - capHeight)
        close()
    }
    drawPath(capPath, color = strokeColor, style = Stroke(width = strokeWidth))

    // Draw the gauge
    clipPath(body) {
        // Calculate the number of segments to draw
        val totalSegments = 10
        val segmentsToDraw = (level * totalSegments).roundToInt()

        val segmentHeight = height / totalSegments
        val segmentWidth = width * 0.8f
        val segmentSpacing = segmentHeight * 0.2f

        for (i in 0 until segmentsToDraw) {
            val y = origin.y + height - (i + 1) * segmentHeight + segmentSpacing / 2f
            drawRect(
                color = gaugeColor,
                topLeft = Offset(origin.x + (width - segmentWidth) / 2, y),
                size = Size(segmentWidth, segmentHeight - segmentSpacing)
            )
        }
    }

    // Draw the percentage text
    val label = "${(level * 100).roundToInt()}%"
    val textStyle = TextStyle(
        color = textColor,
        fontSize = (height * settings.textSize).sp,
        textAlign = TextAlign.Center
    )
    val textLayoutResult = textMeasurer.measure(label, style = textStyle)

    translate(
        left = origin.x + (width - textLayoutResult.size.width) / 2f,
        top = origin.y + (height - textLayoutResult.size.height) / 2f
    ) {
        drawText(textLayoutResult)
    }
}

private fun DrawScope.drawCartoonishBattery(
    level: Float,
    gaugeColor: Color,
    strokeColor: Color,
    textColor: Color,
    random: Random,
    textMeasurer: TextMeasurer,
    gaugeAnimationLevel: Float,
    edgeAnimationLevel: Float,
    batteryWidth: Float,
    batteryHeight: Float,
    textSize: Float
) {
    val width = size.width * batteryWidth
    val height = size.width * batteryHeight // Use size.width to maintain aspect ratio
    val origin = Offset(x = (size.width - width) / 2f, y = (size.height - height) / 2f)
    val strokeWidth = width * 0.04f

    val wobble = strokeWidth * 1.2f * edgeAnimationLevel

    fun r(w: Float): Float = (random.nextFloat() - 0.5f) * w * 2

    val cornerRadius = width * 0.1f

    val bodyPath = Path().apply {
        addRoundRect(
            roundRect = androidx.compose.ui.geometry.RoundRect(
                rect = Rect(origin, Size(width, height)),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }
    drawPath(
        path = bodyPath,
        color = strokeColor,
        style = Stroke(width = strokeWidth)
    )

    val capWidth = width * 0.1f
    val capHeight = height * 0.25f
    val capPath = Path().apply {
        val capOriginX = origin.x + width
        val capOriginY = origin.y + (height - capHeight) / 2f
        addRoundRect(
            roundRect = androidx.compose.ui.geometry.RoundRect(
                rect = Rect(Offset(capOriginX, capOriginY), Size(capWidth, capHeight)),
                cornerRadius = CornerRadius(cornerRadius / 2, cornerRadius / 2)
            )
        )
    }
    drawPath(capPath, strokeColor, style = Stroke(width = strokeWidth * 0.9f))

    val innerPadding = strokeWidth * 1.5f
    val fillWidth = (width - innerPadding * 2) * level
    val fillHeight = height - innerPadding * 2
    val wobbleOffset = sin(System.currentTimeMillis() / 500f * gaugeAnimationLevel) * strokeWidth

    if (gaugeColor.alpha > 0f) {
        drawRoundRect(
            color = gaugeColor,
            topLeft = Offset(origin.x + innerPadding, origin.y + innerPadding + wobbleOffset),
            size = Size(fillWidth, fillHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    }

    val label = "${(level * 100).roundToInt()}%"
    val textStyle = TextStyle(
        color = textColor,
        fontSize = (height * textSize).sp,
        textAlign = TextAlign.Center
    )
    val textLayoutResult = textMeasurer.measure(label, style = textStyle)

    translate(
        left = origin.x + (width - textLayoutResult.size.width) / 2f + r(wobble * 0.2f),
        top = origin.y + (height - textLayoutResult.size.height) / 2f + wobbleOffset
    ) {
        drawText(textLayoutResult)
    }
}