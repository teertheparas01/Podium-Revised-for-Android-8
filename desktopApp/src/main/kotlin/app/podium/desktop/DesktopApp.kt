package app.podium.desktop

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Podium Desktop - Minimal JavaFX Application Entry Point
 */
class DesktopApp : Application() {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var primaryStage: Stage? = null
    
    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        
        primaryStage.title = "Podium Music Player"
        primaryStage.minWidth = 800.0
        primaryStage.minHeight = 600.0
        primaryStage.width = 1200.0
        primaryStage.height = 800.0
        
        // Simple main view
        val root = BorderPane()
        
        // Top bar
        val topBar = HBox(16.0)
        topBar.padding = javafx.geometry.Insets(12.0, 16.0, 12.0, 16.0)
        topBar.style = "-fx-background-color: #1a1a2e;"
        topBar.alignment = javafx.geometry.Pos.CENTER_LEFT
        
        val title = Label("Podium")
        title.style = "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;"
        
        val searchField = TextField()
        searchField.promptText = "Search..."
        searchField.prefWidth = 300.0
        searchField.style = "-fx-background-color: #2a2a4a; -fx-text-fill: white; -fx-prompt-text-fill: #888;"
        
        val spacer1 = Region()
        HBox.setHgrow(spacer1, Priority.ALWAYS)
        
        val settingsBtn = Button("⚙")
        settingsBtn.style = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px;"
        
        topBar.children.addAll(title, searchField, spacer1, settingsBtn)
        root.top = topBar
        
        // Center content
        val centerContent = VBox(12.0)
        centerContent.padding = javafx.geometry.Insets(16.0)
        centerContent.style = "-fx-background-color: #0f0f1a;"
        
        val header = Label("Your Library")
        header.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
        
        val content = Label("Podium Desktop MVP\n\nJavaFX is working!\n\nNext steps:\n- Connect DesktopLocalMusicSource\n- Add playback with JavaFX MediaPlayer\n- Implement PodWheel/Navigation")
        content.style = "-fx-text-fill: #888; -fx-font-size: 14px;"
        content.isWrapText = true
        
        centerContent.children.addAll(header, content)
        
        val scrollPane = ScrollPane()
        scrollPane.content = centerContent
        scrollPane.setFitToWidth(true)
        scrollPane.style = "-fx-background: transparent; -fx-background-color: transparent;"
        root.center = scrollPane
        
        // Bottom - Mini player
        val bottomPlayer = HBox(12.0)
        bottomPlayer.padding = javafx.geometry.Insets(8.0, 16.0, 8.0, 16.0)
        bottomPlayer.style = "-fx-background-color: #1a1a2e; -fx-border-color: #333; -fx-border-width: 1 0 0 0;"
        bottomPlayer.alignment = javafx.geometry.Pos.CENTER_LEFT
        
        val artwork = Rectangle(56.0, 56.0)
        artwork.fill = Color.web("#333")
        artwork.arcWidth = 8.0
        artwork.arcHeight = 8.0
        
        val trackInfo = VBox(2.0)
        val trackTitle = Label("Nothing playing")
        trackTitle.style = "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
        val trackArtist = Label("Select a track from your library")
        trackArtist.style = "-fx-text-fill: #888; -fx-font-size: 12px;"
        trackInfo.children.addAll(trackTitle, trackArtist)
        
        val progressBar = ProgressBar(0.0)
        progressBar.prefWidth = 200.0
        progressBar.style = "-fx-accent: #367FE0;"
        
        val controls = HBox(8.0)
        controls.alignment = javafx.geometry.Pos.CENTER
        
        val prevBtn = createControlButton("⏮")
        val playBtn = createControlButton("▶")
        val nextBtn = createControlButton("⏭")
        
        controls.children.addAll(prevBtn, playBtn, nextBtn)
        
        val volumeSlider = Slider(0.0, 100.0, 80.0)
        volumeSlider.prefWidth = 100.0
        
        val spacer2 = Region()
        HBox.setHgrow(spacer2, Priority.ALWAYS)
        
        bottomPlayer.children.addAll(artwork, trackInfo, controls, progressBar, volumeSlider, spacer2)
        root.bottom = bottomPlayer
        
        val scene = Scene(root)
        primaryStage.scene = scene
        primaryStage.title = "Podium Music Player"
        primaryStage.minWidth = 800.0
        primaryStage.minHeight = 600.0
        primaryStage.width = 1200.0
        primaryStage.height = 800.0
        primaryStage.show()
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
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(DesktopApp::class.java, *args)
        }
    }
}