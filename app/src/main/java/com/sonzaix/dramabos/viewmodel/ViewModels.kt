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
                        isLastPage = false
                        currentPage++
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

class ForYouViewModel : ViewModel() {
    private val repo = DramaRepository()
    private val _items = MutableStateFlow<List<DramaItem>>(emptyList())
    val items = _items.asStateFlow()
    private val _uiState = MutableStateFlow<UiState<Boolean>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _currentSource = MutableStateFlow("all")
    val currentSource = _currentSource.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false
    private var isLoading = false

    init {
        loadNextPage()
    }

    fun setSourceFilter(source: String) {
        if (_currentSource.value != source) {
            _currentSource.value = source
            currentPage = 1
            _items.value = emptyList()
            isLastPage = false
            _uiState.value = UiState.Loading
            loadNextPage()
        }
    }

    fun loadNextPage() {
        if (isLoading || isLastPage) return
        viewModelScope.launch {
            isLoading = true
            if (_items.value.isEmpty()) _uiState.value = UiState.Loading

            repo.getForYouCombined(currentPage, _currentSource.value).collect { result ->
                result.onSuccess { res ->
                    val newItems = res?.list ?: emptyList()
                    if (newItems.isNotEmpty()) {
                        _items.value += newItems
                        isLastPage = false
                        currentPage++
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

class NewViewModel : PaginatedViewModel({ page -> DramaRepository().getNew(page) })

class RankViewModel : PaginatedViewModel({ page -> DramaRepository().getRank(page) })

class MainViewModel : ViewModel() {
    private val _isDramaboxDown = MutableStateFlow(false)
    val isDramaboxDown = _isDramaboxDown.asStateFlow()

    private val _isMeloloDown = MutableStateFlow(false)
    val isMeloloDown = _isMeloloDown.asStateFlow()

    private val _isMaintenance = MutableStateFlow(false)
    val isMaintenance = _isMaintenance.asStateFlow()

    init {
        checkApiStatus()
        checkMaintenanceMode()
    }

    fun checkMaintenanceMode() {
        viewModelScope.launch {
            DramaRepository().checkAppMaintenance().collect { result ->
                result.onSuccess {
                    _isMaintenance.value = it.isMT.equals("yes", ignoreCase = true)
                }.onFailure {
                    _isMaintenance.value = false
                }
            }
        }
    }

    fun checkApiStatus() {
        viewModelScope.launch {
            val repo = DramaRepository()

            launch {
                repo.checkMonitorStatus().collect { result ->
                    result.onSuccess { response ->
                        _isDramaboxDown.value = response.status != "online"
                    }.onFailure {
                        _isDramaboxDown.value = true
                    }
                }
            }

            launch {
                repo.checkMeloloStatus().collect { result ->
                    result.onSuccess { response ->
                        _isMeloloDown.value = response.status != "ok"
                    }.onFailure {
                        _isMeloloDown.value = true
                    }
                }
            }
        }
    }
}

class SearchViewModel : ViewModel() {
    private val repo = DramaRepository()
    private val _queryText = MutableStateFlow("")
    val queryText = _queryText.asStateFlow()

    private val _filterState = MutableStateFlow("all") 
    val filterState = _filterState.asStateFlow()

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

    fun setFilter(filter: String) {
        if (_filterState.value != filter) {
            _filterState.value = filter
            if (currentQuery.isNotEmpty()) {
                performSearch(currentQuery)
            }
        }
    }

    fun onQueryChange(q: String) {
        _queryText.value = q
        searchJob?.cancel()
        if (q.length > 2) {
            searchJob = viewModelScope.launch {
                delay(800)
                repo.getSuggestions(q).collect { r ->
                    r.onSuccess { _suggestions.value = it ?: emptyList() }.onFailure { _suggestions.value = emptyList() }
                }
            }
        } else { _suggestions.value = emptyList() }
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

        val currentFilter = _filterState.value
        val useDramabox = currentFilter == "all" || currentFilter == "dramabox"
        val useMelolo = currentFilter == "all" || currentFilter == "melolo"

        isLoading = true
        viewModelScope.launch {
            repo.searchCombined(currentQuery, currentPage, useDramabox, useMelolo).collect { r ->
                r.onSuccess { res ->
                    isLoading = false
                    val newItems = res.list
                    if (newItems.isNotEmpty()) {
                        _searchResult.value += newItems
                        currentPage++
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

    fun loadDetail(id: String, source: String = "dramabox") {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            DramaRepository().getDetail(id, source).collect { result ->
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

    fun loadVideo(id: String, idx: Int, name: String, source: String = "dramabox") {
        viewModelScope.launch {
            _videoState.value = UiState.Loading
            DramaRepository().getVideo(id, idx, source).collect { r ->
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