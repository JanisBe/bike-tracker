package com.biketracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface PowerSaveRepository {
    val shouldPromptRestoreFlow: StateFlow<Boolean>
    fun isPowerSaveModeActive(): Boolean
    fun onPowerSavePromptShown()
    fun onWorkoutStarted()
    fun onWorkoutFinished()
    fun dismissRestorePrompt()
}

@Singleton
class PowerSaveRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PowerSaveRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _shouldPromptRestoreFlow = MutableStateFlow(false)
    override val shouldPromptRestoreFlow: StateFlow<Boolean> = _shouldPromptRestoreFlow.asStateFlow()

    override fun isPowerSaveModeActive(): Boolean {
        return powerManager?.isPowerSaveMode == true
    }

    override fun onPowerSavePromptShown() {
        if (isPowerSaveModeActive()) {
            prefs.edit().putBoolean(KEY_POWER_SAVE_BEFORE_WORKOUT, true).apply()
        }
    }

    override fun onWorkoutStarted() {
        if (isPowerSaveModeActive()) {
            prefs.edit().putBoolean(KEY_POWER_SAVE_BEFORE_WORKOUT, true).apply()
        }
    }

    override fun onWorkoutFinished() {
        val wasEnabledBefore = prefs.getBoolean(KEY_POWER_SAVE_BEFORE_WORKOUT, false)
        val currentlyActive = isPowerSaveModeActive()

        if (wasEnabledBefore && !currentlyActive) {
            _shouldPromptRestoreFlow.value = true
        } else {
            prefs.edit().putBoolean(KEY_POWER_SAVE_BEFORE_WORKOUT, false).apply()
            _shouldPromptRestoreFlow.value = false
        }
    }

    override fun dismissRestorePrompt() {
        _shouldPromptRestoreFlow.value = false
        prefs.edit().putBoolean(KEY_POWER_SAVE_BEFORE_WORKOUT, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "bike_tracker_power_save"
        private const val KEY_POWER_SAVE_BEFORE_WORKOUT = "power_save_before_workout"
    }
}
