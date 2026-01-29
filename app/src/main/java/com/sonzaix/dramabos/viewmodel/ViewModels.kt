package com.sonzaix.dramabos.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.sonzaix.dramabos.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

open class PaginatedViewModel(private val fetcher: suspend (Int) -> Flow<Result<DramaListContainer?>>) : ViewModel() {
    protected val _items = MutableStateFlow<List<DramaItem>>(emptyList())
    val items = _items.asStateFlow()
    protected val _uiState = MutableStateFlow<UiState<Boolean>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    protected var currentPage = 1
    protected var isLastPage = false
    protected var isLoading = false

    init { loadNextPage() }

    open fun loadNextPage() {
        if (isLoading || isLastPage) return
        viewModelScope.launch {
            isLoading = true
            if (_items.value.isEmpty()) _uiState.value = UiState.Loading

            fetcher(currentPage).collect { result ->
                result.onSuccess { res ->
                    val newItems = res?.list ?: emptyList()
                    if (newItems.isNotEmpty()) {
                        _items.value += newItems
                        isLastPage = !res!!.isMore // Update isLastPage based on isMore flag from repository
                        if (!isLastPage) currentPage++
                        _uiState.value = UiState.Success(true)
                    } else {
                        isLastPage = true
                        if (_items.value.isEmpty()) _uiState.value = UiState.Error("Data kosong")
                    }
                }.onFailure {
                    if (_items.value.isEmpty()) _uiState.value = UiState.Error(it.message ?: "Error")
                }
                isLoading = false
            }
        }
    }
}

class ForYouViewModel : PaginatedViewModel({ page -> DramaRepository().getHome() })

// For NewViewModel, we use getHome() and filter for new items.
class NewViewModel : PaginatedViewModel({ page ->
    DramaRepository().getHome().map { result ->
        result.map { container ->
            // Filter only new items
            val filtered = container.list.filter { it.isNew }
            container.copy(list = filtered, total = filtered.size)
        }
    }
})

class RankViewModel : PaginatedViewModel({ page -> DramaRepository().getPopuler() })

class MainViewModel : ViewModel() {
    // Removed Dramabox specific checks.
    private val _isMaintenance = MutableStateFlow(false)
    val isMaintenance = _isMaintenance.asStateFlow()

    // We can add a generic connectivity check here if needed, but for now we keep it simple.
}

class SearchViewModel : ViewModel() {
    private val repo = DramaRepository()
    private val _queryText = MutableStateFlow("")
    val queryText = _queryText.asStateFlow()

    // Removed filterState as we only have Melolo now.

    private val _suggestions = MutableStateFlow<List<DramaItem>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    private val _searchResult = MutableStateFlow<List<DramaItem>>(emptyList())
    val searchResult = _searchResult.asStateFlow()
    private val _searchState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val searchState = _searchState.asStateFlow()

    private var searchJob: Job? = null
    private var currentQuery = ""
    private var currentPage = 1
    private var isLastPage = false
    private var isLoading = false

    // Removed setFilter

    fun onQueryChange(q: String) {
        _queryText.value = q
        // Removed suggestion logic as API doesn't support specific suggestion endpoint,
        // and using search for suggestions might be too heavy or duplicate logic.
        // We can just clear suggestions.
        _suggestions.value = emptyList()
    }

    fun performSearch(q: String) {
        if (q.isEmpty()) return
        _queryText.value = q
        currentQuery = q
        currentPage = 1
        isLastPage = false
        isLoading = false
        _searchResult.value = emptyList()
        _searchState.value = UiState.Loading
        _suggestions.value = emptyList()
        loadMoreSearch()
    }

    fun loadMoreSearch() {
        if (isLastPage || currentQuery.isEmpty() || isLoading) return

        isLoading = true
        viewModelScope.launch {
            repo.search(currentQuery, currentPage).collect { r ->
                r.onSuccess { res ->
                    isLoading = false
                    val newItems = res.list
                    if (newItems.isNotEmpty()) {
                        _searchResult.value += newItems
                        if (res.isMore) currentPage++ else isLastPage = true
                        _searchState.value = UiState.Success(true)
                    } else { isLastPage = true }
                }.onFailure {
                    isLoading = false
                    if (_searchResult.value.isEmpty()) {
                        _searchState.value = UiState.Error(it.message ?: "Gagal mencari")
                    }
                }
            }
        }
    }
}

class DetailViewModel : ViewModel() {
    private val _detailState = MutableStateFlow<UiState<DramaDetail>>(UiState.Loading)
    val detailState = _detailState.asStateFlow()

    fun loadDetail(id: String, source: String = "melolo") {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            DramaRepository().getDetail(id).collect { result ->
                result.onSuccess {
                    _detailState.value = UiState.Success(it)
                }.onFailure {
                    _detailState.value = UiState.Error(it.message ?: "Gagal memuat detail")
                }
            }
        }
    }
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val _videoState = MutableStateFlow<UiState<VideoData>>(UiState.Loading)
    val videoState = _videoState.asStateFlow()

    fun loadVideo(id: String, idx: Int, name: String, source: String = "melolo") {
        viewModelScope.launch {
            _videoState.value = UiState.Loading
            DramaRepository().getVideo(id, idx, name).collect { r ->
                r.onSuccess {
                    _videoState.value = UiState.Success(it)
                    saveHistory(id, name, idx, it.cover, source)
                }.onFailure {
                    _videoState.value = UiState.Error(it.message?:"Gagal")
                }
            }
        }
    }

    private fun saveHistory(id: String, name: String, idx: Int, cover: String?, source: String) {
        if (name.isNotEmpty()) {
            viewModelScope.launch {
                DramaDataStore(getApplication()).addToHistory(
                    LastWatched(id, name, idx, cover, System.currentTimeMillis(), source, 0L)
                )
            }
        }
    }
}

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    val historyList = DramaDataStore(app).historyListFlow

    fun saveToHistory(item: LastWatched) {
        viewModelScope.launch {
            DramaDataStore(getApplication()).addToHistory(item)
        }
    }

    fun removeItems(ids: List<String>) {
        viewModelScope.launch {
            DramaDataStore(getApplication()).removeHistoryItems(ids)
        }
    }
}

class FavoriteViewModel(app: Application) : AndroidViewModel(app) {
    val favoritesList = DramaDataStore(app).favoritesListFlow

    fun toggleFavorite(item: FavoriteDrama) {
        viewModelScope.launch {
            DramaDataStore(getApplication()).toggleFavorite(item)
        }
    }

    fun removeItems(ids: List<String>) {
        viewModelScope.launch {
            DramaDataStore(getApplication()).removeFavoriteItems(ids)
        }
    }
}
