package com.biketracker.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketracker.BuildConfig
import com.biketracker.ui.theme.DarkBackground
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.SuccessGreen
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ChangelogSection(
    val category: String,
    val items: List<String>
)

data class ChangelogRelease(
    val version: String,
    val date: String,
    val sections: List<ChangelogSection>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var releases by remember { mutableStateOf<List<ChangelogRelease>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 1. Instant fallback from bundled assets
        val assetContent = withContext(Dispatchers.IO) {
            loadFromAssets(context)
        }
        if (!assetContent.isNullOrBlank()) {
            releases = parseChangelogMarkdown(assetContent)
            isLoading = false
        }

        // 2. Fetch fresh version from GitHub Pages online
        val onlineContent = withContext(Dispatchers.IO) {
            fetchOnlineChangelog()
        }
        if (!onlineContent.isNullOrBlank()) {
            releases = parseChangelogMarkdown(onlineContent)
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TextSecondary.copy(alpha = 0.5f)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📜",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = "Historia zmian",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bike Tracker v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrangeAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zamknij",
                        tint = TextSecondary
                    )
                }
            }

            if (isLoading && releases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = OrangeAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(releases) { release ->
                        ChangelogReleaseCard(release)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangelogReleaseCard(release: ChangelogRelease) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground, RoundedCornerShape(14.dp))
            .border(1.dp, TextSecondary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Version and Date Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(OrangeAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, OrangeAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "v${release.version}",
                    color = OrangeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (release.date.isNotBlank()) {
                Text(
                    text = release.date,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Sections (Dodano, Poprawiono)
        release.sections.forEach { section ->
            val isAdded = section.category.contains("dodano", ignoreCase = true)
            val isFixed = section.category.contains("poprawiono", ignoreCase = true)
            val badgeColor = if (isAdded) SuccessGreen else if (isFixed) TealAccent else TextSecondary

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = section.category.uppercase(),
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                section.items.forEach { itemText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = OrangeAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatMarkdownBold(itemText),
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatMarkdownBold(text: String) = buildAnnotatedString {
    val parts = text.split("**")
    for (i in parts.indices) {
        if (i % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append(parts[i])
            }
        } else {
            append(parts[i])
        }
    }
}

private fun loadFromAssets(context: Context): String? {
    return try {
        context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

private fun fetchOnlineChangelog(): String? {
    return try {
        val url = URL("https://janisbe.github.io/bike-tracker/CHANGELOG.md")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            useCaches = false
        }
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun parseChangelogMarkdown(markdown: String): List<ChangelogRelease> {
    val releases = mutableListOf<ChangelogRelease>()
    val lines = markdown.lines()

    var currentVersion: String? = null
    var currentDate: String = ""
    val currentSections = mutableListOf<ChangelogSection>()
    var currentCategory: String? = null
    val currentItems = mutableListOf<String>()

    fun finishCategory() {
        if (currentCategory != null && currentItems.isNotEmpty()) {
            currentSections.add(ChangelogSection(currentCategory!!, currentItems.toList()))
            currentItems.clear()
        }
        currentCategory = null
    }

    fun finishRelease() {
        finishCategory()
        if (currentVersion != null) {
            releases.add(ChangelogRelease(currentVersion!!, currentDate, currentSections.toList()))
            currentSections.clear()
        }
        currentVersion = null
        currentDate = ""
    }

    for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.startsWith("## ")) {
            finishRelease()
            val match = Regex("""##\s+\[(.*?)\](?:\s+-\s+(.*))?""").find(line)
            if (match != null) {
                currentVersion = match.groupValues.getOrNull(1).orEmpty()
                currentDate = match.groupValues.getOrNull(2).orEmpty()
            }
        } else if (line.startsWith("### ")) {
            finishCategory()
            currentCategory = line.removePrefix("### ").trim()
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            val item = line.substring(2).trim()
            if (item.isNotEmpty()) {
                currentItems.add(item)
            }
        }
    }
    finishRelease()
    return releases
}
