package com.example.batterywallpaper

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.batterywallpaper.data.WallpaperSettingsRepository
import com.example.batterywallpaper.ui.theme.BatteryAmber
import com.example.batterywallpaper.ui.theme.BatteryGreen
import com.example.batterywallpaper.ui.theme.BatteryRed
import com.example.batterywallpaper.ui.theme.BatteryWallpaperTheme
import com.example.batterywallpaper.wallpaper.BatteryWallpaperService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryWallpaperTheme {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val batteryLevel by rememberBatteryLevel(context)
    val animatedLevel by animateFloatAsState(targetValue = batteryLevel / 100f, label = "battery")
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val settingsRepository = remember { WallpaperSettingsRepository(context) }
    val settings by settingsRepository.wallpaperSettings.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wallpaper Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        SketchyBatteryPreview(
            batteryLevel = animatedLevel,
            modifier = Modifier.size(220.dp),
            textMeasurer = textMeasurer,
            settings = settings
        )

        Spacer(Modifier.height(24.dp))

        settings?.let { currentSettings ->
            SettingSlider(
                label = "Animation Level",
                value = currentSettings.animationLevel,
                onValueChange = { scope.launch { settingsRepository.setAnimationLevel(it) } }
            )
            SettingSlider(
                label = "Battery Size",
                value = currentSettings.batterySize,
                onValueChange = { scope.launch { settingsRepository.setBatterySize(it) } }
            )
        }

        // Color pickers - you can implement these
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { /* TODO: Implement battery color picker */ }) {
                Text("Battery Color")
            }
            Button(onClick = { /* TODO: Implement background color picker */ }) {
                Text("Background Color")
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            scope.launch { launchWallpaperSettings(context) }
        }, shape = RoundedCornerShape(20.dp)) {
            Text(text = "Set as Live Wallpaper")
        }
    }
}

@Composable
private fun SettingSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = value, onValueChange = onValueChange)
    }
}

private suspend fun launchWallpaperSettings(context: Context) {
    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            android.content.ComponentName(context, BatteryWallpaperService::class.java)
        )
    }
    val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
    val target = if (intent.resolveActivity(context.packageManager) != null) intent else chooserIntent
    target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(target)
}

@Composable
private fun rememberBatteryLevel(context: Context): androidx.compose.runtime.State<Float> {
    val state = remember { mutableStateOf(50f) }
    val latestContext by rememberUpdatedState(context)

    DisposableEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val batteryPct = intent?.extractBatteryLevel() ?: return
                state.value = batteryPct
            }
        }
        val sticky = latestContext.registerReceiver(receiver, filter)
        sticky?.extractBatteryLevel()?.let { state.value = it }

        onDispose { latestContext.unregisterReceiver(receiver) }
    }

    return state
}

private fun Intent.extractBatteryLevel(): Float {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val batteryPct = (level / scale.toFloat()) * 100f
    return batteryPct.coerceIn(0f, 100f)
}

@Composable
private fun SketchyBatteryPreview(
    modifier: Modifier = Modifier,
    batteryLevel: Float,
    textMeasurer: TextMeasurer,
    settings: com.example.batterywallpaper.data.WallpaperSettings?
) {
    val gaugeColor = settings?.batteryColor?.let { Color(it) } ?: when {
        batteryLevel <= 0.1f -> BatteryRed
        batteryLevel <= 0.2f -> BatteryAmber
        else -> BatteryGreen
    }
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = settings?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background

    Canvas(modifier = modifier.background(backgroundColor)) {
        scale(settings?.batterySize ?: 1f) {
            drawSketchyBattery(
                level = batteryLevel,
                gaugeColor = gaugeColor,
                strokeColor = strokeColor,
                random = Random(System.currentTimeMillis()),
                textMeasurer = textMeasurer,
                animationLevel = settings?.animationLevel ?: 1f
            )
        }
    }
}

private fun DrawScope.drawSketchyBattery(
    level: Float,
    gaugeColor: Color,
    strokeColor: Color,
    random: Random,
    textMeasurer: TextMeasurer,
    animationLevel: Float
) {
    val width = size.width * 0.7f
    val height = size.height * 0.35f
    val origin = Offset(x = (size.width - width) / 2f, y = (size.height - height) / 2f)
    val strokeWidth = width * 0.025f

    fun Path.sketchyRect() {
        val wobble = strokeWidth * 0.8f * animationLevel
        moveTo(origin.x + random.nextFloat() * wobble, origin.y + random.nextFloat() * wobble)
        lineTo(origin.x + width + random.nextFloat() * wobble, origin.y + random.nextFloat() * wobble)
        lineTo(origin.x + width + random.nextFloat() * wobble, origin.y + height + random.nextFloat() * wobble)
        lineTo(origin.x + random.nextFloat() * wobble, origin.y + height + random.nextFloat() * wobble)
        close()
    }

    val bodyPath = Path().apply { sketchyRect() }
    drawPath(
        path = bodyPath,
        color = strokeColor,
        style = Stroke(width = strokeWidth)
    )

    val capWidth = width * 0.1f
    val capHeight = height * 0.25f
    translate(left = origin.x + width, top = origin.y + (height - capHeight) / 2f) {
        val capPath = Path().apply {
            val wobble = strokeWidth * 0.6f * animationLevel
            moveTo(random.nextFloat() * wobble, random.nextFloat() * wobble)
            lineTo(capWidth + random.nextFloat() * wobble, random.nextFloat() * wobble)
            lineTo(capWidth + random.nextFloat() * wobble, capHeight + random.nextFloat() * wobble)
            lineTo(random.nextFloat() * wobble, capHeight + random.nextFloat() * wobble)
            close()
        }
        drawPath(capPath, strokeColor, style = Stroke(width = strokeWidth * 0.9f))
    }

    val innerPadding = strokeWidth * 2.5f
    val fillWidth = (width - innerPadding * 2) * level
    val fillHeight = height - innerPadding * 2
    drawRoundRect(
        color = gaugeColor,
        topLeft = Offset(origin.x + innerPadding, origin.y + innerPadding),
        size = androidx.compose.ui.geometry.Size(fillWidth, fillHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth * 2, strokeWidth * 2)
    )

    val tickCount = 4
    val spacing = fillHeight / (tickCount + 1)
    repeat(tickCount) { index ->
        val tickY = origin.y + innerPadding + spacing * (index + 1)
        val tickLength = width * 0.05f + random.nextFloat() * strokeWidth * animationLevel
        drawLine(
            color = strokeColor.copy(alpha = 0.6f),
            start = Offset(origin.x + innerPadding, tickY),
            end = Offset(origin.x + innerPadding + tickLength, tickY),
            strokeWidth = strokeWidth * 0.8f
        )
    }

    val label = "${(level * 100).roundToInt()}%"
    val textLayoutResult = textMeasurer.measure(
        label,
        style = TextStyle(
            color = strokeColor,
            fontSize = (height * 0.3f).sp
        )
    )

    drawText(
        textLayoutResult,
        topLeft = Offset(
            x = origin.x + (width - textLayoutResult.size.width) / 2f,
            y = origin.y + (height - textLayoutResult.size.height) / 2f
        )
    )
}
