package app.podium.desktop

import app.podium.core.common.Outcome
import app.podium.core.common.PodiumError
import app.podium.core.model.QueueUid
import app.podium.core.model.Track
import app.podium.core.model.TrackId
import app.podium.player.api.PlaybackController
import app.podium.player.api.PlayIntent
import app.podium.player.api.PlaybackSnapshot
import app.podium.player.api.PlaybackStatus
import app.podium.player.api.QueueManager
import app.podium.player.api.QueueView
import app.podium.player.api.RepeatMode
import app.podium.sources.api.FacetResolution
import app.podium.sources.api.PlayableMedia
import app.podium.sources.api.PlaybackTarget
import app.podium.sources.api.QualityRequest
import app.podium.sources.api.SourceRegistry
import app.podium.sources.api.resolve.StreamResolver
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Desktop playback engine using JavaFX MediaPlayer.
 * Implements the PlaybackController interface using JavaFX MediaPlayer.
 */
class DesktopPlaybackEngine(
    private val scope: CoroutineScope,
    private val registry: SourceRegistry,
    private val resolver: app.podium.sources.api.resolve.StreamResolver,
    private val queueManager: QueueManager,
) : PlaybackController {
    
    private val _snapshot = MutableStateFlow<PlaybackSnapshot>(PlaybackSnapshot.Idle)
    override val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()
    
    private val _queue = MutableStateFlow<QueueView>(QueueView.Empty)
    override val queue: StateFlow<QueueView> = _queue.asStateFlow()
    
    private val mediaPlayerRef = java.util.concurrent.atomic.AtomicReference<MediaPlayer?>(null)
    private val currentItemRef = java.util.concurrent.atomic.AtomicReference<QueueItem?>(null)
    private val positionUpdater = scope.launch(Dispatchers.Main) { updatePositionLoop() }
    
    private data class QueueItem(
        val uid: QueueUid,
        val trackId: TrackId,
        val media: PlayableMedia,
    )
    
    private val playbackState = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    
    init {
        // Subscribe to queue changes
        scope.launch {
            queueManager.state.collect { q ->
                _queue.value = QueueView(
                    entries = q.items.map { it to it.uid to it.track.id to it.selection!!.media },
                    currentIndex = q.currentIndex,
                    shuffleEnabled = q.shuffleEnabled,
                    repeatMode = q.repeatMode,
                    autoplayEnabled = q.autoplayEnabled,
                )
            }
        }
        
        // TODO: Subscribe to queue manager's current item changes
    }
    
    private fun playMedia(media: PlayableMedia) {
        // Stop current player
        mediaPlayerRef.getAndSet(null)?.run {
            stop()
            dispose()
        }
        
        val mediaObj = Media(media.uri)
        val player = MediaPlayer(mediaObj)
        
        player.setOnReady {
            // Auto-play when ready
            play()
        }
        
        player.setOnError {
            val error = player.getError()
            _snapshot.value = PlaybackSnapshot.Error(
                status = PlaybackStatus.Error(PodiumError.Unexpected(error.getMessage())),
                item = currentItemRef.get()?.let { it.toNowPlayingItem() },
                intent = PlayIntent.PLAY,
                queue = _queue.value,
                error = PodiumError.Unexpected(error.getMessage()),
                recovery = app.podium.player.api.Recovery.SKIPPING,
            )
        }
        
        player.setOnEndOfMedia {
            next()
        }
        
        mediaPlayerRef.set(player)
    }
    
    private suspend fun updatePositionLoop() {
        while (true) {
            kotlinx.coroutines.delay(500)
            val player = mediaPlayerRef.get()
            if (player != null) {
                val position = player.getCurrentTime().toMillis().toLong()
                val duration = player.getTotalDuration().toMillis().toLong()
                
                val currentItem = currentItemRef.get()
                _snapshot.value = _snapshot.value.copy(
                    positionMs = position,
                    item = currentItem?.copy(durationMs = duration),
                )
            }
        }
    }
    
    override fun positionMs(): Long {
        return mediaPlayerRef.get()?.getCurrentTime()?.toMillis()?.toLong() ?: 0L
    }
    
    override fun play() {
        mediaPlayerRef.get()?.play()
        _snapshot.value = _snapshot.value.copy(
            status = PlaybackStatus.Playing,
            intent = PlayIntent.PLAY,
        )
    }
    
    override fun pause() {
        mediaPlayerRef.get()?.pause()
        _snapshot.value = _snapshot.value.copy(
            status = PlaybackStatus.Paused(app.podium.player.api.PauseReason.USER),
            intent = PlayIntent.PAUSE,
        )
    }
    
    override fun togglePlayPause() {
        val player = mediaPlayerRef.get()
        if (player != null) {
            val status = player.getStatus()
            if (status == MediaPlayer.Status.PLAYING) {
                pause()
            } else {
                play()
            }
        }
    }
    
    override fun next() {
        queueManager.next()
    }
    
    override fun previous() {
        queueManager.previous()
    }
    
    override fun seekTo(positionMs: Long) {
        mediaPlayerRef.get()?.seek(javafx.util.Duration.millis(positionMs.toDouble()))
    }
    
    override fun setRepeat(mode: RepeatMode) {
        queueManager.setRepeat(mode)
    }
    
    override fun setShuffle(enabled: Boolean) {
        queueManager.setShuffle(enabled)
    }
    
    override fun playContext(tracks: List<TrackId>, startIndex: Int, contextLabel: String?, shuffle: Boolean = false, radio: Boolean = false) {
        // Resolve tracks to PlayableMedia
        scope.launch {
            val mediaItems = tracks.mapNotNull { trackId ->
                // Get track from registry
                // TODO: Implement track lookup
                null
            }.filterNotNull()
            
            if (mediaItems.isNotEmpty()) {
                queueManager.setContext(
                    tracks = mediaItems.map { it.toTrack() },
                    startIndex = startIndex,
                    context = app.podium.player.api.PlayContext.fromLabel(contextLabel),
                    shuffle = shuffle,
                    fallbackPolicy = app.podium.sources.api.resolve.FallbackPolicy.EXACT_ONLY,
                )
            }
        }
    }
    
    override fun playNext(tracks: List<TrackId>) {
        queueManager.playNext(tracks.map { /* TODO: resolve to Track */ })
    }
    
    override fun addToQueue(tracks: List<TrackId>) {
        queueManager.addToQueue(tracks.map { /* TODO: resolve to Track */ })
    }
    
    override fun move(uid: QueueUid, toIndex: Int) {
        queueManager.move(uid, toIndex)
    }
    
    override fun remove(uids: Set<QueueUid>) {
        queueManager.remove(uids)
    }
    
    override fun skipTo(uid: QueueUid) {
        queueManager.skipTo(uid)
    }
    
    override fun clearUpcoming() {
        queueManager.clearUpcoming()
    }
    
    override fun openRemoteApp() {
        // Not applicable for desktop
    }
    
    override fun resumeLocal() {
        // Already local
    }
    
    fun shutdown() {
        positionUpdater.cancel()
        mediaPlayerRef.getAndSet(null)?.run {
            stop()
            dispose()
        }
    }
}

private fun app.podium.desktop.DesktopPlaybackEngine.QueueItem.toNowPlayingItem(): app.podium.player.api.NowPlayingItem {
    return app.podium.player.api.NowPlayingItem(
        uid = uid,
        trackId = trackId,
        title = "", // TODO: fill from track
        artistDisplay = "",
        albumTitle = null,
        artworkUri = null,
        durationMs = 0,
        indexInQueue = 0,
        queueSize = 0,
        kind = app.podium.core.model.MediaKind.SONG,
    )
}

private fun PlayableMedia.toTrack(): Track {
    // TODO: implement
    return Track(
        id = TrackId(SourceId("local"), uri),
        source = SourceRef(SourceId("local"), uri),
        title = "",
        artists = emptyList(),
    )
}