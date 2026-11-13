package app.podium.desktop

import app.podium.player.api.PlaybackController
import app.podium.player.api.VolumeController
import app.podium.player.api.PlaybackSnapshot
import app.podium.player.api.PlayIntent
import app.podium.player.api.PlaybackStatus
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.shape.Rectangle
import javafx.scene.paint.Color
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Priority
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.layout.Region
import javafx.scene.layout.Priority
import javafx.scene.shape.Rectangle
import javafx.scene.paint.Color
import javafx.geometry.Insets
import javafx.geometry.Pos

/**
 * Mini player view - always visible at bottom of window.
 */
class MiniPlayerView(
    private val playbackController: app.podium.player.api.PlaybackController,
    private val volumeController: app.podium.desktop.DesktopVolumeController,
) {
    
    fun createView(): Node {
        val root = HBox(12.0)
        root.padding = Insets(8.0, 16.0, 8.0, 16.0)
        root.style = "-fx-background-color: #1a1a2e; -fx-border-color: #333; -fx-border-width: 1 0 0 0;"
        root.alignment = Pos.CENTER_LEFT
        
        // Artwork placeholder
        val artwork = Rectangle(56.0, 56.0)
        artwork.fill = Color.web("#333")
        artwork.arcWidth = 8.0
        artwork.arcHeight = 8.0
        
        // Track info
        val trackInfo = VBox(2.0)
        val trackTitle = Label("Nothing playing")
        trackTitle.style = "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
        val trackArtist = Label("Select a track from your library")
        trackArtist.style = "-fx-text-fill: #888; -fx-font-size: 12px;"
        trackInfo.children.addAll(trackTitle, trackArtist)
        
        // Progress bar
        val progressBar = ProgressBar(0.0)
        progressBar.prefWidth = 200.0
        progressBar.style = "-fx-accent: #367FE0;"
        
        // Controls
        val controls = HBox(8.0)
        controls.alignment = Pos.CENTER
        
        val prevBtn = createControlButton("⏮")
        val playBtn = createControlButton("▶")
        val nextBtn = createControlButton("⏭")
        
        controls.children.addAll(prevBtn, playBtn, nextBtn)
        
        // Volume
        val volumeSlider = Slider(0.0, 100.0, 80.0)
        volumeSlider.prefWidth = 100.0
        
        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        
        val root = HBox(12.0)
        root.padding = Insets(8.0, 16.0, 8.0, 16.0)
        root.style = "-fx-background-color: #1a1a2e; -fx-border-color: #333; -fx-border-width: 1 0 0 0;"
        root.alignment = Pos.CENTER_LEFT
        root.children.addAll(artwork, trackInfo, controls, progressBar, volumeSlider, spacer)
        
        return root
    }
    
    private fun createControlButton(text: String): Button {
        val btn = Button(text)
        btn.prefWidth = 40.0
        btn.prefHeight = 40.0
        btn.style = """
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-border-radius: 20;
            -fx-background-radius: 20;
        """
        return btn
    }
}