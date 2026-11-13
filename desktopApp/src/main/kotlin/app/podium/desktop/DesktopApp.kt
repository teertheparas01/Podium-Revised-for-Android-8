package app.podium.desktop

import app.podium.core.common.Clock
import app.podium.core.common.DefaultClock
import app.podium.core.lyrics.LyricsRepository
import app.podium.core.model.SourceId
import app.podium.player.api.PlaybackController
import app.podium.player.api.PlayIntent
import app.podium.player.api.PlaybackSnapshot
import app.podium.player.api.PlaybackStatus
import app.podium.player.api.QueueManager
import app.podium.player.api.QueueView
import app.podium.sources.api.SourceDescriptor
import app.podium.sources.api.MusicSource
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
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
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Priority
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.layout.Region
import javafx.scene.layout.Priority
import javafx.scene.shape.Rectangle
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

/**
 * Podium Desktop - JavaFX Application Entry Point
 */
class DesktopApp : Application() {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var appGraph: DesktopAppGraph
    private var primaryStage: Stage? = null
    
    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        
        // Initialize app graph
        appGraph = DesktopAppGraph.initialize(coroutineScope)
        
        primaryStage.title = "Podium Music Player"
        primaryStage.minWidth = 800.0
        primaryStage.minHeight = 600.0
        primaryStage.width = 1200.0
        primaryStage.height = 800.0
        
        val root = BorderPane()
        
        // Top bar
        val topBar = createTopBar()
        root.top = topBar
        
        // Center content - Library view
        val centerContent = createLibraryView()
        root.center = centerContent
        
        // Bottom - Mini player
        val bottomPlayer = createMiniPlayer()
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
    
    private fun createTopBar(): HBox {
        val topBar = HBox(16.0)
        topBar.padding = Insets(12.0, 16.0, 12.0, 16.0)
        topBar.style = "-fx-background-color: #1a1a2e;"
        topBar.alignment = Pos.CENTER_LEFT
        
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
        settingsBtn.setOnAction { showSettings() }
        
        topBar.children.addAll(title, searchField, spacer1, settingsBtn)
        return topBar
    }
    
    private fun createLibraryView(): ScrollPane {
        val libraryView = LibraryView(appGraph)
        val scrollPane = ScrollPane()
        scrollPane.content = libraryView.createView()
        scrollPane.fitToWidth = true
        scrollPane.style = "-fx-background: transparent; -fx-background-color: transparent;"
        return scrollPane
    }
    
    private fun createMiniPlayer(): HBox {
        val miniPlayer = MiniPlayerView(appGraph.playbackController, appGraph.volumeController)
        return miniPlayer.createView()
    }
    
    private fun showSettings() {
        // TODO: Implement settings dialog
    }
    
    override fun stop() {
        appGraph.shutdown()
        Platform.exit()
    }
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(DesktopApp::class.java, *args)
        }
    }
}