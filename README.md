# MotionWall

A simple Android live wallpaper app that plays any video from your phone as your home-screen wallpaper.

## Features

- **Pick a local video** from your phone using the system file picker. No storage permission is required.
- **Live preview** inside the app: the selected video plays in a preview window that mirrors what the wallpaper will look like.
- **Set it as the real Android live wallpaper** through the standard system chooser. The wallpaper keeps working after you leave the app and after locking/unlocking the phone.
- **Original quality**: the video file is played directly from its URI. It is never compressed, converted, or re-encoded.
- **Fill the screen without stretching**: the video keeps its aspect ratio and the extra edges are cropped. Choose how it fits:
  - **Auto** — fills the whole screen, crops the smallest overflow (default).
  - **Vertical** — crops the left/right edges.
  - **Horizontal** — crops the top/bottom edges.
  - **Crop** — center cover (same as Auto on most phones).
- **Sound ON/OFF** switch.
- **Seamless looping** with no obvious pause between repetitions.
- Android 9+ (API 28) and up. No internet access, minimal permissions.

## How it works

The app is a standard `WallpaperService` engine. It creates a `MediaPlayer` pointing at the video URI you picked and renders it onto the wallpaper surface. Playback loops via `MediaPlayer.setLooping(true)`, which restarts sample-accurately, so the loop is not noticeable. The fit modes use MediaPlayer's built-in cover scaling for symmetric crops, so nothing is ever stretched.

## Build

Open the project in Android Studio, sync Gradle, and run it on a device (Android 9+).

To build a release APK from the command line:

```bash
./gradlew :app:assembleRelease
```

The signed APK is published automatically by the `MotionWall APK` GitHub Actions workflow (see `.github/workflows/build-release.yml`).
