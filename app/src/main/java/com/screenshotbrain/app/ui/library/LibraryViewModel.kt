package com.trove.app.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.trove.app.data.repository.DeleteRequestOutcome
import com.trove.app.data.repository.ScreenshotRepository
import com.trove.app.data.settings.SettingsKeys
import com.trove.app.data.settings.SettingsRepository
import com.trove.app.domain.model.CategoryKey
import com.trove.app.domain.model.LibraryListItem
import com.trove.app.util.timelineSectionFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class LibraryViewMode { GRID, TIMELINE }

data class LibraryUiState(
    val selection: Set<Long> = emptySet(),
    val categoryTitle: String? = null,
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val screenshotRepository: ScreenshotRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val categoryKey: String? = savedStateHandle.get<String>("category")?.takeUnless { it == "_" }

    private val _uiState = MutableStateFlow(
        LibraryUiState(categoryTitle = categoryKey?.let { CategoryKey.fromKey(it)?.displayName }),
    )
    val uiState: StateFlow<LibraryUiState> = _uiState

    val pagingData = (
        if (categoryKey != null) {
            screenshotRepository.pagedScreenshotsByCategory(categoryKey)
        } else {
            screenshotRepository.pagedScreenshots()
        }
        ).cachedIn(viewModelScope)

    val timelinePagingData = pagingData.map { data ->
        data.map { LibraryListItem.Item(it) as LibraryListItem }
            .insertSeparators { before, after ->
                val afterScreenshot = (after as? LibraryListItem.Item)?.screenshot ?: return@insertSeparators null
                val beforeScreenshot = (before as? LibraryListItem.Item)?.screenshot
                val afterSection = timelineSectionFor(afterScreenshot.dateAddedSec)
                val beforeSection = beforeScreenshot?.let { timelineSectionFor(it.dateAddedSec) }
                if (beforeSection == null || beforeSection != afterSection) {
                    LibraryListItem.Header(afterSection.label)
                } else {
                    null
                }
            }
    }.cachedIn(viewModelScope)

    // Saved as a setting so the Library opens in the last-used view.
    fun setViewMode(mode: LibraryViewMode) = viewModelScope.launch {
        settingsRepository.set(SettingsKeys.TIMELINE_VIEW, mode == LibraryViewMode.TIMELINE)
    }

    fun toggleSelection(id: Long) {
        val current = _uiState.value.selection
        _uiState.value = _uiState.value.copy(
            selection = if (id in current) current - id else current + id,
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selection = emptySet())
    }

    fun toggleFavoriteForSelection() = viewModelScope.launch {
        val ids = _uiState.value.selection
        val makeFavorite = !screenshotRepository.getScreenshotsByIds(ids.toList()).all { it.isFavorite }
        ids.forEach { screenshotRepository.setFavorite(it, makeFavorite) }
        clearSelection()
    }

    suspend fun requestDeleteSelection(): DeleteRequestOutcome {
        val outcome = screenshotRepository.requestDelete(_uiState.value.selection.toList())
        if (outcome is DeleteRequestOutcome.Deleted) {
            screenshotRepository.removeLocalRows(outcome.ids)
        }
        clearSelection()
        return outcome
    }

    fun confirmDelete(ids: List<Long>) = viewModelScope.launch {
        screenshotRepository.removeLocalRows(ids)
    }

    suspend fun contentUrisFor(ids: List<Long>): List<String> = screenshotRepository.getContentUris(ids)
}
