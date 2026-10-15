package com.trove.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trove.app.data.update.Release
import com.trove.app.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val release: Release? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    /** 0..1 once the download reports a size; null before that. */
    val progress: Float? = null,
    /** Set after a manual check that found nothing, so Settings can say so. */
    val upToDate: Boolean = false,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /** Launch check: skipped when a store such as Obtainium installed Trove and owns updates. */
    fun checkOnLaunch() {
        if (updateRepository.updatesItself) check()
    }

    fun check() {
        if (_uiState.value.isChecking) return
        _uiState.value = _uiState.value.copy(isChecking = true, upToDate = false)
        viewModelScope.launch {
            val release = updateRepository.newerRelease()
            _uiState.value = UpdateUiState(release = release, upToDate = release == null)
        }
    }

    fun download() {
        val release = _uiState.value.release ?: return
        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch {
            updateRepository.downloadAndInstall(release) { progress ->
                _uiState.value = _uiState.value.copy(progress = progress)
            }
            _uiState.value = UpdateUiState()
        }
    }

    fun dismiss() {
        _uiState.value = UpdateUiState()
    }
}
