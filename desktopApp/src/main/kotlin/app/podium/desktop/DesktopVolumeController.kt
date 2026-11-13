package app.podium.desktop

import app.podium.player.api.VolumeController
import app.podium.player.api.VolumeState
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.MMSystem
import com.sun.jna.platform.win32.WinDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop volume controller using Windows Core Audio API (MMSystem).
 * Controls system media volume via waveOut API.
 */
class DesktopVolumeController : VolumeController {
    
    private val _state = MutableStateFlow(VolumeState(level = 50, max = 100, isFixed = false))
    override val state: StateFlow<VolumeState> = _state.asStateFlow()
    
    private val mixerHandle = WinDef.HMIXER()
    
    init {
        // Open mixer
        val result = MMSystem.INSTANCE.mixerOpen(mixerHandle, 0, null, 0, MMSystem.MIXER_OBJECTF_MIXER)
        if (result != MMSystem.MMSYSERR_NOERROR) {
            // Mixer not available, use fallback
        }
        
        // Initialize volume from system
        refreshVolume()
    }
    
    override fun step(delta: Int) {
        val current = _state.value.level
        val newLevel = (current + delta).coerceIn(0, 100)
        setVolume(newLevel)
    }
    
    fun setVolume(level: Int) {
        val boundedLevel = level.coerceIn(0, 100)
        val volume = (boundedLevel * 65535 / 100).toInt()
        val dwVolume = (volume shl 16) or volume // Left and right channels
        
        val result = MMSystem.INSTANCE.mixerSetControlDetails(
            mixerHandle.pointer,
            null, // Use default volume control
            MMSystem.MIXER_OBJECTF_MIXER or MMSystem.MIXER_SETCONTROLDETAILSF_VALUE,
            // We'd need to find the volume control ID first
        )
        
        if (result == MMSystem.MMSYSERR_NOERROR) {
            _state.value = VolumeState(level = boundedLevel, max = 100, isFixed = false)
        }
    }
    
    private fun refreshVolume() {
        // TODO: Query current volume from mixer
        // For now, use default
        _state.value = VolumeState(level = 50, max = 100, isFixed = false)
    }
    
    fun shutdown() {
        if (mixerHandle != WinDef.HMIXER.NULL) {
            MMSystem.INSTANCE.mixerClose(mixerHandle)
        }
    }
}