package com.biketracker

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class BikeTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid user agent and preferences for OSM tile servers
        val defaultPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        Configuration.getInstance().apply {
            load(applicationContext, defaultPreferences)
            userAgentValue = packageName
        }
    }
}
