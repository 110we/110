package com.crawler.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawler.data.history.CrawlHistoryEntity
import com.crawler.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _histories = MutableStateFlow<List<CrawlHistoryEntity>>(emptyList())
    val histories: StateFlow<List<CrawlHistoryEntity>> = _histories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistories()
    }

    fun loadHistories() {
        viewModelScope.launch {
            _isLoading.value = true
            historyRepository.getAll().collect { list ->
                _histories.value = list
                _isLoading.value = false
            }
        }
    }

    fun deleteById(historyId: String) {
        viewModelScope.launch {
            historyRepository.deleteById(historyId)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            historyRepository.deleteAll()
            _histories.value = emptyList()
        }
    }
}
