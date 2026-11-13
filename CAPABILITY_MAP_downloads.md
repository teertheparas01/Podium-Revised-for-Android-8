# Capability map: Music downloads and app updates

## Assumptions

- “Music downloads” means saving online tracks inside Podium for verified offline playback.
- “App updates” means checking for and installing newer APK releases from `Podium-Revised-for-Android-8`.
- The app already has a GitHub Releases updater, but it currently targets `GHOSTxASR/podium-music-player`. The new repository currently has a committed APK on `main`, but no release asset for the updater to consume.
- The active YouTube Music source rejects `Purpose.DOWNLOAD`; custom sources are intentionally stream-only (D-91). No current music source permits downloads.

## Capabilities

| Module id | Responsibility | Depends on |
|---|---|---|
| `music-offline` | Offer provider-authorized online tracks for download, persist and verify audio files, show progress and failures, and play completed tracks offline. | Source download permission and resolver contract; durable download store; transfer and verification; playback resolver integration; download UI. |
| `app-updates` | Check stable releases from `Podium-Revised-for-Android-8`, find the APK asset, verify it, and hand it to Android’s installer. | A published GitHub Release with a version newer than the installed app and an APK signed with the same key. |

## Build order

1. Resolve the music download policy before implementing `music-offline`: keep downloads unavailable for YouTube Music and stream-only custom sources, or explicitly change that source boundary.
2. Implement `app-updates` independently by pointing the existing updater at the new repository and ensuring its release asset convention is documented.
3. Implement `music-offline` against the approved source-permission policy.

## Open decision

Should `music-offline` keep respecting each source’s current download permission (which means YouTube Music remains unavailable), or should the YouTube Music source be changed to allow saving its streams?
