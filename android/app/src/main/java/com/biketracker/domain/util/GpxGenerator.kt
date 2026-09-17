package com.biketracker.domain.util

import com.biketracker.data.model.TrackPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxGenerator {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Generates a standard GPX 1.1 XML string from recorded track points.
     */
    fun generate(points: List<TrackPoint>, rideName: String = "Bike Ride"): String {
        val nowIso = isoFormat.format(Date())

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="BikeTracker Android" xmlns="http://www.topografix.com/GPX/1/1">""")
            appendLine("  <metadata>")
            appendLine("    <name>$rideName</name>")
            appendLine("    <time>$nowIso</time>")
            appendLine("  </metadata>")
            appendLine("  <trk>")
            appendLine("    <name>$rideName</name>")
            appendLine("    <trkseg>")

            for ((latitude, longitude, elevation, timestamp) in points) {
                appendLine("""      <trkpt lat="$latitude" lon="$longitude">""")
                if (elevation != null) {
                    appendLine("        <ele>${"%.1f".format(Locale.US, elevation)}</ele>")
                }
                appendLine("        <time>${isoFormat.format(Date(timestamp))}</time>")
                appendLine("      </trkpt>")
            }

            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    /**
     * Saves GPX content to a cache file for sharing.
     */
    fun saveToCache(cacheDir: File, fileName: String, gpxContent: String): File {
        val safeName = if (fileName.endsWith(".gpx")) fileName else "$fileName.gpx"
        val file = File(cacheDir, safeName)
        file.writeText(gpxContent, Charsets.UTF_8)
        return file
    }
}
