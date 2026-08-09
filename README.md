# VideoWall

A small Android live wallpaper app that:

- picks a local video from the phone
- keeps the original video file untouched
- lets the user switch sound on/off quickly
- supports Fit and Crop wallpaper scaling

## Android Studio

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run it on an Android device.
4. Choose a video.
5. Tap **Set as live wallpaper**.

The app uses Android's live wallpaper service API and the system file picker, so it does not need broad storage or internet permissions. The selected video is played directly from its URI and is not re-encoded.
