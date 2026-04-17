# Glass Library

A library for creating glass morphism effects in Jetpack Compose with support for Android API 24+.

## Features

- ✨ Realistic glass morphism effects
- 📱 Support for Android API 24+ (Android 7.0 and above)
- 🔄 Automatic fallback for older Android versions
- 🎨 Customizable parameters: blur, distortion, shadow, transparency
- 🚀 AGSL shaders (Android 13+)

## Version Support

### Android 13+ (API 33+)
- Full support with AGSL shaders
- All effects work at hardware level
- Maximum performance

### Android 7.0 - 12 (API 24-32)
- Fallback implementation using standard Compose modifiers
- Glass effect simulation using gradients and transparency
- No blur support (blur only available with AGSL shaders)

## Usage

### Basic Example

```kotlin
GlassContainer(
    modifier = Modifier.fillMaxSize(),
    content = {
        // Your main content (background of glass)
        Image(...)
    }
) {
    GlassBox(
        modifier = Modifier
            .size(200.dp)
            .align(Alignment.Center),
        blur = 0.5f,
        scale = 0.3f,
        tint = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Glass Effect")
    }
}
```

### Advanced Example

```kotlin
GlassContainer(
    modifier = Modifier.fillMaxSize(),
    content = { BackgroundContent() }
) {
    GlassBox(
        modifier = Modifier
            .size(300.dp, 200.dp)
            .align(Alignment.Center),
        blur = 0.8f,              // Blur intensity (0.0 - 1.0)
        scale = 0.2f,             // Scale/magnification effect (0.0 - 1.0)
        centerDistortion = 0.3f,  // Center distortion (0.0 - 1.0)
        elevation = 8.dp,         // Shadow elevation
        tint = Color.Blue.copy(alpha = 0.1f), // Glass tint
        darkness = 0.2f,          // Darkness from edges (0.0 - 1.0)
        warpEdges = 0.4f,         // Edge warping (0.0 - 1.0)
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Text("Advanced Glass Effect")
        }
    }
}
```

## GlassBox Parameters

| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `blur` | Float | 0.0-1.0 | Blur intensity |
| `scale` | Float | 0.0-1.0 | Magnification/lens effect |
| `centerDistortion` | Float | 0.0-1.0 | Distortion in element center |
| `elevation` | Dp | - | Shadow elevation |
| `tint` | Color | - | Glass color tint |
| `darkness` | Float | 0.0-1.0 | Darkening from edges to center |
| `warpEdges` | Float | 0.0-1.0 | Edge warping distortion |
| `shape` | CornerBasedShape | - | Glass element shape |

## Fallback Implementation Features

On devices with Android < 13, a simplified implementation is used:

- Gradient backgrounds to simulate glass effects
- No blur support (blur only works with AGSL shaders on Android 13+)
- Scale simulation through `graphicsLayer`
- Transparency effects to simulate distortions

## Dependencies

```kotlin
dependencies {
    implementation(project(":glass"))
    // or if published
    // implementation("com.mrtdk:glass:1.0.0")
}
```

## Minimum Requirements

- Android API 24+ (Android 7.0)
- Jetpack Compose BOM 2024.09.00+
- Kotlin 2.0.21+

## License

```
Copyright 2024 MRTDK

Licensed under the Apache License, Version 2.0
``` 