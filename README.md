# Podium for Android 8

A personal port of [Podium](https://github.com/GHOSTxASR/podium-music-player) for an unused Android phone with a 3.5 mm headphone jack.

I loved Podium and had a phone sitting unused, so I ported it to Android 8 (API 26) and put the phone back to work as a music player.

## Download

[Download the latest APK](https://github.com/teertheparas01/Podium-Revised-for-Android-8/raw/refs/heads/main/artifacts/apk/Podium-0.1.5.1-android8-debug.apk)

This is a debug build for sideloading. On your phone, allow your browser or file manager to install unknown apps when Android prompts you.

## Build it

Requirements: JDK 21 and the Android SDK.

```powershell
.\gradlew.bat assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Credits and license

This is an unofficial community port and is not affiliated with the original Podium authors. Podium is licensed under the [GNU GPL v3](LICENSE). Third-party notices and licenses are in [`third_party/`](third_party/).
