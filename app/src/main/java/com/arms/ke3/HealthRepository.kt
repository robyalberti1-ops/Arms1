package com.arms.ke3

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HealthState(
    val steps: Int? = null,
    val heartRate: Int? = null,
    val spo2: Int? = null,
    val battery: Int? = null,
    val lastRaw: String = ""
)

object HealthRepository {
    private val _state = MutableStateFlow(HealthState())
    val state: StateFlow<HealthState> = _state

    fun updateBattery(level: Int) { _state.value = _state.value.copy(battery = level) }
    fun updateRaw(hex: String) {
        val old = _state.value
        val hr = parseHeartRate(hex) ?: old.heartRate
        _state.value = old.copy(heartRate = hr, lastRaw = hex)
    }

    // Protocollo salute ancora parziale: dai test reali abbiamo visto pacchetti E5-11-.. durante il battito.
    private fun parseHeartRate(hex: String): Int? {
        val parts = hex.split("-").mapNotNull { it.toIntOrNull(16) }
        if (parts.size >= 4 && parts[0] == 0xE5 && parts[1] == 0x11) {
            val candidate = parts.last()
            if (candidate in 40..220) return candidate
        }
        return null
    }
}
