# Sketchy Battery Live Wallpaper

An Android live wallpaper that renders a hand-drawn, sketch-style battery gauge. The fill animates with subtle wobble and reacts to the device battery level in real time:

- **Green** when the battery is above 20%.
- **Amber** between 10% and 20%.
- **Red** below 10%.

The app ships with a simple Compose-powered preview screen that shows the gauge and links directly to the live wallpaper picker.

## Modules
- `app`: Android application module containing the wallpaper service, preview UI, and resources.

## Getting started
1. Open the project in Android Studio (Giraffe or newer recommended).
2. Build and install the app on a device or emulator running Android 7.0 (API 24) or later.
3. From the app, tap **Set as live wallpaper** to open the chooser and apply the sketchy battery wallpaper.
