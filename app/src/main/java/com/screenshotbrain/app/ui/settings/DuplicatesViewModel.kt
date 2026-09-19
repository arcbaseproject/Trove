package com.trove.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trove.app.data.repository.DeleteRequestOutcome
import com.trove.app.data.repository.ScreenshotRepository
import com.trove.app.domain.model.ScreenshotUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DuplicateGroupUi(val screenshots: List<ScreenshotUi>)

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val groups: List<DuplicateGroupUi> = emptyList(),
)

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val groups = screenshotRepository.findExactDuplicates()
        val resolved = groups.mapNotNull { group ->
            val screenshots = screenshotRepository.getScreenshotsByIds(group.screenshotIds)
                .sortedByDescending { it.dateAddedSec }
            if (screenshots.size < 2) null else DuplicateGroupUi(screenshots)
        }
        _uiState.value = DuplicatesUiState(isLoading = false, groups = resolved)
    }

    suspend fun requestDeleteAllButFirst(group: DuplicateGroupUi): DeleteRequestOutcome {
        val idsToDelete = group.screenshots.drop(1).map { it.id }
        val outcome = screenshotRepository.requestDelete(idsToDelete)
        if (outcome is DeleteRequestOutcome.Deleted) {
            screenshotRepository.removeLocalRows(outcome.ids)
        }
        return outcome
    }
}
