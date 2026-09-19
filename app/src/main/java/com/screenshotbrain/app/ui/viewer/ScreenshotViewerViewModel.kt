package com.trove.app.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trove.app.data.repository.CollectionRepository
import com.trove.app.data.repository.DeleteRequestOutcome
import com.trove.app.data.repository.ScreenshotRepository
import com.trove.app.data.repository.SearchRepository
import com.trove.app.domain.model.CollectionUi
import com.trove.app.domain.model.ScreenshotDetail
import com.trove.app.domain.model.ScreenshotUi
import com.trove.app.ui.navigation.ScreenshotSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ViewerUiState(
    val screenshots: List<ScreenshotUi> = emptyList(),
    val initialIndex: Int = 0,
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val showDetails: Boolean = false,
)

@HiltViewModel
class ScreenshotViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val screenshotRepository: ScreenshotRepository,
    private val searchRepository: SearchRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    private val source = ScreenshotSource.parse(checkNotNull(savedStateHandle["source"]))
    private val initialScreenshotId: Long = checkNotNull(savedStateHandle.get<String>("screenshotId")).toLong()

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState

    val collections: StateFlow<List<CollectionUi>> = collectionRepository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val list = when (source) {
                is ScreenshotSource.All -> screenshotRepository.listAllOnce()
                is ScreenshotSource.Category -> screenshotRepository.listByCategoryOnce(source.key)
                is ScreenshotSource.Collection -> screenshotRepository.listByCollectionOnce(source.id)
                is ScreenshotSource.SearchResults -> searchRepository.search(source.query)
            }
            val index = list.indexOfFirst { it.id == initialScreenshotId }.coerceAtLeast(0)
            _uiState.value = ViewerUiState(screenshots = list, initialIndex = index, currentIndex = index, isLoading = false)
        }
    }

    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun toggleDetails() {
        _uiState.value = _uiState.value.copy(showDetails = !_uiState.value.showDetails)
    }

    fun detailFor(screenshotId: Long): Flow<ScreenshotDetail?> =
        screenshotRepository.observeDetail(screenshotId)

    fun setFavorite(screenshotId: Long, isFavorite: Boolean) = viewModelScope.launch {
        screenshotRepository.setFavorite(screenshotId, isFavorite)
    }

    fun updateNotes(screenshotId: Long, notes: String) = viewModelScope.launch {
        screenshotRepository.updateNotes(screenshotId, notes.ifBlank { null })
    }

    fun addTag(screenshotId: Long, tagName: String) = viewModelScope.launch {
        screenshotRepository.addTag(screenshotId, tagName)
    }

    fun removeTag(screenshotId: Long, tagId: Long) = viewModelScope.launch {
        screenshotRepository.removeTag(screenshotId, tagId)
    }

    fun addToCollection(screenshotId: Long, collectionId: Long) = viewModelScope.launch {
        screenshotRepository.addToCollection(screenshotId, collectionId)
    }

    fun removeFromCollection(screenshotId: Long, collectionId: Long) = viewModelScope.launch {
        screenshotRepository.removeFromCollection(screenshotId, collectionId)
    }

    suspend fun requestDelete(screenshotId: Long): DeleteRequestOutcome {
        val outcome = screenshotRepository.requestDelete(listOf(screenshotId))
        if (outcome is DeleteRequestOutcome.Deleted) {
            screenshotRepository.removeLocalRows(outcome.ids)
        }
        return outcome
    }

    fun confirmDelete(ids: List<Long>) = viewModelScope.launch {
        screenshotRepository.removeLocalRows(ids)
    }
}
