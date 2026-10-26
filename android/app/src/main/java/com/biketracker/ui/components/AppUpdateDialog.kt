package com.biketracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketracker.R
import com.biketracker.data.model.AppUpdateInfo
import com.biketracker.ui.theme.DarkBackground
import com.biketracker.ui.theme.DarkSurface
import com.biketracker.ui.theme.ErrorRed
import com.biketracker.ui.theme.OrangeAccent
import com.biketracker.ui.theme.SuccessGreen
import com.biketracker.ui.theme.TealAccent
import com.biketracker.ui.theme.TextPrimary
import com.biketracker.ui.theme.TextSecondary
import com.biketracker.ui.update.DownloadState
import java.util.Locale

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onSkip: () -> Unit,
    onIgnore: () -> Unit
) {
    val context = LocalContext.current

    val displayVersion = if (updateInfo.tagName.isNotBlank() && updateInfo.tagName != "latest") {
        updateInfo.tagName
    } else if (!updateInfo.commitSha.isNullOrBlank()) {
        "build (${updateInfo.commitSha.take(7)})"
    } else {
        updateInfo.releaseName
    }

    val formattedSize = if (updateInfo.apkSize > 0) {
        String.format(Locale.US, "%.1f MB", updateInfo.apkSize / (1024f * 1024f))
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is DownloadState.Downloading) {
                onSkip()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = TealAccent,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_update_title),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.dialog_update_version, displayVersion),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (formattedSize != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dialog_update_size, formattedSize),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (downloadState) {
                    is DownloadState.NotStarted -> {
                        // Standard state before download begins
                    }
                    is DownloadState.Downloading -> {
                        if (downloadState.progress >= 0) {
                            Text(
                                text = stringResource(R.string.dialog_update_downloading, downloadState.progress),
                                color = TealAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = TealAccent,
                                trackColor = DarkBackground
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.dialog_update_downloading_indeterminate),
                                color = TealAccent,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = TealAccent,
                                trackColor = DarkBackground
                            )
                        }
                    }
                    is DownloadState.ReadyToInstall -> {
                        Text(
                            text = stringResource(R.string.dialog_update_downloaded),
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is DownloadState.Error -> {
                        Text(
                            text = downloadState.message,
                            color = ErrorRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                when (downloadState) {
                    is DownloadState.NotStarted -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.dialog_update_download_action),
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is DownloadState.Downloading -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (downloadState.progress >= 0) {
                                    "${downloadState.progress}%"
                                } else {
                                    stringResource(R.string.dialog_update_downloading_indeterminate)
                                }
                            )
                        }
                    }
                    is DownloadState.ReadyToInstall -> {
                        Button(
                            onClick = onInstall,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_update_install_action),
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is DownloadState.Error -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_update_retry_action),
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onIgnore,
                        enabled = downloadState !is DownloadState.Downloading
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_update_ignore_action),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = onSkip,
                        enabled = downloadState !is DownloadState.Downloading
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_update_skip_action),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
