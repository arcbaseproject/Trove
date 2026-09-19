package com.trove.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trove.app.data.repository.SearchRepository
import com.trove.app.domain.model.ScreenshotUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: List<ScreenshotUi> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
)

/** Debounced so the FTS query only runs once the user pauses typing, keeping results feeling instant without hammering Room on every keystroke. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    private val searchResults: StateFlow<SearchUiState> = _query
        .debounce { if (it.isBlank()) 0L else 220L }
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchUiState(query = query))
            } else {
                flow {
                    emit(SearchUiState(query = query, isSearching = true))
                    val results = searchRepository.search(query)
                    emit(SearchUiState(query = query, results = results, hasSearched = true))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    val uiState: StateFlow<SearchUiState> = combine(_query, searchResults) { query, results ->
        results.copy(query = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }
}
