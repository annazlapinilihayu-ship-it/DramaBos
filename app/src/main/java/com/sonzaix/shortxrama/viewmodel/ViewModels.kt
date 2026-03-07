package com.sonzaix.shortxrama.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.sonzaix.shortxrama.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

open class PaginatedViewModel(private val fetcher: suspend (Int, String) -> Flow<Result<DramaListContainer?>>) : ViewModel() {
    protected val _items = MutableStateFlow<List<DramaItem>>(emptyList())
    val items = _items.asStateFlow()
    protected val _uiState = MutableStateFlow<UiState<Boolean>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _source = MutableStateFlow("melolo")
    val source = _source.asStateFlow()
    protected var currentPage = 1
    protected var isLastPage = false
    protected var isLoading = false

    init { loadNextPage() }

    open fun refresh() {
        viewModelScope.launch {
            currentPage = 1
            isLastPage = false
            isLoading = false
            _items.value = emptyList()
            _uiState.value = UiState.Loading
            loadNextPage()
        }
    }

    fun setSource(newSource: String) {
        if (newSource == _source.value) return
        _source.value = newSource
        refresh()
    }

    open fun loadNextPage() {
        if (isLoading || isLastPage) return
        viewModelScope.launch {
            isLoading = true
            if (_items.value.isEmpty()) _uiState.value = UiState.Loading

            fetcher(currentPage, _source.value).collect { result ->
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

class ForYouViewModel : PaginatedViewModel({ page, source -> DramaRepository.getHome(page, source) })

// For NewViewModel, use getNew for Dramabox and search with result=100 for Melolo to get new items
class NewViewModel : PaginatedViewModel({ page, source ->
    if (source == "dramabox" || source == "reelshort" || source == "freereels" || source == "netshort" || source == "meloshort" || source == "goodshort" || source == "dramawave") {
        DramaRepository.getNew(page, source)
    } else {
        DramaRepository.search("a", page, result = 100, source = source).map { result ->
            result.map { container ->
                val filtered = container.list.filter { it.isNew }
                container.copy(list = filtered, total = filtered.size)
            }
        }
    }
})

class RankViewModel : PaginatedViewModel({ page, source -> DramaRepository.getPopuler(page, source) })

class MainViewModel : ViewModel() {
    private val _isMaintenance = MutableStateFlow(false)
    val isMaintenance = _isMaintenance.asStateFlow()
}

class SearchViewModel : ViewModel() {
    private val repo = DramaRepository
    private val _queryText = MutableStateFlow("")
    val queryText = _queryText.asStateFlow()
    private val _source = MutableStateFlow("melolo")
    val source = _source.asStateFlow()



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

    fun setSource(newSource: String) {
        if (newSource == _source.value) return
        _source.value = newSource
        if (currentQuery.isNotEmpty()) {
            performSearch(currentQuery)
        } else {
            _searchResult.value = emptyList()
            _searchState.value = UiState.Idle
        }
    }

    fun onQueryChange(q: String) {
        _queryText.value = q
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
            repo.search(currentQuery, currentPage, source = _source.value).collect { r ->
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

    fun loadDetail(
        id: String,
        source: String = "melolo",
        bookName: String = "",
        cover: String = "",
        intro: String = ""
    ) {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            DramaRepository.getDetail(id, source, bookName, cover, intro).collect { result ->
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

    fun loadVideo(
        id: String,
        idx: Int,
        name: String,
        source: String = "melolo",
        introduction: String? = null,
        preferredQuality: Int = 720,
        dramaCover: String? = null
    ) {
        viewModelScope.launch {
            _videoState.value = UiState.Loading
            DramaRepository.getVideo(id, idx, name, source, preferredQuality).collect { r ->
                r.onSuccess {
                    _videoState.value = UiState.Success(it)
                    saveHistory(id, name, idx, dramaCover ?: it.cover, source, introduction)
                }.onFailure {
                    _videoState.value = UiState.Error(it.message?:"Gagal")
                }
            }
        }
    }

    private fun saveHistory(id: String, name: String, idx: Int, cover: String?, source: String, introduction: String? = null) {
        if (name.isNotEmpty()) {
            viewModelScope.launch {
                DramaDataStore(getApplication()).addToHistory(
                    LastWatched(id, name, idx, cover, System.currentTimeMillis(), source, 0L, introduction)
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
