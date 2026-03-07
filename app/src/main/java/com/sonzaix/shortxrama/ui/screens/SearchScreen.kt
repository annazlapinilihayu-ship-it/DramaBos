package com.sonzaix.shortxrama.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sonzaix.shortxrama.ui.components.*
import com.sonzaix.shortxrama.ui.theme.*
import com.sonzaix.shortxrama.ui.util.playSmart
import com.sonzaix.shortxrama.data.AppSettingsStore
import com.sonzaix.shortxrama.data.AppSettings
import com.sonzaix.shortxrama.viewmodel.*
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController, vm: SearchViewModel = viewModel(), historyVM: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    val searchText by vm.queryText.collectAsState()
    val rawResults by vm.searchResult.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val searchState by vm.searchState.collectAsState()
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    val watchedIds = remember(historyList) { historyList.map { it.bookId }.toSet() }
    val results = if (settings.hideWatchedDramas) rawResults.filter { it.bookId !in watchedIds } else rawResults
    val source by vm.source.collectAsState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val listState = rememberLazyGridState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) vm.loadMoreSearch() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
                    border = BorderStroke(1.dp, Color.White.copy(0.12f))
                ) {
                    TextField(
                        value = searchText,
                        onValueChange = { vm.onQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari drama favorit...", color = TextGray) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { if(searchText.isNotEmpty()) Icon(Icons.Filled.Close, null, Modifier.clickable{ vm.onQueryChange("") }, tint= TextWhite) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardActions = KeyboardActions(onSearch = { vm.performSearch(searchText) }),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                }
            }

            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))

            if (searchState is UiState.Error) {
                Text(getFriendlyErrorMessage((searchState as UiState.Error).message), color = Color.Red, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            if (results.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { item ->
                        DramaCard(item) { playSmart(nav, item, historyList, historyVM) }
                    }
                    if (searchState is UiState.Loading) {
                        item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) } }
                    }
                }
            } else if (suggestions.isNotEmpty() && searchText.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(16.dp)),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    LazyColumn {
                        items(suggestions) { item ->
                            Row(Modifier.fillMaxWidth()
                                .clickable {
                                    vm.performSearch(item.bookName)
                                }
                                .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp))
                                Text(item.bookName, color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            } else if (searchState is UiState.Loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            }
            }
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
