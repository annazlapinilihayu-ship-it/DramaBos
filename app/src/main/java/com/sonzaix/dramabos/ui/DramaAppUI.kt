package com.sonzaix.dramabos.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonzaix.dramabos.R
import com.sonzaix.dramabos.data.*
import com.sonzaix.dramabos.viewmodel.*
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private val DarkBackground = Color(0xFF0F0F0F)
private val CardBackground = Color(0xFF1E1E1E)
private val PrimaryColor = Color(0xFFFF2965)
private val MeloloColor = Color(0xFFFFD700)
private val TextWhite = Color(0xFFF5F5F5)
private val TextGray = Color(0xFFB0B0B0)
private val TextDarkGray = Color(0xFF808080)
private val WatchedColor = Color(0xFF333333)
private val SearchBarColor = Color(0xFF252525)
private val BadgeBackground = Color.Black.copy(alpha = 0.7f)

fun getFriendlyErrorMessage(msg: String): String {
    return if (msg.contains("http") || msg.contains("UnknownHost") || msg.contains("Connect") || msg.contains("html")) {
        "Gagal memuat data. Periksa koneksi internet Anda."
    } else {
        "Terjadi kesalahan. Silakan coba lagi."
    }
}

@Composable
fun DramaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PrimaryColor,
            background = DarkBackground,
            surface = CardBackground,
            onBackground = TextWhite,
            onSurface = TextWhite
        ),
        content = content
    )
}

fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun playSmart(navController: NavController, item: DramaItem, historyList: List<LastWatched>, vm: HistoryViewModel) {
    val historyItem = historyList.find { it.bookId == item.bookId }
    val targetIndex = historyItem?.chapterIndex ?: 0
    val targetPos = historyItem?.position ?: 0L
    vm.saveToHistory(LastWatched(item.bookId, item.bookName, targetIndex, item.cover, System.currentTimeMillis(), item.source, targetPos))
    navController.navigate("detail/${item.bookId}")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    featuredItems: List<DramaItem>,
    onClick: (DramaItem) -> Unit
) {
    if (featuredItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featuredItems.size })

    Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            pageSpacing = 12.dp,
            flingBehavior = PagerDefaults.flingBehavior(pagerState)
        ) { page ->
            val item = featuredItems[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onClick(item) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.bookName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.8f),
                                    DarkBackground
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    val sourceColor = MeloloColor
                    val sourceName = "MELOLO"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sourceName,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(sourceColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val count = item.chapterCount ?: 0
                        if (count > 0) {
                            Text(
                                text = "•  $count Episode",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = item.bookName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.introduction.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.introduction,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { onClick(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tonton Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val width = if (isSelected) 24.dp else 8.dp
                val color = if (isSelected) PrimaryColor else Color.Gray.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .height(6.dp)
                        .width(width)
                )
            }
        }
    }
}

@Composable
fun DramaApp(
    mainVM: MainViewModel = viewModel()
) {
    DramaTheme {
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(1400)
            showSplash = false
        }

        if (showSplash) {
            SplashScreen()
            return@DramaTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in listOf("foryou", "new", "rank", "search", "library")
        val context = LocalContext.current

        val isMaintenance by mainVM.isMaintenance.collectAsState()

        if (isMaintenance) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = PrimaryColor)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "MAINTENANCE", fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                },
                text = { Text(text = "Aplikasi sedang dalam perbaikan (Maintenance) atau Server Down. Mohon kembali lagi nanti.", color = TextGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://t.me/November2k".toUri())
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("Beritahu Developer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { (context as? Activity)?.finish() }) {
                        Text("Close / Keluar", color = TextWhite)
                    }
                },
                containerColor = CardBackground,
                properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            )
        }

        Scaffold(
            bottomBar = { if (showBottomBar) BottomNavBar(navController) },
            containerColor = DarkBackground
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "foryou",
                modifier = Modifier.padding(padding)
            ) {
                composable("foryou") { ForYouScreen(navController) }
                composable("new") { NewScreen(navController) }
                composable("rank") { RankScreen(navController) }
                composable("search") { SearchScreen(navController) }
                composable("library") { LibraryScreen(navController) }

                composable(
                    route = "detail/{bookId}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    DetailPlayerScreen(navController, bookId, "melolo") // Default to melolo
                }

                composable(
                    route = "player_full/{bookId}/{index}/{bookName}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType },
                        navArgument("index") { type = NavType.IntType },
                        navArgument("bookName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    val index = backStackEntry.arguments?.getInt("index") ?: 0
                    val bookName = backStackEntry.arguments?.getString("bookName") ?: ""
                    PlayerScreen(navController, bookId, index, bookName, "melolo") // Default to melolo
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splashTransition")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color.Black)
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(PrimaryColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(300f, 200f),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .shadow(20.dp, CircleShape)
                    .graphicsLayer(scaleX = pulse, scaleY = pulse),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PrimaryColor.copy(alpha = 0.3f), Color.Transparent),
                                center = Offset(75f, 75f),
                                radius = 220f
                            ),
                            CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "DramaBos Logo",
                    modifier = Modifier.size(110.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "DramaBos",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Nonton drama China tanpa batas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(999.dp)),
                color = PrimaryColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun ForYouScreen(nav: NavController, vm: ForYouViewModel = viewModel(), historyVM: HistoryViewModel = viewModel()) {
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    val items by vm.items.collectAsState()
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) vm.loadNextPage() }

    Box(Modifier.fillMaxSize()) {
        if (state is UiState.Loading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryColor) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (items.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        HeroCarousel(
                            featuredItems = items.take(5)
                        ) { item ->
                            playSmart(nav, item, historyList, historyVM)
                        }
                    }

                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "Rekomendasi Untukmu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                }

                items(items) { item ->
                    DramaCard(item) {
                        playSmart(nav, item, historyList, historyVM)
                    }
                }

                if (state is UiState.Loading) {
                    item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = PrimaryColor) } }
                }
            }
        }
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
    PaginatedGrid(nav, vm, historyList, historyVM, "Peringkat Teratas")
}

@Composable
fun PaginatedGrid(
    navController: NavController,
    vm: PaginatedViewModel,
    historyList: List<LastWatched>,
    historyVM: HistoryViewModel,
    title: String
) {
    val items by vm.items.collectAsState()
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) vm.loadNextPage() }

    if (state is UiState.Loading && items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryColor) }
    } else if (state is UiState.Error && items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(getFriendlyErrorMessage((state as UiState.Error).message), color = TextGray)
        }
    } else {
        val carouselItems = items.take(5)
        val gridItems = if(items.size > 5) items.drop(5) else emptyList()

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = listState,
            contentPadding = PaddingValues(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                }
            }

            items(gridItems) { item ->
                DramaCard(item) {
                    playSmart(navController, item, historyList, historyVM)
                }
            }
            if (state is UiState.Loading) {
                item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = PrimaryColor) } }
            }
        }
    }
}

@Composable
fun DramaCard(drama: DramaItem, onClick: () -> Unit) {
    val sourceColor = MeloloColor
    val sourceName = "Melolo"
    val epCount = drama.chapterCount ?: 0
    val playCount = drama.playCount ?: ""

    Column(Modifier.clickable { onClick() }) {
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, Color.White.copy(0.06f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(drama.cover).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(0.92f))
                            )
                        )
                )

                val showEpBadge = epCount > 0
                val showPlayBadge = !showEpBadge && playCount.isNotEmpty()

                if (showEpBadge || showPlayBadge) {
                    Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(BadgeBackground, RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showPlayBadge) {
                                Icon(Icons.Rounded.Visibility, null, tint = TextWhite, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = if (showEpBadge) "$epCount Eps" else playCount,
                                color = TextWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(BadgeBackground, RoundedCornerShape(6.dp))
                        .border(0.5.dp, sourceColor.copy(0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = sourceName,
                        color = sourceColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = drama.bookName,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController, vm: SearchViewModel = viewModel(), historyVM: HistoryViewModel = viewModel()) {
    val searchText by vm.queryText.collectAsState()
    val results by vm.searchResult.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val searchState by vm.searchState.collectAsState()
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())
    // Removed filter logic

    val listState = rememberLazyGridState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) vm.loadMoreSearch() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SearchBarColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { vm.onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari judul drama...", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray) },
                    trailingIcon = { if(searchText.isNotEmpty()) Icon(Icons.Default.Close, null, Modifier.clickable{ vm.onQueryChange("") }, tint=TextWhite) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = PrimaryColor
                    ),
                    keyboardActions = KeyboardActions(onSearch = { vm.performSearch(searchText) }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
            Spacer(Modifier.height(16.dp))

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
                        item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = PrimaryColor) } }
                    }
                }
            } else if (suggestions.isNotEmpty() && searchText.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn {
                        items(suggestions) { item ->
                            Row(Modifier.fillMaxWidth()
                                .clickable {
                                    vm.performSearch(item.bookName)
                                }
                                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = TextGray, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(12.dp))
                                Text(item.bookName, color = TextWhite)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            } else if (searchState is UiState.Loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryColor) }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DetailPlayerScreen(
    nav: NavController,
    bookId: String,
    source: String, // Kept but ignored/defaulted
    detailVM: DetailViewModel = viewModel(),
    playerVM: PlayerViewModel = viewModel(),
    historyVM: HistoryViewModel = viewModel(),
    favoriteVM: FavoriteViewModel = viewModel()
) {
    val detailState by detailVM.detailState.collectAsState()
    val videoState by playerVM.videoState.collectAsState()
    val historyListState = historyVM.historyList.collectAsState(initial = null)
    val historyList = historyListState.value
    val favoritesList by favoriteVM.favoritesList.collectAsState(initial = emptyList())
    val isFavorite = favoritesList.any { it.bookId == bookId }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("video_quality_prefs", Context.MODE_PRIVATE) }
    var currentEpIndex by remember { mutableIntStateOf(0) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var initialLoadDone by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf(false) }
    var currentQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var videoData by remember { mutableStateOf<VideoData?>(null) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var totalTime by remember { mutableLongStateOf(0L) }
    var initialSeekPosition by remember { mutableLongStateOf(0L) }
    var reloadToken by remember { mutableIntStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = true
                    isPlayerReady = false
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        playerError = false
                        isPlayerReady = true
                    }
                }
            })
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (detailState is UiState.Success && isPlayerReady) {
                    val d = (detailState as UiState.Success).data
                    val pos = exoPlayer.currentPosition
                    historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, pos))
                }
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(exoPlayer) {
        while(true) {
            currentTime = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalTime = exoPlayer.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    LaunchedEffect(bookId, source) {
        currentEpIndex = 0
        initialSeekPosition = 0L
        initialLoadDone = false
        isPlayerReady = false
        playerError = false
        currentQuality = null
        videoData = null
        detailVM.loadDetail(bookId, source)
    }

    LaunchedEffect(historyList) {
        if (historyList != null) {
            val latestHistory = historyList.find { it.bookId == bookId }
            if (latestHistory != null && latestHistory.chapterIndex != currentEpIndex) {
                currentEpIndex = latestHistory.chapterIndex
                if (isPlayerReady) { isPlayerReady = false; playerError = false; initialLoadDone = false }
            }
        }
    }

    LaunchedEffect(historyList, detailState) {
        if (!initialLoadDone && detailState is UiState.Success && historyList != null) {
            val d = (detailState as UiState.Success).data
            if (d.bookId == bookId) {
                val historyItem = historyList.find { it.bookId == bookId }
                val historyIndex = historyItem?.chapterIndex
                val fallbackIndex = d.chapterList.minByOrNull { it.chapterIndex }?.chapterIndex ?: 0
                val hasValidHistory = historyIndex != null && d.chapterList.any { it.chapterIndex == historyIndex }
                currentEpIndex = if (hasValidHistory) historyIndex else fallbackIndex
                initialSeekPosition = historyItem?.position ?: 0L
                initialLoadDone = true
            }
        }
    }

    LaunchedEffect(bookId, currentEpIndex, initialLoadDone, reloadToken) {
        if (initialLoadDone && detailState is UiState.Success) {
            val d = (detailState as UiState.Success).data
            if (d.bookId == bookId) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                playerError = false
                isPlayerReady = false
                currentQuality = null
                playerVM.loadVideo(bookId, currentEpIndex, d.bookName, source)
            }
        }
    }

    LaunchedEffect(videoState) {
        if (videoState is UiState.Success) {
            val data = (videoState as UiState.Success).data
            if (data.bookId != bookId || data.chapterIndex != currentEpIndex) return@LaunchedEffect
            videoData = data
            val availableQualities = data.qualities?.filter { it.videoPath.isNotEmpty() }.orEmpty()
            if (currentQuality == null || currentQuality?.videoPath.isNullOrEmpty()) {
                val freshSavedQuality = prefs.getInt("quality_$bookId", -1)
                val savedQ = if (freshSavedQuality != -1) availableQualities.minByOrNull { abs(it.quality - freshSavedQuality) } else null
                currentQuality = savedQ ?: (availableQualities.find { it.isDefault == 1 } ?: availableQualities.find { it.quality == 720 } ?: availableQualities.firstOrNull())
            }
            val fallbackQuality = availableQualities.firstOrNull()
            val resolvedQuality = currentQuality?.takeIf { !it.videoPath.isNullOrEmpty() } ?: fallbackQuality
            if (resolvedQuality != null) currentQuality = resolvedQuality
            val url = resolvedQuality?.videoPath?.takeIf { it.isNotEmpty() } ?: data.videoUrl
            if (url.isNotEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                if (initialSeekPosition > 0) exoPlayer.seekTo(initialSeekPosition)
                exoPlayer.prepare()
            } else {
                playerError = true
            }
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    fun goFullscreen() {
        if (detailState is UiState.Success) {
            val d = (detailState as UiState.Success).data
            val encodedName = URLEncoder.encode(d.bookName, StandardCharsets.UTF_8.toString())
            val pos = exoPlayer.currentPosition
            historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, pos))
            exoPlayer.pause()
            nav.navigate("player_full/$bookId/$currentEpIndex/$encodedName") // Removed source
        }
    }

    fun retryVideo() {
        playerError = false
        isPlayerReady = false
        val d = (detailState as? UiState.Success)?.data
        if (d != null) {
            playerVM.loadVideo(bookId, currentEpIndex, d.bookName, source)
        }
    }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black)) {
            if (playerError) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Refresh, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Gagal memuat video", color = Color.White)
                    Button(onClick = { retryVideo() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("Coba Lagi") }
                }
            } else if (isPlayerReady) {
                AndroidView(factory = { PlayerView(it).apply { player = exoPlayer; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; keepScreenOn = true } }, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { goFullscreen() }, modifier = Modifier.align(Alignment.Center).size(64.dp).background(Color.Black.copy(0.5f), CircleShape)) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                IconButton(onClick = { goFullscreen() }, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(0.5f), CircleShape)) { Icon(Icons.Rounded.Fullscreen, null, tint = Color.White) }
                Box(Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("${formatDuration(currentTime)} / ${formatDuration(totalTime)}", color = TextWhite, fontSize = 10.sp) }
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PrimaryColor) }
            }
            IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
        }

        if (historyList == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PrimaryColor) }
        } else {
            when (val s = detailState) {
                is UiState.Success -> {
                    val d = s.data
                    val watchedItem = historyList.find { it.bookId == bookId }
                    val lastWatchedIndex = watchedItem?.chapterIndex ?: -1

                    if (d.chapterList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Episode belum tersedia", color = TextGray) }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Column(Modifier.padding(16.dp)) {
                                    Text(d.bookName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val sourceColor = MeloloColor
                                        val sourceText = "MELOLO"
                                        Text(text = sourceText, style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.background(sourceColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                        Spacer(Modifier.width(8.dp)); Text("${d.chapterList.size} Episode", color = TextGray, fontSize = 12.sp)
                                        Spacer(Modifier.width(8.dp)); Text("•", color = TextGray)
                                        Spacer(Modifier.width(8.dp)); Text("Ongoing", color = TextGray, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { favoriteVM.toggleFavorite(FavoriteDrama(d.bookId, d.bookName, d.cover, d.source, d.chapterList.size)) }, colors = ButtonDefaults.buttonColors(containerColor = CardBackground), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                        Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFavorite) PrimaryColor else TextWhite)
                                        Spacer(Modifier.width(8.dp)); Text(if (isFavorite) "Disukai" else "Tambah ke Favorit", color = TextWhite)
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    d.tags?.let { tags -> if(tags.isNotEmpty()) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { tags.forEach { tag -> Box(modifier = Modifier.border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text(tag, color = TextGray, fontSize = 12.sp) } } }; Spacer(Modifier.height(16.dp)) } }
                                    Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Spacer(Modifier.height(4.dp)); Text(d.introduction ?: "", style = MaterialTheme.typography.bodyMedium, color = TextGray, lineHeight = 22.sp)
                                    Spacer(Modifier.height(24.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text("Daftar Episode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                                        Spacer(Modifier.weight(1f)); Text("${currentEpIndex + 1} / ${d.chapterList.size}", color = TextGray, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            items(d.chapterList) { chapter ->
                                val isSelected = chapter.chapterIndex == currentEpIndex
                                val isWatched = chapter.chapterIndex <= lastWatchedIndex && !isSelected
                                val bgColor = if (isSelected) PrimaryColor.copy(alpha = 0.15f) else CardBackground
                                val textColor = if (isSelected) PrimaryColor else TextWhite
                                val iconTint = if (isSelected) PrimaryColor else TextGray
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth().background(bgColor).clickable {
                                        if(isPlayerReady) { val pos = exoPlayer.currentPosition; historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, pos)) }
                                        initialSeekPosition = 0
                                        isPlayerReady = false
                                        playerError = false
                                        currentQuality = null
                                        val targetIndex = chapter.chapterIndex
                                        if (isSelected) {
                                            reloadToken++
                                        } else {
                                            currentEpIndex = targetIndex
                                        }
                                        historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, targetIndex, d.cover, System.currentTimeMillis(), d.source, 0L))
                                    }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (isSelected) Icons.Rounded.PlayArrow else Icons.Rounded.PlayCircleOutline, null, tint = iconTint, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) { Text("Episode ${chapter.chapterIndex + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textColor) }
                                        if (isWatched) Text("Ditonton", fontSize = 10.sp, color = TextGray)
                                    }
                                    HorizontalDivider(color = Color.Black.copy(0.4f), thickness = 0.5.dp)
                                }
                            }
                            item { Spacer(Modifier.height(40.dp)) }
                        }
                    }
                }
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Memuat...", color = TextGray) }
                is UiState.Error -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(getFriendlyErrorMessage(s.message), color = Color.Red)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { detailVM.loadDetail(bookId, source) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text("Coba Lagi", color = Color.White)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    nav: NavController,
    bookId: String,
    initialIndex: Int,
    bookName: String,
    source: String,
    vm: PlayerViewModel = viewModel(),
    detailVM: DetailViewModel = viewModel(),
    historyVM: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val prefs = remember { context.getSharedPreferences("video_quality_prefs", Context.MODE_PRIVATE) }
    val videoState by vm.videoState.collectAsState()
    val detailState by detailVM.detailState.collectAsState()
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var initialSeekPosition by remember { mutableLongStateOf(0L) }
    var seekDone by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(bookId) {
        detailVM.loadDetail(bookId, source)
        val item = historyList.find { it.bookId == bookId && it.chapterIndex == currentIndex }
        if (item != null) initialSeekPosition = item.position
    }

    LaunchedEffect(activity) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlaying by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEpisodeMenu by remember { mutableStateOf(false) }
    var currentQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var videoData by remember { mutableStateOf<VideoData?>(null) }
    var playerError by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var gestureIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var gestureText by remember { mutableStateOf("") }
    var showGestureOverlay by remember { mutableStateOf(false) }
    var gestureInteractionCount by remember { mutableIntStateOf(0) }
    var currentVolumeFloat by remember { mutableFloatStateOf(0f) }
    var originalBrightness by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max > 0) currentVolumeFloat = current.toFloat() / max.toFloat()
        if (originalBrightness == null) {
            originalBrightness = activity?.window?.attributes?.screenBrightness
        }
    }

    fun saveCurrentProgress(pos: Long = 0) {
        val finalPos = if (pos > 0) pos else 0L
        val existingHistory = historyList.find { it.bookId == bookId }
        val coverToSave = videoData?.cover.takeIf { !it.isNullOrEmpty() } ?: existingHistory?.cover
        historyVM.saveToHistory(LastWatched(bookId, bookName, currentIndex, coverToSave, System.currentTimeMillis(), source, finalPos))
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (videoData != null && currentQuality != null) {
                        val fallbackUrl = videoData?.videoUrl
                        if (!fallbackUrl.isNullOrEmpty() && fallbackUrl != currentQuality?.videoPath) {
                            currentQuality = null; setMediaItem(MediaItem.fromUri(fallbackUrl)); prepare(); play(); return
                        }
                    }
                    playerError = true
                }
                override fun onIsPlayingChanged(_isPlaying: Boolean) { isPlaying = _isPlaying }
                override fun onPlaybackStateChanged(s: Int) {
                    if (s == Player.STATE_ENDED) { saveCurrentProgress(0); currentIndex++; initialSeekPosition = 0; seekDone = false }
                    if (s == Player.STATE_READY) { playerError = false; videoDuration = duration }
                }
            })
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (videoState is UiState.Success) saveCurrentProgress(exoPlayer.currentPosition)
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val current = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (!isDragging) { currentPos = current; sliderValue = current.toFloat() }
            videoDuration = dur
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            if (videoState is UiState.Success) saveCurrentProgress(exoPlayer.currentPosition)
            activity?.window?.let { window ->
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            originalBrightness?.let { saved ->
                activity?.window?.attributes?.let { lp ->
                    lp.screenBrightness = saved
                    activity.window.attributes = lp
                }
            }
            exoPlayer.release()
        }
    }

    LaunchedEffect(bookId, currentIndex) {
        exoPlayer.stop(); exoPlayer.clearMediaItems(); playerError = false; currentQuality = null
        vm.loadVideo(bookId, currentIndex, bookName, source)
    }

    LaunchedEffect(videoState) {
        if (videoState is UiState.Success) {
            val data = (videoState as UiState.Success).data
            if (data.bookId != bookId || data.chapterIndex != currentIndex) return@LaunchedEffect
            videoData = data
            val availableQualities = data.qualities?.filter { it.videoPath.isNotEmpty() }.orEmpty()
            if (currentQuality == null || currentQuality?.videoPath.isNullOrEmpty()) {
                val freshSavedQuality = prefs.getInt("quality_$bookId", -1)
                val savedQ = if (freshSavedQuality != -1) availableQualities.minByOrNull { abs(it.quality - freshSavedQuality) } else null
                currentQuality = savedQ ?: (availableQualities.find { it.isDefault == 1 } ?: availableQualities.find { it.quality == 720 } ?: availableQualities.firstOrNull())
            }
            val fallbackQuality = availableQualities.firstOrNull()
            val resolvedQuality = currentQuality?.takeIf { !it.videoPath.isNullOrEmpty() } ?: fallbackQuality
            if (resolvedQuality != null) currentQuality = resolvedQuality
            val url = resolvedQuality?.videoPath?.takeIf { it.isNotEmpty() } ?: data.videoUrl
            if (url.isNotEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                var finalSeekPos = initialSeekPosition
                if (finalSeekPos == 0L) {
                    val freshHistory = historyList.find { it.bookId == bookId && it.chapterIndex == currentIndex }
                    if (freshHistory != null) finalSeekPos = freshHistory.position
                }
                if (finalSeekPos > 0 && !seekDone) exoPlayer.seekTo(finalSeekPos)
                exoPlayer.prepare()
            } else playerError = true
        }
    }

    fun changeQuality(quality: VideoQuality) {
        currentQuality = quality; showQualityDialog = false; prefs.edit { putInt("quality_$bookId", quality.quality) }
        val pos = exoPlayer.currentPosition; val p = exoPlayer.isPlaying
        exoPlayer.setMediaItem(MediaItem.fromUri(quality.videoPath)); exoPlayer.seekTo(pos); exoPlayer.prepare(); if (p) exoPlayer.play()
    }

    fun retryVideo() { playerError = false; vm.loadVideo(bookId, currentIndex, bookName, source) }
    fun seekByMs(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
        exoPlayer.seekTo(target)
        gestureIcon = if (deltaMs < 0) Icons.Rounded.FastRewind else Icons.Rounded.FastForward
        gestureText = if (deltaMs < 0) "-5s" else "+5s"
        showGestureOverlay = true
        gestureInteractionCount++
    }

    fun adjustVolume(delta: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumeChange = -delta / 2000f
        currentVolumeFloat = (currentVolumeFloat + volumeChange).coerceIn(0f, 1f)
        val newStep = (currentVolumeFloat * maxVolume).toInt()
        val currentStep = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (newStep != currentStep) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newStep, 0)
        gestureIcon = if (currentVolumeFloat <= 0.01f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp
        gestureText = "${(currentVolumeFloat * 100).toInt()}%"; showGestureOverlay = true
    }

    fun adjustBrightness(delta: Float) {
        val lp = activity?.window?.attributes
        if (lp != null) {
            var currentBrightness = lp.screenBrightness; if (currentBrightness == -1f) currentBrightness = 0.5f
            val brightnessChange = -delta / 2000f; val newBrightness = (currentBrightness + brightnessChange).coerceIn(0.01f, 1f)
            lp.screenBrightness = newBrightness; activity.window.attributes = lp
            gestureIcon = Icons.Rounded.BrightnessHigh; gestureText = "${(newBrightness * 100).toInt()}%"; showGestureOverlay = true
        }
    }

    LaunchedEffect(gestureInteractionCount) { if (showGestureOverlay) { delay(1000); showGestureOverlay = false } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (playerError) {
            Column(Modifier.fillMaxSize().background(Color.Black), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Refresh, null, tint = Color.Red, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(if (videoState is UiState.Error) getFriendlyErrorMessage((videoState as UiState.Error).message) else "Error", color = Color.White)
                Spacer(Modifier.height(16.dp)); Button(onClick = { retryVideo() }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("Coba Lagi") }
                Spacer(Modifier.height(32.dp)); TextButton(onClick = { nav.popBackStack() }) { Text("Kembali", color = TextGray) }
            }
        } else {
            AndroidView(factory = { PlayerView(it).apply { player = exoPlayer; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; keepScreenOn = true } }, modifier = Modifier.fillMaxSize())

            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val width = size.width
                        when {
                            offset.x <= width * 0.2f -> seekByMs(-5000)
                            offset.x >= width * 0.8f -> seekByMs(5000)
                        }
                    },
                    onTap = { controlsVisible = !controlsVisible }
                )
            }.pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { },
                    onDragEnd = { gestureInteractionCount++ },
                    onDragCancel = { gestureInteractionCount++ }
                ) { change, dragAmount ->
                    val width = size.width; val x = change.position.x
                    when {
                        x <= width * 0.2f -> adjustBrightness(dragAmount)
                        x >= width * 0.8f -> adjustVolume(dragAmount)
                    }
                }
            })

            AnimatedVisibility(visible = showGestureOverlay, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                Box(Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { gestureIcon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(48.dp)) }; Spacer(Modifier.height(8.dp)); Text(gestureText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                }
            }

            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val totalEpisodes = (detailState as? UiState.Success)?.data?.chapterList?.size ?: 0
                        val episodeTitle = if (totalEpisodes > 0) "Episode ${currentIndex + 1} / $totalEpisodes" else "Episode ${currentIndex + 1}"
                        Text(episodeTitle, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Box {
                        IconButton(onClick = { showEpisodeMenu = true }) { Icon(Icons.Rounded.FormatListNumbered, null, tint = Color.White) }
                        DropdownMenu(
                            expanded = showEpisodeMenu,
                            onDismissRequest = { showEpisodeMenu = false },
                            modifier = Modifier.background(CardBackground).heightIn(max = 320.dp)
                        ) {
                            val episodes = (detailState as? UiState.Success)?.data?.chapterList.orEmpty()
                            episodes.sortedBy { it.chapterIndex }.forEach { chapter ->
                                val isSelected = chapter.chapterIndex == currentIndex
                                DropdownMenuItem(
                                    text = { Text("Episode ${chapter.chapterIndex + 1}", color = if (isSelected) PrimaryColor else TextWhite) },
                                    onClick = {
                                        showEpisodeMenu = false
                                        if (chapter.chapterIndex != currentIndex) {
                                            saveCurrentProgress(exoPlayer.currentPosition)
                                            currentIndex = chapter.chapterIndex
                                            initialSeekPosition = 0
                                            seekDone = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        TextButton(onClick = { showQualityDialog = true }) { Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("${currentQuality?.quality ?: "Auto"}p", color = Color.White, fontWeight = FontWeight.Bold) }
                        DropdownMenu(expanded = showQualityDialog, onDismissRequest = { showQualityDialog = false }, modifier = Modifier.background(CardBackground)) { videoData?.qualities?.sortedByDescending { it.quality }?.forEach { q -> DropdownMenuItem(text = { Text("${q.quality}p", color = if (q.quality == currentQuality?.quality) PrimaryColor else TextWhite) }, onClick = { changeQuality(q) }) } }
                    }
                }

                if (controlsVisible) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(0.7f),
                                        Color.Transparent,
                                        Color.Black.copy(0.7f)
                                    )
                                )
                            )
                    ) {
                    }
                }

                AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { if(currentIndex > 0) { saveCurrentProgress(exoPlayer.currentPosition); currentIndex--; initialSeekPosition = 0; seekDone = false } }, enabled = currentIndex > 0) { Icon(Icons.Rounded.SkipPrevious, null, tint = if(currentIndex > 0) Color.White else Color.Gray, modifier = Modifier.size(40.dp)) }
                        Spacer(Modifier.width(24.dp))
                        IconButton(
                            onClick = { seekByMs(-5000) },
                            modifier = Modifier.size(52.dp).background(Color.White.copy(0.12f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Replay5, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }, modifier = Modifier.size(72.dp).background(PrimaryColor.copy(0.8f), CircleShape)) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
                        Spacer(Modifier.width(16.dp))
                        IconButton(
                            onClick = { seekByMs(5000) },
                            modifier = Modifier.size(52.dp).background(Color.White.copy(0.12f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Forward5, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(24.dp))
                        IconButton(onClick = { saveCurrentProgress(exoPlayer.currentPosition); currentIndex++; initialSeekPosition = 0; seekDone = false }) { Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(formatDuration(if (isDragging) sliderValue.toLong() else currentPos), color = TextWhite, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = { isDragging = true; sliderValue = it },
                            onValueChangeFinished = { exoPlayer.seekTo(sliderValue.toLong()); isDragging = false },
                            valueRange = 0f..(videoDuration.toFloat().coerceAtLeast(0f)),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryColor,
                                activeTrackColor = PrimaryColor,
                                inactiveTrackColor = Color.White.copy(0.3f)
                            ),
                            modifier = Modifier.weight(1f).height(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(formatDuration(videoDuration), color = TextWhite, fontSize = 11.sp)
                    }
                }
            }
        }
        if (videoState is UiState.Loading && !playerError) CircularProgressIndicator(color = PrimaryColor, modifier = Modifier.align(Alignment.Center))
    }
}

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
    var showAboutDialog by remember { mutableStateOf(false) }

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

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                IconButton(onClick = { isSelectionMode = false; selectedIds.clear() }) {
                    Icon(Icons.Default.Close, null, tint = TextWhite)
                }
                Text("${selectedIds.size} Dipilih", style = MaterialTheme.typography.titleMedium, color = TextWhite, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val allIds = if (selectedTab == 0) historyList.map { it.bookId } else favoritesList.map { it.bookId }
                    selectedIds.clear()
                    selectedIds.addAll(allIds)
                }) { Text("Pilih Semua", color = PrimaryColor) }
            } else {
                Text("Pustaka Saya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.weight(1f))
                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Tentang Aplikasi", tint = TextGray)
                }
            }
        }

        if (!isSelectionMode) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterTabButton(text = "Riwayat", isSelected = selectedTab == 0) { selectedTab = 0 }
                FilterTabButton(text = "Favorit", isSelected = selectedTab == 1) { selectedTab = 1 }
            }
            Spacer(Modifier.height(16.dp))
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
                                else navController.navigate("detail/${item.bookId}")
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } 
                } else {
                    item { EmptyState("Belum ada riwayat tontonan") }
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
                                else navController.navigate("detail/${item.bookId}")
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                } else {
                    item { EmptyState("Belum ada drama favorit") }
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
                Icon(Icons.Default.Delete, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Hapus (${selectedIds.size})", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableHistoryItem(
    item: LastWatched,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryColor, uncheckedColor = TextGray)
            )
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, Color.White.copy(0.06f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(Modifier.padding(10.dp)) {
                Box(modifier = Modifier.width(110.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                    if (!item.cover.isNullOrEmpty()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(item.cover).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center))
                    }
                    if (item.position > 0) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text(text = formatDuration(item.position), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).height(IntrinsicSize.Min), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        val decodedName = try { URLDecoder.decode(item.bookName, "UTF-8") } catch (_: Exception) { item.bookName }
                        Text(decodedName, maxLines = 2, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis, color = TextWhite, lineHeight = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("MELOLO", style = MaterialTheme.typography.labelSmall, color = MeloloColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(" • Episode ${item.chapterIndex + 1}", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableFavoriteItem(
    item: FavoriteDrama,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryColor, uncheckedColor = TextGray)
            )
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, Color.White.copy(0.06f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(Modifier.padding(10.dp)) {
                Box(modifier = Modifier.width(80.dp).aspectRatio(3f/4f).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                    if (!item.cover.isNullOrEmpty()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(item.cover).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).height(IntrinsicSize.Min), verticalArrangement = Arrangement.Center) {
                    Text(item.bookName, maxLines = 2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis, color = TextWhite)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Melolo", style = MaterialTheme.typography.labelSmall, color = MeloloColor, fontWeight = FontWeight.Bold, modifier = Modifier.border(1.dp, MeloloColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        if (item.totalEpisodes > 0) { Spacer(Modifier.width(8.dp)); Text("${item.totalEpisodes} Episode", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(PrimaryColor, CircleShape), Alignment.Center) {
                    Icon(Icons.Rounded.Movie, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Tentang Aplikasi", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Versi 4.0", color = TextGray, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column {
                Text("Developed by Sonzai X シ", color = TextWhite, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/November2k".toUri())) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color(0xFF0088CC), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Hubungi via Telegram", color = TextWhite)
                }
                Row(modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/November2kBio".toUri())) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Channel Update", color = TextWhite)
                }
                Spacer(Modifier.height(8.dp))
                Text("Special Thanks: @yourealya API Provider", color = TextGray, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = PrimaryColor) } }
    )
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        Triple("foryou", Icons.Default.Home, "For You"),
        Triple("new", Icons.Default.NewReleases, "New"),
        Triple("rank", Icons.AutoMirrored.Filled.TrendingUp, "Rank"),
        Triple("search", Icons.Default.Search, "Search"),
        Triple("library", Icons.Default.VideoLibrary, "Library")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(18.dp, RoundedCornerShape(28.dp)),
        color = CardBackground.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp
    ) {
        NavigationBar(containerColor = Color.Transparent) {
            items.forEach { (route, icon, label) ->
                val selected = currentRoute == route
                NavigationBarItem(
                    icon = {
                        Icon(
                            icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                    selected = selected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite,
                        selectedTextColor = TextWhite,
                        indicatorColor = PrimaryColor.copy(alpha = 0.2f),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        disabledIconColor = TextGray,
                        disabledTextColor = TextGray
                    ),
                    onClick = {
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FilterTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryColor else Color.Transparent,
        label = "tabBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryColor else TextGray.copy(alpha = 0.6f),
        label = "tabBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextGray,
        label = "tabText"
    )
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .shadow(if (isSelected) 6.dp else 0.dp, RoundedCornerShape(20.dp))
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Icon(Icons.Default.VideocamOff, null, tint = TextDarkGray, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(8.dp))
                Text(message, color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

fun androidx.compose.ui.Modifier.rotate(degrees: Float) = this.then(
    androidx.compose.ui.Modifier.graphicsLayer { rotationZ = degrees }
)

@Composable
fun FavoriteItemCard(item: FavoriteDrama, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, Color.White.copy(0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(3f/4f) 
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (!item.cover.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.cover)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f).height(IntrinsicSize.Min),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    item.bookName,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = TextWhite,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(8.dp))

                val sourceName = "Melolo"
                val sourceColor = MeloloColor

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = sourceColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.border(1.dp, sourceColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    if (item.totalEpisodes > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${item.totalEpisodes} Episode",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = PrimaryColor, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
fun HistoryItemCard(item: LastWatched, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, Color.White.copy(0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (!item.cover.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.cover)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center))
                }

                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }

                if (item.position > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(item.position),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f).height(IntrinsicSize.Min),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val decodedName = try { URLDecoder.decode(item.bookName, "UTF-8") } catch (_: Exception) { item.bookName }
                    Text(
                        decodedName,
                        maxLines = 2,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        color = TextWhite,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    val sourceName = "Melolo"
                    val sourceColor = MeloloColor

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = sourceColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(" • ", color = TextDarkGray, fontSize = 10.sp)
                        Text("Episode ${item.chapterIndex + 1}", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terakhir ditonton", fontSize = 10.sp, color = TextDarkGray)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0.6f },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = PrimaryColor,
                        trackColor = Color.DarkGray,
                    )
                }
            }
        }
    }
}
