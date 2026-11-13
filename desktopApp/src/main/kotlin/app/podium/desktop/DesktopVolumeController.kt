package app.podium.desktop

import app.podium.player.api.VolumeController
import app.podium.player.api.VolumeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop volume controller - simple implementation for MVP.
 * Uses application-level volume control (not system mixer).
 */
class DesktopVolumeController : VolumeController {
    
    private val _state = MutableStateFlow(VolumeState(level = 50, max = 100, isFixed = false))
    override val state: StateFlow<VolumeState> = _state.asStateFlow()
    
    override fun step(delta: Int) {
        val current = _state.value.level
        val newLevel = (current + delta).coerceIn(0, 100)
        setVolume(newLevel)
    }
    
    fun setVolume(level: Int) {
        val boundedLevel = level.coerceIn(0, 100)
        _state.value = VolumeState(level = boundedLevel, max = 100, isFixed = false)
    }
    
    fun shutdown() {
        // No-op for MVP
    }
}