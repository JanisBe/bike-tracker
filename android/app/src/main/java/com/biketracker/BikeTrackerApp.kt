package com.biketracker

import android.app.Application
import android.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class BikeTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid user agent for OSM tile servers
        Configuration.getInstance().apply {
            load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
            userAgentValue = packageName
        }
    }
}
