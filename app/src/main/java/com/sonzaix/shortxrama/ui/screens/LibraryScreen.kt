package com.sonzaix.shortxrama.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sonzaix.shortxrama.ui.components.*
import com.sonzaix.shortxrama.ui.theme.*
import com.sonzaix.shortxrama.viewmodel.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun LibraryScreen(
    navController: NavController,
    historyVM: HistoryViewModel = viewModel(),
    favoriteVM: FavoriteViewModel = viewModel()
) {
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    val favoritesList by favoriteVM.favoritesList.collectAsState(initial = emptyList())
    val coverLookup = remember(historyList) { historyList.associateBy({ it.bookId }, { it.cover }) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        if (selectedIds.isEmpty()) isSelectionMode = false
    }

    fun deleteSelected() {
        if (selectedTab == 0) {
            historyVM.removeItems(selectedIds.toList())
        } else {
            favoriteVM.removeItems(selectedIds.toList())
        }
        isSelectionMode = false
        selectedIds.clear()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header with subtle accent gradient
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    IconButton(onClick = { isSelectionMode = false; selectedIds.clear() }) {
                        Icon(Icons.Filled.Close, null, tint = TextWhite, modifier = Modifier.size(24.dp))
                    }
                    Text("${selectedIds.size} Dipilih", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), fontSize = 16.sp)
                    TextButton(onClick = {
                        val allIds = if (selectedTab == 0) historyList.map { it.bookId } else favoritesList.map { it.bookId }
                        selectedIds.clear()
                        selectedIds.addAll(allIds)
                    }) { Text("Pilih Semua", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                } else {
                    Text("Pustaka Saya", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextWhite, modifier = Modifier.weight(1f), fontSize = 26.sp)
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            if (!isSelectionMode) {
                Spacer(Modifier.height(12.dp))
                // Animated sliding tab indicator
                val tabs = listOf("Riwayat", "Favorit")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    val tabWidth = maxWidth / tabs.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * selectedTab,
                        animationSpec = tween(durationMillis = 300),
                        label = "tabIndicator"
                    )
                    // Sliding pill indicator
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    // Tab labels on top
                    Row(Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedTab = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    color = if (selectedTab == index) Color.White else TextGray,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            if (selectedTab == 0) {
                if (historyList.isNotEmpty()) {
                    items(historyList) { item ->
                        SelectableHistoryItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(item.bookId),
                            onLongClick = {
                                isSelectionMode = true
                                toggleSelection(item.bookId)
                            },
                            onClick = {
                                if (isSelectionMode) toggleSelection(item.bookId)
                                else {
                                    val encodedName = URLEncoder.encode(item.bookName, StandardCharsets.UTF_8.toString())
                                    val encodedCover = URLEncoder.encode(item.cover ?: "", StandardCharsets.UTF_8.toString())
                                    val encodedSource = URLEncoder.encode(item.source, StandardCharsets.UTF_8.toString())
                                    navController.navigate("detail/${item.bookId}?bookName=$encodedName&cover=$encodedCover&intro=&source=$encodedSource")
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                } else {
                    item {
                        EmptyStateEnhanced(
                            icon = Icons.Filled.History,
                            title = "Belum ada riwayat",
                            subtitle = "Drama yang kamu tonton akan muncul di sini"
                        )
                    }
                }
            } else {
                if (favoritesList.isNotEmpty()) {
                    items(favoritesList) { item ->
                        val displayItem = if (item.cover.isNullOrEmpty()) {
                            item.copy(cover = coverLookup[item.bookId])
                        } else {
                            item
                        }
                        SelectableFavoriteItem(
                            item = displayItem,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(item.bookId),
                            onLongClick = {
                                isSelectionMode = true
                                toggleSelection(item.bookId)
                            },
                            onClick = {
                                if (isSelectionMode) toggleSelection(item.bookId)
                                else {
                                    val encodedName = URLEncoder.encode(item.bookName, StandardCharsets.UTF_8.toString())
                                    val encodedCover = URLEncoder.encode(item.cover ?: "", StandardCharsets.UTF_8.toString())
                                    val encodedSource = URLEncoder.encode(item.source, StandardCharsets.UTF_8.toString())
                                    navController.navigate("detail/${item.bookId}?bookName=$encodedName&cover=$encodedCover&intro=&source=$encodedSource")
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                } else {
                    item {
                        EmptyStateEnhanced(
                            icon = Icons.Filled.FavoriteBorder,
                            title = "Belum ada favorit",
                            subtitle = "Tandai drama kesukaanmu dengan ♥"
                        )
                    }
                }
            }
        }
    }

    if (isSelectionMode) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Button(
                onClick = { deleteSelected() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Delete, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Hapus (${selectedIds.size})", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
