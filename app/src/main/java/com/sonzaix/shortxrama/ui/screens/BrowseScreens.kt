package com.sonzaix.shortxrama.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sonzaix.shortxrama.data.LastWatched
import com.sonzaix.shortxrama.data.AppSettingsStore
import com.sonzaix.shortxrama.data.AppSettings
import com.sonzaix.shortxrama.ui.components.*
import com.sonzaix.shortxrama.ui.theme.*
import com.sonzaix.shortxrama.ui.util.*
import com.sonzaix.shortxrama.viewmodel.*
import androidx.compose.ui.platform.LocalContext

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ForYouScreen(nav: NavController, vm: ForYouViewModel = viewModel(), historyVM: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    val rawItems by vm.items.collectAsState()
    val state by vm.uiState.collectAsState()
    val source by vm.source.collectAsState()
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    val watchedIds = remember(historyList) { historyList.map { it.bookId }.toSet() }
    val items = if (settings.hideWatchedDramas) rawItems.filter { it.bookId !in watchedIds } else rawItems
    val listState = rememberLazyGridState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val refreshing = state is UiState.Loading && items.isNotEmpty()
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = { vm.refresh() })
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) vm.loadNextPage() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            if (state is UiState.Loading && items.isEmpty()) {
                ShimmerLoadingGrid()
            } else if (state is UiState.Error && items.isEmpty()) {
                EmptyState(getFriendlyErrorMessage((state as UiState.Error).message))
            } else {
                val carouselItems = items.take(5)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                ) {
                    if (carouselItems.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            HeroCarousel(featuredItems = items.take(5)) { item ->
                                playSmart(nav, item, historyList, historyVM)
                            }
                        }
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = "Untuk Kamu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            )
                        }
                    }
                    items(items) { item ->
                        DramaCard(item) {
                            playSmart(nav, item, historyList, historyVM)
                        }
                    }
                    if (state is UiState.Loading) {
                        item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) } }
                    }
                }
            }
            PullRefreshIndicator(refreshing = refreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), contentColor = MaterialTheme.colorScheme.primary, backgroundColor = MaterialTheme.colorScheme.surface)
        }
        SourceFilterFab(
            selectedSource = source,
            onSelect = vm::setSource,
            showLabel = true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navBarBottom + 20.dp)
        )
    }
}

@Composable
fun NewScreen(nav: NavController, vm: NewViewModel = viewModel(), historyVM: HistoryViewModel = viewModel()) {
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    PaginatedGrid(nav, vm, historyList, historyVM, "Terbaru")
}

@Composable
fun RankScreen(nav: NavController, vm: RankViewModel = viewModel(), historyVM: HistoryViewModel = viewModel()) {
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    PaginatedGrid(nav, vm, historyList, historyVM, "Populer")
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun PaginatedGrid(
    navController: NavController,
    vm: PaginatedViewModel,
    historyList: List<LastWatched>,
    historyVM: HistoryViewModel,
    title: String
) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    val rawItems by vm.items.collectAsState()
    val watchedIds = remember(historyList) { historyList.map { it.bookId }.toSet() }
    val items = if (settings.hideWatchedDramas) rawItems.filter { it.bookId !in watchedIds } else rawItems
    val state by vm.uiState.collectAsState()
    val source by vm.source.collectAsState()
    val listState = rememberLazyGridState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val refreshing = state is UiState.Loading && items.isNotEmpty()
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = { vm.refresh() })

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) vm.loadNextPage() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            if (state is UiState.Loading && items.isEmpty()) {
                ShimmerLoadingGrid()
            } else if (state is UiState.Error && items.isEmpty()) {
                EmptyState(getFriendlyErrorMessage((state as UiState.Error).message))
            } else {
                val carouselItems = items.take(5)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                ) {
                    if (carouselItems.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            HeroCarousel(
                                featuredItems = items.take(5)
                            ) { item ->
                                playSmart(navController, item, historyList, historyVM)
                            }
                        }

                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            )
                        }
                    }

                    items(items) { item ->
                        DramaCard(item) {
                            playSmart(navController, item, historyList, historyVM)
                        }
                    }
                    if (state is UiState.Loading) {
                        item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) } }
                    }
                }
            }
            PullRefreshIndicator(refreshing = refreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), contentColor = MaterialTheme.colorScheme.primary, backgroundColor = MaterialTheme.colorScheme.surface)
        }
        SourceFilterFab(
            selectedSource = source,
            onSelect = vm::setSource,
            showLabel = true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navBarBottom + 20.dp)
        )
    }
}
