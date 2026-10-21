package com.biketracker.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biketracker.data.model.AppUpdateInfo
import com.biketracker.data.repository.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface DownloadState {
    data object NotStarted : DownloadState
    data class Downloading(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState
    data class ReadyToInstall(val apkFile: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpdateAvailable(
        val updateInfo: AppUpdateInfo,
        val downloadState: DownloadState = DownloadState.NotStarted
    ) : UpdateUiState
}

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadedApk: File? = null

    fun checkForUpdates() {
        if (_uiState.value !is UpdateUiState.Idle) return

        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            val updateInfo = appUpdateRepository.checkForUpdate()
            if (updateInfo != null) {
                _uiState.value = UpdateUiState.UpdateAvailable(updateInfo)
            } else {
                _uiState.value = UpdateUiState.Idle
            }
        }
    }

    fun startDownload(updateInfo: AppUpdateInfo, context: Context) {
        viewModelScope.launch {
            _uiState.update { current ->
                if (current is UpdateUiState.UpdateAvailable) {
                    current.copy(
                        downloadState = DownloadState.Downloading(
                            progress = 0,
                            downloadedBytes = 0L,
                            totalBytes = updateInfo.apkSize
                        )
                    )
                } else current
            }

            try {
                val file = appUpdateRepository.downloadApk(
                    downloadUrl = updateInfo.downloadUrl,
                    expectedSize = updateInfo.apkSize
                ) { progress, downloaded, total ->
                    _uiState.update { current ->
                        if (current is UpdateUiState.UpdateAvailable) {
                            current.copy(
                                downloadState = DownloadState.Downloading(
                                    progress = progress,
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                            )
                        } else current
                    }
                }

                downloadedApk = file
                _uiState.update { current ->
                    if (current is UpdateUiState.UpdateAvailable) {
                        current.copy(downloadState = DownloadState.ReadyToInstall(file))
                    } else current
                }

                // Automatically trigger installation once downloaded
                installApk(context)
            } catch (e: Exception) {
                _uiState.update { current ->
                    if (current is UpdateUiState.UpdateAvailable) {
                        current.copy(
                            downloadState = DownloadState.Error(
                                e.localizedMessage ?: "Błąd pobierania aktualizacji"
                            )
                        )
                    } else current
                }
            }
        }
    }

    fun installApk(context: Context) {
        val file = downloadedApk ?: return
        if (!file.exists()) return

        if (!appUpdateRepository.canInstallUnknownApps(context)) {
            appUpdateRepository.openInstallPermissionSettings(context)
        }
        appUpdateRepository.installApk(context, file)
    }

    fun dismissForSession() {
        _uiState.value = UpdateUiState.Idle
    }

    fun ignoreThisVersion(versionIdentifier: String) {
        appUpdateRepository.ignoreVersion(versionIdentifier)
        _uiState.value = UpdateUiState.Idle
    }
}
