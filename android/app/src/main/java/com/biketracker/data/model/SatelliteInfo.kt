package com.biketracker.data.model

data class SatelliteInfo(
    val used: Int = 0,
    val total: Int = 0
) {
    val isGoodSignal: Boolean
        get() = used >= 7

    val signalQuality: SignalQuality
        get() = when {
            used >= 12 -> SignalQuality.EXCELLENT
            used >= 7 -> SignalQuality.GOOD
            used >= 4 -> SignalQuality.POOR
            else -> SignalQuality.NONE
        }
}

enum class SignalQuality {
    NONE,
    POOR,
    GOOD,
    EXCELLENT
}
