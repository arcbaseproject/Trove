package com.trove.app.ui.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.trove.app.data.repository.ScreenshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    screenshotRepository: ScreenshotRepository,
) : ViewModel() {

    val collectionId: Long = checkNotNull(savedStateHandle.get<String>("collectionId")).toLong()
    val collectionName: String = java.net.URLDecoder.decode(
        checkNotNull(savedStateHandle.get<String>("name")),
        "UTF-8",
    )

    val pagingData = screenshotRepository.pagedScreenshotsByCollection(collectionId).cachedIn(viewModelScope)
}
