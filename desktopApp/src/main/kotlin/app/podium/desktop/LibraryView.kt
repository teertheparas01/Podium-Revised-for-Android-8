package app.podium.desktop

import app.podium.core.common.DefaultClock
import app.podium.core.common.Clock
import app.podium.core.model.SourceId
import app.podium.player.api.PlaybackController
import app.podium.sources.api.SourceDescriptor
import app.podium.sources.api.MusicSource
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.shape.Rectangle
import javafx.scene.paint.Color
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Priority
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.layout.Region
import javafx.scene.layout.Priority
import javafx.scene.shape.Rectangle
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.geometry.Insets
import javafx.geometry.Pos

/**
 * Library view showing music library with navigation sidebar and content area.
 */
class LibraryView(
    private val appGraph: DesktopAppGraph,
) {
    
    private val clock: Clock = DefaultClock
    
    fun createView(): Node {
        val root = BorderPane()
        
        // Left sidebar - Navigation
        val sidebar = createSidebar()
        root.left = sidebar
        
        // Center - Content area
        val centerContent = createContentArea()
        val centerPane = BorderPane()
        centerPane.center = centerContent
        root.center = centerPane
        
        return root
    }
    
    private fun createSidebar(): VBox {
        val sidebar = VBox(8.0)
        sidebar.prefWidth = 240.0
        sidebar.padding = Insets(16.0)
        sidebar.style = "-fx-background-color: #1a1a2e; -fx-border-color: #333; -fx-border-width: 0 1 0 0;"
        
        // Podium logo
        val logo = Label("Podium")
        logo.style = "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;"
        logo.padding = Insets(0.0, 0.0, 16.0, 0.0)
        
        // Navigation items
        val navItems = listOf(
            NavItem("🎵", "Music", { showMusic() }),
            NavItem("📀", "Albums", { showAlbums() }),
            NavItem("👤", "Artists", { showArtists() }),
            NavItem("📝", "Playlists", { showPlaylists() }),
            NavItem("❤️", "Liked Songs", { showLikedSongs() }),
            NavItem("⬇️", "Downloads", { showDownloads() }),
            NavItem("🕐", "Recently Played", { showRecentlyPlayed() }),
        )
        
        val navBox = VBox(4.0)
        navItems.forEach { item ->
            val btn = Button(item.label)
            btn.style = """
                -fx-background-color: transparent;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-alignment: CENTER_LEFT;
                -fx-padding: 12 16;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
            """
            btn.setOnAction { item.action() }
            btn.setOnMouseEntered { btn.style = btn.style + "-fx-background-color: #2a2a4a;" }
            btn.setOnMouseExited { btn.style = btn.style.replace("-fx-background-color: #2a2a4a;", "") }
            btn.maxWidth = Double.MAX_VALUE
            navBox.children.add(btn)
        }
        
        sidebar.children.addAll(logo, navBox)
        return sidebar
    }
    
    private fun createContentArea(): Node {
        val root = VBox(16.0)
        root.padding = Insets(16.0)
        root.style = "-fx-background-color: #0f0f1a;"
        
        val header = Label("Your Library")
        header.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
        
        // Placeholder - will be replaced with actual library content
        val content = Label("Loading your library...")
        content.style = "-fx-text-fill: #888; -fx-font-size: 14px;"
        content.wrapText = true
        
        val centerBox = VBox(12.0)
        centerBox.children.addAll(header, content)
        
        val scrollPane = ScrollPane()
        scrollPane.content = centerBox
        scrollPane.setFitToWidth(true)
        scrollPane.style = "-fx-background: transparent; -fx-background-color: transparent;"
        
        return scrollPane
    }
    
    private fun showMusic() {
        // TODO: Show music library
    }
    
    private fun showAlbums() {
        // TODO: Show albums
    }
    
    private fun showArtists() {
        // TODO: Show artists
    }
    
    private fun showPlaylists() {
        // TODO: Show playlists
    }
    
    private fun showLikedSongs() {
        // TODO: Show liked songs
    }
    
    private fun showDownloads() {
        // TODO: Show downloads
    }
    
    private fun showRecentlyPlayed() {
        // TODO: Show recently played
    }
    
    private data class NavItem(
        val icon: String,
        val label: String,
        val action: () -> Unit,
    )
}