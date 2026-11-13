# Podium Desktop - Remaining Work Tracker

## 🚨 BLOCKED (Flagged UNDONE)
- [ ] **JavaFX Runtime Module Path** - `--add-modules` works but modules not on module path
  - Tried: `runtimeClasspath` task (doesn't exist), `dependencies --configuration runtimeClasspath` (no paths), manual cache lookup (found JARs)
  - **Status**: FLAGGED UNDONE - Need proper module path resolution

## 📋 REMAINING MODULES (Priority Order)

### 1. DesktopLocalMusicSource ⏱️ 45 min
- [ ] WatchService-based file scanner for music folders
- [ ] JavaFX MediaPlayer metadata extraction
- [ ] Track/Album/Artist aggregation
- [ ] Folder selection persistence

### 2. Playback Engine ⏱️ 60 min
- [ ] JavaFX MediaPlayer wrapper
- [ ] Play/Pause/Next/Previous/Seek
- [ ] Volume control (Windows Core Audio via JNA)
- [ ] Queue management (reuse player:api QueueManager)
- [ ] Gapless playback preparation

### 3. UI Integration ⏱️ 45 min
- [ ] Library view (Artists/Albums/Songs/Playlists)
- [ ] Now Playing screen
- [ ] Mini player (always visible)
- [ ] Navigation (Navigation3)

### 4. Settings & Persistence ⏱️ 30 min
- [ ] JSON settings in %APPDATA%\Podium
- [ ] Windows DPAPI credential store
- [ ] Folder selection UI

### 5. YouTube Music Extension Point ⏱️ 30 min
- [ ] Register future source in SourceRegistry
- [ ] WebView2 for OAuth (deferred)
- [ ] GraalJS for signatures (deferred)

### 6. Packaging ⏱️ 30 min
- [ ] MSIX packaging
- [ ] Auto-update check
- [ ] Startup shortcut

---

## 📝 NOTES
- JavaFX Runtime: **FLAGGED UNDONE** - will use JAVA_OPTS workaround for now
- All pure Kotlin core modules ready: `core:model`, `core:common`, `core:lyrics`, `sources:api`, `player:api`
- Compose Desktop deferred - using JavaFX for MVP
- YouTube Music: Extension points ready in `DesktopAppGraph.registerFutureSource()`

---

## ⏰ COMMIT SCHEDULE
Every 15-20 minutes. Current: 2026-11-14 02:00