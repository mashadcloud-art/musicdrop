package com.musicdrop.app.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerPreset(
    val name: String,
    // Band gains in mB (typically -1500 to +1500, or -15dB to +15dB)
    val bandGains: List<Short>
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val currentPreset: String = "Normal",
    val bandGains: List<Int> = listOf(0, 0, 0, 0, 0), // in dB (-12 to +12)
    val bassBoost: Int = 0, // 0 - 100
    val virtualizer: Int = 0 // 0 - 100
)

class EqualizerManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("musicdrop_eq_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    companion object {
        val PRESETS = listOf(
            EqualizerPreset("Normal", listOf(0, 0, 0, 0, 0)),
            EqualizerPreset("Bass Boost", listOf(600, 400, 200, 0, 0)),
            EqualizerPreset("Vocal Boost", listOf(-200, 300, 600, 300, -100)),
            EqualizerPreset("Rock", listOf(500, 300, -100, 300, 500)),
            EqualizerPreset("Pop", listOf(-100, 200, 500, 200, -200)),
            EqualizerPreset("Hip Hop", listOf(500, 300, 0, 200, 400)),
            EqualizerPreset("Electronic", listOf(400, 200, 0, 200, 400)),
            EqualizerPreset("Jazz", listOf(0, 200, -100, 300, 400)),
            EqualizerPreset("Classical", listOf(400, 300, -200, 400, 300)),
            EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0))
        )

        val BAND_LABELS = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    }

    private fun loadInitialState(): EqualizerState {
        val enabled = prefs.getBoolean("eq_enabled", true)
        val preset = prefs.getString("eq_preset", "Normal") ?: "Normal"
        val bass = prefs.getInt("eq_bass", 25)
        val virt = prefs.getInt("eq_virt", 15)
        val gains = (0 until 5).map { prefs.getInt("eq_band_$it", 0) }
        return EqualizerState(
            isEnabled = enabled,
            currentPreset = preset,
            bandGains = gains,
            bassBoost = bass,
            virtualizer = virt
        )
    }

    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentAudioSessionId) return
        currentAudioSessionId = audioSessionId
        releaseEffects()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            bassBoostEffect = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength((_state.value.bassBoost * 10).toShort())
                }
            }
            virtualizerEffect = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength((_state.value.virtualizer * 10).toShort())
                }
            }
            applyBandGainsToHardware()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
        _state.value = _state.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoostEffect?.enabled = enabled
            virtualizerEffect?.enabled = enabled
        } catch (_: Throwable) {}
    }

    fun setBandGain(bandIndex: Int, gainDb: Int) {
        val current = _state.value.bandGains.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb.coerceIn(-12, 12)
            prefs.edit().putInt("eq_band_$bandIndex", current[bandIndex]).putString("eq_preset", "Custom").apply()
            _state.value = _state.value.copy(bandGains = current, currentPreset = "Custom")
            try {
                equalizer?.let { eq ->
                    if (bandIndex < eq.numberOfBands) {
                        val mB = (current[bandIndex] * 100).toShort()
                        eq.setBandLevel(bandIndex.toShort(), mB)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    fun applyPreset(presetName: String) {
        val p = PRESETS.firstOrNull { it.name == presetName } ?: return
        val gainsDb = p.bandGains.map { (it / 100).toInt() }
        val edit = prefs.edit().putString("eq_preset", presetName)
        gainsDb.forEachIndexed { idx, v -> edit.putInt("eq_band_$idx", v) }
        edit.apply()
        _state.value = _state.value.copy(currentPreset = presetName, bandGains = gainsDb)
        applyBandGainsToHardware()
    }

    fun setBassBoost(strengthPercent: Int) {
        val clamped = strengthPercent.coerceIn(0, 100)
        prefs.edit().putInt("eq_bass", clamped).apply()
        _state.value = _state.value.copy(bassBoost = clamped)
        try {
            bassBoostEffect?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength((clamped * 10).toShort())
                }
            }
        } catch (_: Throwable) {}
    }

    fun setVirtualizer(strengthPercent: Int) {
        val clamped = strengthPercent.coerceIn(0, 100)
        prefs.edit().putInt("eq_virt", clamped).apply()
        _state.value = _state.value.copy(virtualizer = clamped)
        try {
            virtualizerEffect?.let { virt ->
                if (virt.strengthSupported) {
                    virt.setStrength((clamped * 10).toShort())
                }
            }
        } catch (_: Throwable) {}
    }

    private fun applyBandGainsToHardware() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            _state.value.bandGains.forEachIndexed { index, gainDb ->
                if (index < numBands) {
                    val mB = (gainDb * 100).toShort()
                    eq.setBandLevel(index.toShort(), mB)
                }
            }
        } catch (_: Throwable) {}
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoostEffect?.release()
            virtualizerEffect?.release()
        } catch (_: Throwable) {}
        equalizer = null
        bassBoostEffect = null
        virtualizerEffect = null
    }
}
