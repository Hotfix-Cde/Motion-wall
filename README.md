# MotionWall

> **Simple video wallpapers for Android.**
>
> Pick a video from your phone, preview it, and make it your home-screen live wallpaper.

[![Android 9+](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/pie)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Build APK](https://github.com/Hotfix-Cde/Motion-wall/actions/workflows/build-release.yml/badge.svg)](https://github.com/Hotfix-Cde/Motion-wall/actions/workflows/build-release.yml)

MotionWall is a small, offline-first Android live wallpaper app focused on one thing: **turning your own videos into wallpapers without unnecessary complexity.**

## ✨ Features

- 🎥 **Pick any local video** using Android's system file picker.
- 🔗 **Use a public direct video URL** from a website or video CDN, with no app-side download or conversion.
- 👀 **Preview before applying** so you know exactly what you're setting.
- 🏠 **Real Android live wallpaper** using the system wallpaper service.
- 🔊 **Sound ON/OFF** with a simple toggle.
- 📐 **Auto / Vertical / Horizontal** framing options.
- ✂️ **Aspect-ratio preserving crop** instead of stretching or squashing the video.
- 💎 **Original video playback** without app-side re-encoding or compression.
- 🔁 **Continuous looping** for an uninterrupted wallpaper.
- 🔒 **Works offline for local videos**; an internet connection is used only when you deliberately choose a public video URL.
- 🔐 **Minimal permissions**: Android's modern document picker plus the Internet permission required only for public video URLs.
- 📱 **Android 9 (API 28) and newer**.

## 🎯 Design philosophy

MotionWall intentionally stays small and straightforward.

There are no accounts, feeds, cloud uploads, wallpaper stores, or piles of settings. Your video stays on your device, and the app simply handles the job of getting it from local storage to your Android wallpaper surface.

## 🚀 How to use

1. Open **MotionWall**.
2. Tap **Choose video from phone** or **Use video URL**.
3. Select a local video or paste a public direct video URL.
4. Check the preview.
5. Choose your preferred framing setting; use the gear button for sound and app appearance.
6. Tap **Set as live wallpaper**.
7. Confirm the wallpaper in Android's system UI.

That's it.

## 🛠️ Tech

- **Kotlin**
- **Android SDK / WallpaperService**
- **MediaPlayer** for wallpaper playback from local or public HTTP(S) sources
- **Media3 ExoPlayer** for the in-app preview
- **Android Storage Access Framework** for selecting local videos
- **Gradle + Kotlin DSL**
- **GitHub Actions** for automated release APK builds

## 📦 Build locally

Open the project in Android Studio and let Gradle sync.

Build a release APK with:

```bash
./gradlew :app:assembleRelease
```

The generated APK will be under:

```text
app/build/outputs/apk/release/
```

## 🤖 Automated APK builds

This repository includes a GitHub Actions workflow at `.github/workflows/build-release.yml`.

The workflow can build the release APK, sign it using repository secrets, upload the APK as an artifact, and attach the APK to a GitHub Release.

## 📁 Project structure

```text
Motion-wall/
├── app/                    # Android application
├── .github/workflows/      # Automated APK build/release workflow
├── build.gradle.kts        # Root Gradle configuration
├── settings.gradle.kts    # Project settings
└── README.md               # Project documentation
```

## 🔒 Privacy

MotionWall does not upload your selected videos to a server. Local videos stay on your device. If you choose the optional URL feature, the app streams only the public URL you paste; it does not download, re-encode, or send that URL to a MotionWall server.

The app uses Android's system document picker rather than requesting broad storage access.

## 🧪 Status

MotionWall is an independent Android project under active development. Features and implementation details may change as the project evolves.

## 🤝 Contributing

Small improvements, bug fixes, documentation updates, and sensible feature ideas are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the basics.

## 📄 License

This project is released under the MIT License. See [`LICENSE`](LICENSE).

---

Made with Kotlin and a healthy dislike of unnecessary wallpaper-app bloat. 🌌