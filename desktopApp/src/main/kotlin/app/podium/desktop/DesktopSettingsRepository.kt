package app.podium.desktop

import app.podium.core.common.Outcome
import app.podium.core.common.PodiumError
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Desktop settings repository using JSON file in %APPDATA%\Podium\settings.json
 */
class DesktopSettingsRepository(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
    private val settingsFile: File = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Podium/settings.json"),
) {
    
    private var _settings: PodiumSettings = load()
    
    val settings: PodiumSettings
        get() = _settings
    
    fun load(): PodiumSettings {
        if (settingsFile.exists()) {
            return try {
                json.decodeFromString<PodiumSettings>(settingsFile.readText())
            } catch (e: Exception) {
                // Corrupted settings file, start fresh
                PodiumSettings.Default
            }
        }
        return PodiumSettings.Default
    }
    
    fun save(settings: PodiumSettings): Outcome<Unit> {
        _settings = settings
        return try {
            settingsFile.parentFile?.mkdirs()
            settingsFile.writeText(json.encodeToString(settings))
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Failure(PodiumError.Unexpected(e))
        }
    }
    
    fun update(block: PodiumSettings.() -> PodiumSettings): Outcome<Unit> {
        return save(_settings.block())
    }
}

/**
 * Podium settings matching the Android Proto DataStore schema.
 * All fields have sensible defaults for desktop.
 */
@Serializable
data class PodiumSettings(
    val theme: Theme = Theme.AUTO,
    val highlight: Highlight = Highlight.BLUE,
    val transparency: Transparency = Transparency.FULL,
    val wheelSize: WheelSize = WheelSize.MEDIUM,
    val wheelHandedness: WheelHandedness = WheelHandedness.RIGHT,
    val wheelVisibility: WheelVisibility = WheelVisibility.AUTO,
    val hapticsEnabled: Boolean = false, // Desktop: no haptics by default
    val clicker: Clicker = Clicker.OFF,
    val showNowPlayingOnPlay: Boolean = true,
    val qualityPolicy: QualityPolicy = QualityPolicy.Default,
    val gaplessEnabled: Boolean = true,
    val replayGain: ReplayGain = ReplayGain.OFF,
    val crossfade: Crossfade = Crossfade.OFF,
    val autoplayEnabled: Boolean = true,
    val saveSearchHistory: Boolean = true,
    val lyricsOnlineConsent: LyricsConsent = LyricsConsent.UNASKED,
    val skipUnavailableOffline: Boolean = false,
    val bitPerfectUsb: Boolean = false,
    val onboardingVersionSeen: String = "",
    
    companion object {
        val Default = PodiumSettings()
    }
)

enum class Theme { AUTO, LIGHT, DARK }
enum class Highlight { BLUE, GRAPHITE, ARTWORK }
enum class Transparency { FULL, REDUCED, OFF }
enum class WheelSize { SMALL, MEDIUM, LARGE }
enum class WheelHandedness { LEFT, RIGHT }
enum class WheelVisibility { ALWAYS, AUTO, HIDDEN }
enum class Clicker { OFF, ON }
enum class QualityPolicy { 
    DEFAULT,
    CUSTOM
}
enum class ReplayGain { OFF, TRACK, ALBUM }
enum class Crossfade { OFF, S_2, S_4, S_6, S_8, S_12 }
enum class LyricsConsent { UNASKED, ALLOWED, DENIED }