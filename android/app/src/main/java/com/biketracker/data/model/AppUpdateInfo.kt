package com.biketracker.data.model

data class AppUpdateInfo(
    val releaseId: Long,
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val commitSha: String?,
    val publishedAt: String,
    val versionIdentifier: String
)
