package com.biketracker.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRepositoryTest {

    @Test
    fun parseSemVer_validTags_returnsCorrectComponents() {
        val v1 = AppUpdateRepositoryImpl.parseSemVer("v1.2.3")
        assertNotNull(v1)
        assertEquals(Triple(1, 2, 3), v1)

        val v2 = AppUpdateRepositoryImpl.parseSemVer("1.1.0")
        assertNotNull(v2)
        assertEquals(Triple(1, 1, 0), v2)

        val v3 = AppUpdateRepositoryImpl.parseSemVer("v2.5")
        assertNotNull(v3)
        assertEquals(Triple(2, 5, 0), v3)
    }

    @Test
    fun parseSemVer_invalidTags_returnsNull() {
        assertNull(AppUpdateRepositoryImpl.parseSemVer("latest"))
        assertNull(AppUpdateRepositoryImpl.parseSemVer(""))
        assertNull(AppUpdateRepositoryImpl.parseSemVer("alpha-release"))
    }

    @Test
    fun compareVersions_ordersCorrectly() {
        val v1 = Triple(1, 2, 0)
        val v2 = Triple(1, 1, 5)
        val v3 = Triple(1, 1, 5)

        assertTrue(AppUpdateRepositoryImpl.compareVersions(v1, v2) > 0)
        assertTrue(AppUpdateRepositoryImpl.compareVersions(v2, v1) < 0)
        assertEquals(0, AppUpdateRepositoryImpl.compareVersions(v2, v3))
    }

    @Test
    fun isReleaseNewer_newerSemVerTag_returnsTrue() {
        val isNewer = AppUpdateRepositoryImpl.isReleaseNewer(
            tagName = "v1.2.0",
            commitSha = null,
            assetUpdatedAt = "",
            publishedAt = "",
            currentVersionName = "1.1.0",
            currentGitSha = "abc1234",
            currentBuildTime = 1000L
        )
        assertTrue(isNewer)
    }

    @Test
    fun isReleaseNewer_olderSemVerTag_returnsFalse() {
        val isNewer = AppUpdateRepositoryImpl.isReleaseNewer(
            tagName = "v1.0.9",
            commitSha = null,
            assetUpdatedAt = "",
            publishedAt = "",
            currentVersionName = "1.1.0",
            currentGitSha = "abc1234",
            currentBuildTime = 1000L
        )
        assertFalse(isNewer)
    }

    @Test
    fun isReleaseNewer_latestTagWithDifferentCommit_returnsTrue() {
        val isNewer = AppUpdateRepositoryImpl.isReleaseNewer(
            tagName = "latest",
            commitSha = "def5678",
            assetUpdatedAt = "",
            publishedAt = "",
            currentVersionName = "1.1.0",
            currentGitSha = "abc1234",
            currentBuildTime = 1000L
        )
        assertTrue(isNewer)
    }

    @Test
    fun isReleaseNewer_latestTagWithSameCommit_returnsFalse() {
        val isNewer = AppUpdateRepositoryImpl.isReleaseNewer(
            tagName = "latest",
            commitSha = "abc1234",
            assetUpdatedAt = "",
            publishedAt = "",
            currentVersionName = "1.1.0",
            currentGitSha = "abc1234",
            currentBuildTime = 1000L
        )
        assertFalse(isNewer)
    }

    @Test
    fun isReleaseNewer_latestTagWithTimestamp_evaluatesCorrectly() {
        val baseTime = 1_700_000_000_000L // arbitrary fixed time in ms
        // 2 hours after build time in UTC: 2023-11-14T22:13:20Z (approx)
        val newerIso = "2026-10-21T12:00:00Z"
        val olderIso = "2026-10-20T12:00:00Z"

        // Build time is 2026-10-21T00:00:00Z
        val buildTime = 1792540800000L

        val isNewer = AppUpdateRepositoryImpl.isReleaseNewer(
            tagName = "latest",
            commitSha = null,
            assetUpdatedAt = newerIso,
            publishedAt = "",
            currentVersionName = "1.1.0",
            currentGitSha = "",
            currentBuildTime = buildTime
        )
        assertTrue(isNewer)

        val isOlder = AppUpdateRepositoryImpl.isReleaseNewer(
            tagName = "latest",
            commitSha = null,
            assetUpdatedAt = olderIso,
            publishedAt = "",
            currentVersionName = "1.1.0",
            currentGitSha = "",
            currentBuildTime = buildTime
        )
        assertFalse(isOlder)
    }
}
