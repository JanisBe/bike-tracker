package com.biketracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.biketracker.ui.map.MapBaseSource
import com.biketracker.ui.map.MapOverlaySource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface MapPreferencesRepository {
    val baseSourceFlow: StateFlow<MapBaseSource>
    val activeOverlaysFlow: StateFlow<Set<MapOverlaySource>>
    fun getBaseSource(): MapBaseSource
    fun setBaseSource(source: MapBaseSource)
    fun getActiveOverlays(): Set<MapOverlaySource>
    fun toggleOverlay(source: MapOverlaySource, isEnabled: Boolean)
}

@Singleton
class MapPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : MapPreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _baseSourceFlow = MutableStateFlow(loadBaseSource())
    override val baseSourceFlow: StateFlow<MapBaseSource> = _baseSourceFlow.asStateFlow()

    private val _activeOverlaysFlow = MutableStateFlow(loadActiveOverlays())
    override val activeOverlaysFlow: StateFlow<Set<MapOverlaySource>> = _activeOverlaysFlow.asStateFlow()

    private fun loadBaseSource(): MapBaseSource {
        val saved = prefs.getString(KEY_BASE_SOURCE, MapBaseSource.OSM_STANDARD.id)
        return MapBaseSource.fromId(saved)
    }

    private fun loadActiveOverlays(): Set<MapOverlaySource> {
        val savedSet = prefs.getStringSet(KEY_ACTIVE_OVERLAYS, emptySet()) ?: emptySet()
        return savedSet.mapNotNull { MapOverlaySource.fromId(it) }.toSet()
    }

    override fun getBaseSource(): MapBaseSource = _baseSourceFlow.value

    override fun setBaseSource(source: MapBaseSource) {
        _baseSourceFlow.value = source
        prefs.edit().putString(KEY_BASE_SOURCE, source.id).apply()
    }

    override fun getActiveOverlays(): Set<MapOverlaySource> = _activeOverlaysFlow.value

    override fun toggleOverlay(source: MapOverlaySource, isEnabled: Boolean) {
        val current = _activeOverlaysFlow.value.toMutableSet()
        if (isEnabled) {
            current.add(source)
        } else {
            current.remove(source)
        }
        _activeOverlaysFlow.value = current
        val idSet = current.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_ACTIVE_OVERLAYS, idSet).apply()
    }

    companion object {
        private const val PREFS_NAME = "bike_tracker_map_preferences"
        private const val KEY_BASE_SOURCE = "map_base_source"
        private const val KEY_ACTIVE_OVERLAYS = "map_active_overlays"
    }
}
