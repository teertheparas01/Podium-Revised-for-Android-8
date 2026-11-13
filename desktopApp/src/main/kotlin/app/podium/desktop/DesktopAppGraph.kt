package app.podium.desktop

import app.podium.core.common.Clock
import app.podium.core.common.DefaultClock
import app.podium.core.lyrics.FileLyricsCache
import app.podium.core.lyrics.LrclibProvider
import app.podium.core.lyrics.LyricsRepository
import app.podium.core.lyrics.LyricsAttribution
import app.podium.core.lyrics.UrlConnectionLyricsHttp
import app.podium.core.model.SourceId
import app.podium.player.api.AutoplayEngine
import app.podium.player.api.FavoritesRepository
import app.podium.player.api.PlaybackController
import app.podium.player.api.QueueManager
import app.podium.player.api.VolumeController
import app.podium.sources.api.Capability
import app.podium.sources.api.SourceCapabilities
import app.podium.sources.api.SourceDescriptor
import app.podium.sources.api.SourceRegistry
import app.podium.sources.api.resolve.StreamResolver
import app.podium.sources.api.matching.TrackMatcher
import app.podium.sources.api.matching.TrackNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

/**
 * Desktop application dependency graph - replaces Android's AppGraph.
 * Holds all singleton services for the desktop application.
 * 
 * ARCHITECTURE NOTE: Online sources (YouTube Music, etc.) are prepared as extension points.
 * To enable YouTube Music later:
 * 1. Add dependencies to build.gradle.kts (WebView2, GraalJS, WinRT, Media3 JVM)
 * 2. Implement DesktopYouTubeMusicSource wrapping YouTubeMusicSource logic
 * 3. Register via registerFutureSource("ytmusic") { ... }
 * 4. Enable in Settings UI
 */
class DesktopAppGraph(
    val coroutineScope: CoroutineScope,
    val clock: Clock = DefaultClock,
) {
    
    // =========================================================================
    // Core infrastructure
    // =========================================================================
    
    // Database (Room 3 on JVM) - initialized lazily
    val database by lazy { 
        throw UnsupportedOperationException("Room database not yet initialized for desktop")
    }
    
    // OkHttp client for all network requests
    val httpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    // =========================================================================
    // Lyrics
    // =========================================================================
    
    val lyricsCache by lazy {
        val cacheDir = File(System.getProperty("user.home"), ".local/share/podium/lyrics")
        cacheDir.mkdirs()
        app.podium.core.lyrics.FileLyricsCache(cacheDir)
    }
    
    val lyricsHttp by lazy {
        app.podium.core.lyrics.UrlConnectionLyricsHttp(
            userAgent = "Podium/0.1.0 (Desktop; +https://github.com/GHOSTxASR/podium-music-player)",
            executor = Executors.newCachedThreadPool { r -> Thread(r, "podium-lyrics-io").apply { isDaemon = true } }
        )
    }
    
    val lrclibProvider by lazy {
        app.podium.core.lyrics.LrclibProvider(lyricsHttp)
    }
    
    val lyricsRepository by lazy {
        app.podium.core.lyrics.LyricsRepository(
            provider = lrclibProvider,
            cache = lyricsCache,
            consent = { true }, // TODO: Implement consent UI
            clock = clock,
        )
    }
    
    // =========================================================================
    // Source Registry & Matching
    // =========================================================================
    
    val trackNormalizer by lazy { TrackNormalizer() }
    val trackMatcher by lazy { TrackMatcher(trackNormalizer) }
    
    val sourceRegistry by lazy {
        SourceRegistry()
    }
    
    val streamResolver by lazy {
        app.podium.sources.api.resolve.StreamResolver(
            registry = sourceRegistry,
            health = DesktopSourceHealthMonitor(clock),
            matcher = trackMatcher,
            clock = clock,
        )
    }
    
    // =========================================================================
    // Local Music Source (MVP)
    // =========================================================================
    
    val localMusicSource by lazy {
        DesktopLocalMusicSource(coroutineScope)
    }
    
    // Register local source at startup
    init {
        sourceRegistry.register(localMusicSource)
    }
    
    // =========================================================================
    // Future Online Sources Extension Point
    // =========================================================================
    /**
     * Registers a future online source (YouTube Music, etc.) when dependencies are added.
     * Call this from a feature module or settings when user enables the source.
     * 
     * @param sourceId Unique identifier (e.g., "ytmusic")
     * @param factory Lambda that creates the source when dependencies are available
     */
    fun registerFutureSource(sourceId: String, factory: () -> app.podium.sources.api.MusicSource) {
        futureSources[sourceId] = factory
    }
    
    /** Future online sources (YouTube Music, etc.) - registered but not instantiated until enabled */
    private val futureSources = mutableMapOf<String, () -> app.podium.sources.api.MusicSource>()
    
    /** Initializes and registers a future source */
    fun enableFutureSource(sourceId: String): app.podium.sources.api.MusicSource? {
        return futureSources.remove(sourceId)?.let { factory ->
            val source = factory()
            sourceRegistry.register(source)
            source
        }
    }
    
    /** Checks if a future source is available for enabling */
    fun isFutureSourceAvailable(sourceId: String) = futureSources.containsKey(sourceId)
    
    // Pre-register YouTube Music as a future source (not instantiated)
    init {
        registerFutureSource("ytmusic") {
            throw UnsupportedOperationException(
                "YouTube Music not yet implemented. " +
                "Add WebView2, GraalJS, WinRT, ExoPlayer JVM dependencies to build.gradle.kts, " +
                "then implement DesktopYouTubeMusicSource wrapping YouTubeMusicSource logic."
            )
        }
    }
    
    // =========================================================================
    // Playback
    // =========================================================================
    
    val queueManager by lazy { QueueManager() }
    
    val playbackController by lazy {
        DesktopPlaybackEngine(coroutineScope, sourceRegistry, streamResolver, queueManager)
    }
    
    val volumeController by lazy {
        DesktopVolumeController()
    }
    
    val autoplayEngine by lazy { AutoplayEngine() }
    
    val favoritesRepository by lazy {
        throw UnsupportedOperationException("FavoritesRepository not yet implemented for desktop")
    }
    
    // =========================================================================
    // Settings / Preferences
    // =========================================================================
    
    val settingsRepository by lazy {
        DesktopSettingsRepository()
    }
    
    val credentialsStore by lazy {
        DesktopCredentialStore()
    }
    
    // =========================================================================
    // Lifecycle
    // =========================================================================
    
    fun shutdown() {
        coroutineScope.cancel()
        httpClient.dispatcher().executorService.shutdown()
        httpClient.connectionPool().evictAll()
    }
}

companion object {
    @Volatile private var INSTANCE: DesktopAppGraph? = null
    
    fun initialize(coroutineScope: CoroutineScope): DesktopAppGraph {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: DesktopAppGraph(coroutineScope).also { INSTANCE = it }
        }
    }
    
    fun get(): DesktopAppGraph = INSTANCE ?: throw IllegalStateException("DesktopAppGraph not initialized")
}