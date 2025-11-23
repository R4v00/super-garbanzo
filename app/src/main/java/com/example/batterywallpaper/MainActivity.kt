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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.batterywallpaper.data.WallpaperSettings
import com.example.batterywallpaper.data.WallpaperSettingsRepository
import com.example.batterywallpaper.ui.theme.BatteryWallpaperTheme
import com.example.batterywallpaper.wallpaper.BatteryWallpaperService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin
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

    var showBatteryColorPicker by remember { mutableStateOf(false) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showEdgesColorPicker by remember { mutableStateOf(false) }

    if (showBatteryColorPicker) {
        ColorPickerDialog(
            onColorSelected = { scope.launch { settingsRepository.setBatteryColor(it.toArgb()) } },
            onDismiss = { showBatteryColorPicker = false }
        )
    }

    if (showBackgroundColorPicker) {
        ColorPickerDialog(
            onColorSelected = { scope.launch { settingsRepository.setBackgroundColor(it.toArgb()) } },
            onDismiss = { showBackgroundColorPicker = false }
        )
    }

    if (showTextColorPicker) {
        ColorPickerDialog(
            onColorSelected = { scope.launch { settingsRepository.setTextColor(it.toArgb()) } },
            onDismiss = { showTextColorPicker = false }
        )
    }

    if (showEdgesColorPicker) {
        ColorPickerDialog(
            onColorSelected = { scope.launch { settingsRepository.setEdgesColor(it.toArgb()) } },
            onDismiss = { showEdgesColorPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wallpaper Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        SketchyBatteryPreview(
            batteryLevel = animatedLevel,
            modifier = Modifier.size(220.dp),
            textMeasurer = textMeasurer,
            settings = settings
        )

        settings?.let { currentSettings ->
            SettingSlider(
                label = "Animation Level",
                value = currentSettings.animationLevel,
                onValueChange = { scope.launch { settingsRepository.setAnimationLevel(it) } }
            )
            SettingSlider(
                label = "Battery Width",
                value = currentSettings.batteryWidth,
                onValueChange = { scope.launch { settingsRepository.setBatteryWidth(it) } }
            )
            SettingSlider(
                label = "Battery Height",
                value = currentSettings.batteryHeight,
                onValueChange = { scope.launch { settingsRepository.setBatteryHeight(it) } }
            )
            SettingSlider(
                label = "Text Size",
                value = currentSettings.textSize,
                onValueChange = { scope.launch { settingsRepository.setTextSize(it) } }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { showBatteryColorPicker = true }) {
                Text("Battery Color")
            }
            Button(onClick = { showBackgroundColorPicker = true }) {
                Text("Background Color")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { showTextColorPicker = true }) {
                Text("Text Color")
            }
            Button(onClick = { showEdgesColorPicker = true }) {
                Text("Edges Color")
            }
        }

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
        Slider(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ColorPickerDialog(onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val colors = listOf(
        Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta,
        Color(0xFFFFA500), Color(0xFF800080), Color(0xFF008000), Color(0xFF000080)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select a Color", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 48.dp)) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape)
                                .clickable { onColorSelected(color); onDismiss() }
                        )
                    }
                }
            }
        }
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
    settings: WallpaperSettings?
) {
    val gaugeColor = settings?.batteryColor?.let { color ->
        if (color == 0) {
            when {
                batteryLevel <= 0.11f -> Color.Red
                batteryLevel <= 0.2f -> Color.Yellow
                else -> Color.Green
            }
        } else {
            Color(color)
        }
    } ?: when {
        batteryLevel <= 0.11f -> Color.Red
        batteryLevel <= 0.2f -> Color.Yellow
        else -> Color.Green
    }
    val strokeColor = settings?.edgesColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val backgroundColor = settings?.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background
    val textColor = settings?.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.background(backgroundColor)) {
        drawSketchyBattery(
            level = batteryLevel,
            gaugeColor = gaugeColor,
            strokeColor = strokeColor,
            textColor = textColor,
            random = Random(System.currentTimeMillis()),
            textMeasurer = textMeasurer,
            animationLevel = settings?.animationLevel ?: 1f,
            batteryWidth = settings?.batteryWidth ?: 0.6f,
            batteryHeight = settings?.batteryHeight ?: 0.3f,
            textSize = settings?.textSize ?: 0.5f
        )
    }
}

private fun DrawScope.drawSketchyBattery(
    level: Float,
    gaugeColor: Color,
    strokeColor: Color,
    textColor: Color,
    random: Random,
    textMeasurer: TextMeasurer,
    animationLevel: Float,
    batteryWidth: Float,
    batteryHeight: Float,
    textSize: Float
) {
    val width = size.width * batteryWidth
    val height = size.width * batteryHeight // Use size.width to maintain aspect ratio
    val origin = Offset(x = (size.width - width) / 2f, y = (size.height - height) / 2f)
    val strokeWidth = width * 0.04f

    val wobble = strokeWidth * 1.2f * animationLevel

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
    val wobbleOffset = sin(System.currentTimeMillis() / 500f * animationLevel) * strokeWidth

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
