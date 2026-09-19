package com.trove.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trove.app.data.repository.CollectionRepository
import com.trove.app.domain.model.CollectionUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    /** null until the first emission: lets the screen show a skeleton instead of a false "no collections". */
    val collections: StateFlow<List<CollectionUi>?> = collectionRepository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { collectionRepository.createCollection(name.trim()) }
    }

    fun deleteCollection(id: Long) = viewModelScope.launch {
        collectionRepository.deleteCollection(id)
    }
}
