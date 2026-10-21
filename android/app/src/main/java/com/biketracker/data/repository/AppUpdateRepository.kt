package com.biketracker.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.biketracker.BuildConfig
import com.biketracker.data.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

interface AppUpdateRepository {
    suspend fun checkForUpdate(): AppUpdateInfo?
    fun isVersionIgnored(versionIdentifier: String): Boolean
    fun ignoreVersion(versionIdentifier: String)
    suspend fun downloadApk(
        downloadUrl: String,
        expectedSize: Long = -1L,
        onProgress: (progress: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File
    fun installApk(context: Context, apkFile: File)
    fun canInstallUnknownApps(context: Context): Boolean
    fun openInstallPermissionSettings(context: Context)
}

@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppUpdateRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_LATEST_RELEASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "BikeTracker-Android")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext null
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseText)
            val releaseId = json.optLong("id")
            val tagName = json.optString("tag_name", "")
            val releaseName = json.optString("name", "")
            val body = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            var apkSize = 0L
            var assetUpdatedAt = ""

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName == "bike-tracker.apk" || assetName.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url")
                    apkSize = asset.optLong("size", 0L)
                    assetUpdatedAt = asset.optString("updated_at", "")
                    if (assetName == "bike-tracker.apk") {
                        break
                    }
                }
            }

            if (apkUrl.isNullOrBlank()) {
                return@withContext null
            }

            val commitSha = Regex("""commit [`']?([0-9a-fA-F]{7,40})[`']?""")
                .find(body)
                ?.groupValues
                ?.getOrNull(1)

            val versionIdentifier = if (tagName.isNotBlank() && tagName != "latest") {
                tagName
            } else {
                commitSha ?: releaseId.toString()
            }

            if (isVersionIgnored(versionIdentifier)) {
                return@withContext null
            }

            val isNewer = isReleaseNewer(
                tagName = tagName,
                commitSha = commitSha,
                assetUpdatedAt = assetUpdatedAt,
                publishedAt = publishedAt
            )

            if (!isNewer) {
                return@withContext null
            }

            AppUpdateInfo(
                releaseId = releaseId,
                tagName = tagName,
                releaseName = releaseName.ifBlank { "Bike Tracker Update" },
                releaseNotes = body,
                downloadUrl = apkUrl,
                apkSize = apkSize,
                commitSha = commitSha,
                publishedAt = publishedAt,
                versionIdentifier = versionIdentifier
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun isVersionIgnored(versionIdentifier: String): Boolean {
        val ignored = prefs.getString(KEY_IGNORED_VERSION, null)
        return ignored != null && ignored == versionIdentifier
    }

    override fun ignoreVersion(versionIdentifier: String) {
        prefs.edit().putString(KEY_IGNORED_VERSION, versionIdentifier).apply()
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        expectedSize: Long,
        onProgress: (progress: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        val targetFile = File(updatesDir, "bike-tracker.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        var currentUrl = downloadUrl
        var connection: HttpURLConnection
        var redirectCount = 0

        while (true) {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "BikeTracker-Android")
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000

            val responseCode = connection.responseCode
            if (responseCode in 300..399 && redirectCount < 5) {
                val newLocation = connection.getHeaderField("Location")
                connection.disconnect()
                if (newLocation != null) {
                    currentUrl = newLocation
                    redirectCount++
                    continue
                }
            }
            break
        }

        val totalBytes = if (connection.contentLengthLong > 0) {
            connection.contentLengthLong
        } else {
            expectedSize
        }

        connection.inputStream.use { inputStream: InputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalDownloaded = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalDownloaded += bytesRead

                    val progress = if (totalBytes > 0) {
                        ((totalDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        -1
                    }
                    onProgress(progress, totalDownloaded, totalBytes)
                }
                outputStream.flush()
            }
        }
        connection.disconnect()

        targetFile
    }

    override fun installApk(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    override fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    override fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    companion object {
        private const val GITHUB_API_LATEST_RELEASE_URL =
            "https://api.github.com/repos/JanisBe/bike-tracker/releases/latest"
        private const val PREFS_NAME = "bike_tracker_app_update"
        private const val KEY_IGNORED_VERSION = "ignored_version_identifier"

        internal fun isReleaseNewer(
            tagName: String,
            commitSha: String?,
            assetUpdatedAt: String,
            publishedAt: String,
            currentVersionName: String = BuildConfig.VERSION_NAME,
            currentGitSha: String = BuildConfig.GIT_SHA,
            currentBuildTime: Long = BuildConfig.BUILD_TIME
        ): Boolean {
            // 1. If release has a semantic version tag (e.g. v1.2.0 or 1.2.0)
            val tagVersion = parseSemVer(tagName)
            val currentVersion = parseSemVer(currentVersionName)
            if (tagVersion != null && currentVersion != null) {
                val comparison = compareVersions(tagVersion, currentVersion)
                if (comparison != 0) {
                    return comparison > 0
                }
            }

            // 2. If release is on 'latest' tag or semver is identical, compare Git commit SHA
            if (!commitSha.isNullOrBlank() && currentGitSha.isNotBlank()) {
                val isSameCommit = commitSha.startsWith(currentGitSha, ignoreCase = true) ||
                        currentGitSha.startsWith(commitSha, ignoreCase = true)
                return !isSameCommit
            }

            // 3. Fallback: compare release publication / asset timestamp with build time
            val targetTimestampStr = assetUpdatedAt.ifBlank { publishedAt }
            val releaseMillis = parseIso8601ToMillis(targetTimestampStr)
            if (releaseMillis > 0 && currentBuildTime > 0) {
                // 60-second grace window to prevent false positives when testing a build created at the same time
                return releaseMillis > (currentBuildTime + 60_000L)
            }

            return false
        }

        internal fun parseSemVer(versionStr: String): Triple<Int, Int, Int>? {
            val match = Regex("""(?:v|version)?\s*(\d+)\.(\d+)(?:\.(\d+))?""", RegexOption.IGNORE_CASE)
                .find(versionStr) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: 0
            val minor = match.groupValues[2].toIntOrNull() ?: 0
            val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
            return Triple(major, minor, patch)
        }

        internal fun compareVersions(v1: Triple<Int, Int, Int>, v2: Triple<Int, Int, Int>): Int {
            if (v1.first != v2.first) return v1.first.compareTo(v2.first)
            if (v1.second != v2.second) return v1.second.compareTo(v2.second)
            return v1.third.compareTo(v2.third)
        }

        internal fun parseIso8601ToMillis(isoString: String): Long {
            if (isoString.isBlank()) return 0L
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format.parse(isoString)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}
