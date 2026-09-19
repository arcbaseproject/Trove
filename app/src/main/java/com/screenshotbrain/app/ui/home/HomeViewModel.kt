package com.trove.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trove.app.data.indexing.IndexingManager
import com.trove.app.data.local.db.dao.CategoryWithCount
import com.trove.app.data.repository.ScreenshotRepository
import com.trove.app.domain.model.IndexingProgress
import com.trove.app.domain.model.ScreenshotUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val recentScreenshots: List<ScreenshotUi> = emptyList(),
    val categories: List<CategoryWithCount> = emptyList(),
    val totalCount: Int = 0,
    val indexingProgress: IndexingProgress = IndexingProgress(0, 0, false),
    /** True until the first DB emission, so the screen shows a skeleton instead of a false "empty". */
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    screenshotRepository: ScreenshotRepository,
    indexingManager: IndexingManager,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        screenshotRepository.recentScreenshots(limit = 24),
        screenshotRepository.observeCategoriesWithCounts(),
        screenshotRepository.observeTotalCount(),
        indexingManager.progress,
    ) { recent, categories, total, progress ->
        HomeUiState(
            recentScreenshots = recent,
            categories = categories.filter { it.count > 0 },
            totalCount = total,
            indexingProgress = progress,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
