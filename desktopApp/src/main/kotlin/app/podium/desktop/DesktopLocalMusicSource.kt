package app.podium.desktop

import app.podium.core.common.Outcome
import app.podium.core.common.PodiumError
import app.podium.core.model.AlbumId
import app.podium.core.model.ArtistId
import app.podium.core.model.ArtworkRef
import app.podium.core.model.MediaKind
import app.podium.core.model.PlaybackRoute
import app.podium.core.model.SourceRef
import app.podium.core.model.Track
import app.podium.core.model.TrackId
import app.podium.sources.api.AlbumDetail
import app.podium.sources.api.AlbumSummary
import app.podium.sources.api.ArtistDetail
import app.podium.sources.api.ArtistSummary
import app.podium.sources.api.ArtworkFacet
import app.podium.sources.api.ArtworkPayload
import app.podium.sources.api.Basis
import app.podium.sources.api.Capability
import app.podium.sources.api.CapabilityAction
import app.podium.sources.api.CapabilityState
import app.podium.sources.api.CapabilityStatus
import app.podium.sources.api.CatalogFacet
import app.podium.sources.api.FolderFacet
import app.podium.sources.api.FolderSelection
import app.podium.sources.api.LibraryFacet
import app.podium.sources.api.MissReason
import app.podium.sources.api.MusicFolder
import app.podium.sources.api.MusicSource
import app.podium.sources.api.PlayableMedia
import app.podium.sources.api.PlaybackFacet
import app.podium.sources.api.PlaybackTarget
import app.podium.sources.api.Purpose
import app.podium.sources.api.QualityRequest
import app.podium.sources.api.SearchQuery
import app.podium.sources.api.SearchResults
import app.podium.sources.api.SourceCapabilities
import app.podium.sources.api.SourceDescriptor
import app.podium.sources.api.matching.TrackNormalizer
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Desktop local music source using WatchService for file system monitoring
 * and JavaFX MediaPlayer for metadata extraction.
 * Replaces Android's MediaStore-based LocalMusicSource.
 */
class DesktopLocalMusicSource(
    private val scope: CoroutineScope,
    private val musicFolders: List<File> = defaultMusicFolders(),
    private val trackNormalizer: TrackNormalizer = TrackNormalizer(),
) : MusicSource {
    
    companion object {
        private const val SOURCE_ID = "local"
        
        fun defaultMusicFolders(): List<File> {
            val folders = mutableListOf<File>()
            val userHome = File(System.getProperty("user.home"))
            
            // Common music folders on Windows
            listOf("Music", "OneDrive/Music").forEach { name ->
                val f = File(userHome, name)
                if (f.exists() && f.isDirectory) folders.add(f)
            }
            
            // Also check Public Music
            val publicMusic = File(System.getenv("PUBLIC") ?: "C:/Users/Public", "Music")
            if (publicMusic.exists() && publicMusic.isDirectory) folders.add(publicMusic)
            
            return if (folders.isEmpty()) listOf(userHome) else folders
        }
    }
    
    override val descriptor = SourceDescriptor(
        id = SourceId(SOURCE_ID),
        displayName = "On this device",
        providerName = "Local file system",
        basis = Basis.LOCAL_DEVICE,
        cheapResolve = true,
    )
    
    private val _capabilities = kotlinx.coroutines.flow.MutableStateFlow(computeCapabilities())
    override val capabilities: kotlinx.coroutines.flow.StateFlow<app.podium.sources.api.SourceCapabilities> = _capabilities.asStateFlow()
    
    private val _tracks = kotlinx.coroutines.flow.MutableStateFlow<List<Track>>(emptyList())
    val tracksState: kotlinx.coroutines.flow.StateFlow<List<Track>> = _tracks.asStateFlow()
    
    private val scanJob = scope.launch(Dispatchers.IO) { scan() }
    private val watchJob = scope.launch(Dispatchers.IO) { watch() }
    
    private val artworkCache = ConcurrentHashMap<String, ArtworkPayload>()
    
    init {
        // Initial scan
    }
    
    private fun computeCapabilities(): app.podium.sources.api.SourceCapabilities {
        return app.podium.sources.api.SourceCapabilities.available(
            Capability.SEARCH,
            Capability.BROWSE,
            Capability.LIBRARY,
            Capability.DIRECT_STREAM,
            Capability.ARTWORK,
        )
    }
    
    /** Scan all music folders for audio files. */
    private suspend fun scan(): List<Track> {
        val tracks = mutableListOf<Track>()
        
        for (folder in musicFolders) {
            if (!folder.exists()) continue
            
            Files.walk(folder.toPath())
                .filter { path -> isAudioFile(path) }
                .forEach { path ->
                    extractMetadata(path)?.let { tracks.add(it) }
                }
        }
        
        val result = tracks.sortedBy { it.title.lowercase() }
        _tracks.value = result
        return result
    }
    
    /** Watch for file system changes. */
    private suspend fun watch() {
        val watchService = FileSystems.getDefault().newWatchService()
        
        for (folder in musicFolders) {
            if (folder.exists()) {
                folder.toPath().register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
            }
        }
        
        while (true) {
            val key = watchService.take()
            for (event in key.pollEvents()) {
                val path = (key.watchable() as java.nio.file.Path).resolve(event.context() as java.nio.file.Path)
                
                when (event.kind()) {
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY -> {
                        if (isAudioFile(path)) {
                            extractMetadata(path)?.let { track ->
                                _tracks.update { current ->
                                    (current - track) + track
                                }
                            }
                        }
                    }
                    StandardWatchEventKinds.ENTRY_DELETE -> {
                        _tracks.update { current ->
                            current.filter { it.source.providerKey != path.toString() }
                        }
                        artworkCache.remove(path.toString())
                    }
                }
            }
            key.reset()
        }
    }
    
    private fun isAudioFile(path: Path): Boolean {
        val name = path.fileName.toString().lowercase()
        return name.endsWith(".mp3") || name.endsWith(".flac") || 
               name.endsWith(".ogg") || name.endsWith(".m4a") || 
               name.endsWith(".wav") || name.endsWith(".opus") ||
               name.endsWith(".alac") || name.endsWith(".aac")
    }
    
    private fun extractMetadata(path: Path): Track? {
        return try {
            // Use JavaFX MediaPlayer to extract metadata
            val media = Media(path.toUri().toString())
            val mediaPlayer = MediaPlayer(media)
            
            // Wait for metadata to be ready
            val metadataReady = java.util.concurrent.CountDownLatch(1)
            var metadata: javafx.collections.Map<String, Any>? = null
            
            mediaPlayer.setOnReady {
                metadata = mediaPlayer.getMetadata()
                metadataReady.countDown()
            }
            
            mediaPlayer.setOnError {
                metadataReady.countDown()
            }
            
            // Wait for metadata with timeout
            if (!metadataReady.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                mediaPlayer.dispose()
                return null
            }
            
            mediaPlayer.dispose()
            
            val meta = metadata ?: return null
            
            val title = (meta["title"] as? String) ?: path.fileName.toString().removeSuffix(".mp3").removeSuffix(".flac")
            val artist = (meta["artist"] as? String) ?: "Unknown Artist"
            val album = (meta["album"] as? String) ?: "Unknown Album"
            val durationMs = (meta["duration"] as? javafx.util.Duration)?.toMillis()?.toLong() ?: 0L
            
            val trackId = TrackId.of(SourceId(SOURCE_ID), path.toString())
            val sourceRef = SourceRef(SourceId(SOURCE_ID), path.toString())
            
            val track = Track(
                id = trackId,
                source = sourceRef,
                title = title,
                artists = listOf(app.podium.core.model.ArtistCredit(artist)),
                album = app.podium.core.model.AlbumRef(album, app.podium.core.model.AlbumId.of(SourceId(SOURCE_ID), album)),
                durationMs = durationMs,
                kind = MediaKind.SONG,
            )
            
            // Extract artwork if available
            (meta["image"] as? javafx.scene.image.Image)?.let { image ->
                // TODO: Store artwork and create ArtworkRef
            }
            
            track
        } catch (e: Exception) {
            null
        }
    }
    
    // =========================================================================
    // Facets
    // =========================================================================
    
    override val library: LibraryFacet = object : LibraryFacet {
        override fun tracks() = _tracks
        
        override fun albums() = _tracks.map { tracks ->
            tracks.filter { it.album?.id != null }
                .groupBy { it.album!!.id!! }
                .map { (id, ts) ->
                    val first = ts.first()
                    AlbumSummary(
                        id = id,
                        title = first.album!!.title,
                        artistDisplay = first.album?.albumArtist ?: first.artistDisplay,
                        artwork = first.artwork,
                        year = first.releaseDate?.year,
                        trackCount = ts.size,
                    )
                }
                .sortedBy { it.title.lowercase() }
        }
        
        override fun artists() = _tracks.map { tracks ->
            tracks.groupBy { it.artists.first().id ?: app.podium.core.model.ArtistId.of(app.podium.core.model.SourceId(SOURCE_ID), it.artistDisplay) }
                .map { (id, ts) ->
                    ArtistSummary(id, ts.first().artistDisplay, artwork = null, trackCount = ts.size)
                }
                .sortedBy { it.name.lowercase() }
        }
    }
    
    override val catalog: CatalogFacet = object : CatalogFacet {
        override suspend fun search(query: SearchQuery): Outcome<SearchResults> {
            val words = trackNormalizer.identityKey(query.text).split(' ').filter { it.isNotEmpty() }
            if (words.isEmpty()) return Outcome.Success(SearchResults.Empty)
            
            val hits = _tracks.value.filter { t ->
                val hay = trackNormalizer.identityKey("${t.title} ${t.artistDisplay} ${t.album?.title.orEmpty()}").split(' ')
                words.all { w -> hay.any { it.startsWith(w) } }
            }
            return Outcome.Success(SearchResults(tracks = hits.take(query.limit)))
        }
        
        override suspend fun track(ref: app.podium.sources.api.SourceRef): Outcome<Track> =
            _tracks.value.firstOrNull { it.source.providerKey == ref.providerKey }
                ?.let { Outcome.Success(it) }
                ?: Outcome.Failure(PodiumError.NotFound(ref.providerKey))
        
        override suspend fun album(id: AlbumId): Outcome<AlbumDetail> {
            val tracks = _tracks.value.filter { it.album?.id == id }
            val summary = tracks.firstOrNull()?.let { it.album }?.let { album ->
                AlbumSummary(
                    id = id,
                    title = album.title,
                    artistDisplay = album.albumArtist ?: tracks.first().artistDisplay,
                    artwork = tracks.first().artwork,
                    year = tracks.first().releaseDate?.year,
                    trackCount = tracks.size,
                )
            } ?: return Outcome.Failure(PodiumError.NotFound("album"))
            
            return Outcome.Success(AlbumDetail(summary, tracks.sortedBy { it.trackNumber ?: 0 }))
        }
        
        override suspend fun artist(id: ArtistId): Outcome<ArtistDetail> {
            val tracks = _tracks.value.filter { it.artists.first().id == id }
            val summary = tracks.firstOrNull()?.let { 
                ArtistSummary(id, it.artistDisplay, artwork = null, trackCount = tracks.size)
            } ?: return Outcome.Failure(PodiumError.NotFound("artist"))
            
            return Outcome.Success(ArtistDetail(summary, emptyList(), tracks))
        }
    }
    
    override val playback: PlaybackFacet = object : PlaybackFacet {
        override val routes = setOf(PlaybackRoute.DIRECT)
        
        override suspend fun resolve(track: Track, quality: QualityRequest, purpose: Purpose): app.podium.sources.api.FacetResolution {
            if (track.source.sourceId != descriptor.id) {
                return app.podium.sources.api.FacetResolution.Miss(app.podium.sources.api.MissReason.NO_SOURCE)
            }
            
            val uri = track.source.providerKey
            val file = File(uri)
            if (!file.exists()) {
                return app.podium.sources.api.FacetResolution.Miss(app.podium.sources.api.MissReason.NOT_FOUND)
            }
            
            return app.podium.sources.api.FacetResolution.Resolved(
                app.podium.sources.api.PlaybackTarget.DirectStream(
                    track.id,
                    app.podium.sources.api.PlayableMedia(
                        uri = file.toUri().toString(),
                        mimeType = track.advertisedQualities.firstOrNull()?.container,
                        claimedQuality = track.advertisedQualities.firstOrNull(),
                        durationMs = track.durationMs,
                        sourceId = descriptor.id,
                        cacheKey = "${track.id.value}|original",
                    )
                )
            )
        }
    }
    
    override val artwork: ArtworkFacet = object : ArtworkFacet {
        override suspend fun load(ref: ArtworkRef, sizePx: Int): ArtworkPayload? {
            return artworkCache[ref.key]
        }
    }
    
    override val folders: FolderFacet = object : FolderFacet {
        override fun folders() = kotlinx.coroutines.flow.flowOf(
            musicFolders.map { app.podium.sources.api.MusicFolder(it.absolutePath, 0) }
        )
        
        override val selection = kotlinx.coroutines.flow.MutableStateFlow(app.podium.sources.api.FolderSelection.Default).asStateFlow()
        
        override fun select(selection: app.podium.sources.api.FolderSelection) {
            // TODO: Persist folder selection
        }
    }
}