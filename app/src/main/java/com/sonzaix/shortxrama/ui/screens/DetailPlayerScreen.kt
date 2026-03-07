package com.sonzaix.shortxrama.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.sonzaix.shortxrama.data.*
import com.sonzaix.shortxrama.ui.components.*
import com.sonzaix.shortxrama.ui.theme.*
import com.sonzaix.shortxrama.ui.util.formatDuration
import com.sonzaix.shortxrama.viewmodel.*
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DetailPlayerScreen(
    nav: NavController,
    bookId: String,
    source: String,
    passedBookName: String = "",
    passedCover: String = "",
    passedIntro: String = "",
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
    val settingsStore = remember { AppSettingsStore(context) }
    val appSettings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    var currentEpIndex by remember { mutableIntStateOf(0) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var initialLoadDone by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf(false) }
    var playerErrorMsg by remember { mutableStateOf("") }
    var currentQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var videoData by remember { mutableStateOf<VideoData?>(null) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var totalTime by remember { mutableLongStateOf(0L) }
    var initialSeekPosition by remember { mutableLongStateOf(0L) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isUserSwitchingEpisode by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    val hasSubtitles = videoData?.subtitles?.isNotEmpty() == true

    fun buildReferer(url: String): String? {
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        return "$scheme://$host"
    }

    fun buildHlsMediaItem(url: String, subtitles: List<SubtitleData>? = null): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (url.contains(".m3u8", ignoreCase = true)) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        } else if (url.contains("mime_type=video_mp4", ignoreCase = true) || url.contains("awscdn.netshort.com", ignoreCase = true)) {
            builder.setMimeType(MimeTypes.VIDEO_MP4)
        }
        if (!subtitles.isNullOrEmpty()) {
            val subtitleConfigs = subtitles.map { sub ->
                val subtitleMime = when {
                    sub.url.contains(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    sub.url.contains("mime_type=text_plain", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    sub.url.contains("awscdn.netshort.com", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    sub.url.contains(".ass", ignoreCase = true) || sub.url.contains(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(subtitleMime)
                    .setLanguage(sub.language)
                    .setLabel(sub.displayName)
                    .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
            builder.setSubtitleConfigurations(subtitleConfigs)
        }
        return builder.build()
    }

    val httpDataSourceFactory = remember {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
        OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("ShortXRama/1.0 (Android)")
    }
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredVideoMimeType("video/avc")
                    .setPreferredTextLanguage("id")
                    .setSelectUndeterminedTextLanguage(true)
            )
        }
    }
    val mediaSourceFactory = remember { DefaultMediaSourceFactory(httpDataSourceFactory) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = true
                    isPlayerReady = false
                    val cause = error.cause
                    val errorDetail = buildString {
                        append(error.errorCodeName)
                        if (cause is HttpDataSource.InvalidResponseCodeException) {
                            append(" HTTP ${cause.responseCode}")
                        }
                        cause?.message?.let { append(": $it") }
                    }
                    playerErrorMsg = errorDetail
                    Log.e("ShortXRamaPlayer", "Playback error: $errorDetail", error)
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
    var isResumed by remember { mutableStateOf(true) }
    var wasPlayingBeforePause by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isResumed = false
                    wasPlayingBeforePause = exoPlayer.isPlaying
                    if (detailState is UiState.Success && isPlayerReady) {
                        val d = (detailState as UiState.Success).data
                        val pos = exoPlayer.currentPosition
                        historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, pos, d.introduction))
                    }
                    try { exoPlayer.pause() } catch (e: Exception) { }
                }
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    if (wasPlayingBeforePause && isPlayerReady && videoData != null) {
                        try { exoPlayer.play() } catch (e: Exception) { }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try { exoPlayer.release() } catch (e: Exception) { }
                }
                else -> {}
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
        currentEpIndex = 0; initialSeekPosition = 0L; initialLoadDone = false; isPlayerReady = false; playerError = false; currentQuality = null; videoData = null
        detailVM.loadDetail(bookId, source, passedBookName, passedCover, passedIntro)
    }

    LaunchedEffect(historyList) {
        if (isUserSwitchingEpisode) return@LaunchedEffect
        if (historyList != null && !initialLoadDone) {
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
                try {
                    exoPlayer.stop(); exoPlayer.clearMediaItems(); playerError = false; isPlayerReady = false; currentQuality = null
                    playerVM.loadVideo(bookId, currentEpIndex, d.bookName, source, d.introduction, appSettings.preferredQuality, dramaCover = d.cover)
                } catch (e: Exception) { playerError = true }
            }
        }
    }

    LaunchedEffect(videoState) {
        if (videoState is UiState.Success) {
            val data = (videoState as UiState.Success).data
            if (data.bookId != bookId || data.chapterIndex != currentEpIndex) return@LaunchedEffect
            try {
                videoData = data
                val availableQualities = data.qualities?.filter { it.videoPath.isNotEmpty() }.orEmpty()
                if (currentQuality == null || currentQuality?.videoPath.isNullOrEmpty()) {
                    val dramaQPref = DramaQualityMemory.get(bookId)
                    val preferred = dramaQPref?.quality ?: appSettings.preferredQuality
                    val preferredCodec = dramaQPref?.codec
                    val picked = DramaRepository.pickPreferredQuality(availableQualities, preferred, preferredCodec)
                    currentQuality = picked ?: (availableQualities.find { it.isDefault == 1 } ?: availableQualities.find { it.quality == 720 } ?: availableQualities.firstOrNull())
                }
                val fallbackQuality = availableQualities.firstOrNull()
                val resolvedQuality = currentQuality?.takeIf { !it.videoPath.isNullOrEmpty() } ?: fallbackQuality
                if (resolvedQuality != null) currentQuality = resolvedQuality
                val url = resolvedQuality?.videoPath?.takeIf { it.isNotEmpty() } ?: data.videoUrl
                if (url.isNotEmpty()) {
                    val referer = buildReferer(url)
                    val headers = mutableMapOf<String, String>()
                    if (referer != null) {
                        headers["Referer"] = referer
                        headers["Origin"] = referer
                    }
                    httpDataSourceFactory.setDefaultRequestProperties(headers)
                    Log.d("ShortXRamaPlayer", "Playing URL (source=$source): ${url.take(120)}...")
                    exoPlayer.setMediaItem(buildHlsMediaItem(url, data.subtitles))
                    if (initialSeekPosition > 0) exoPlayer.seekTo(initialSeekPosition)
                    exoPlayer.prepare()
                } else { playerError = true }
            } catch (e: Exception) { playerError = true }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (detailState is UiState.Success && isPlayerReady) {
                    val d = (detailState as UiState.Success).data
                    historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, exoPlayer.currentPosition, d.introduction))
                }
                exoPlayer.stop(); exoPlayer.clearMediaItems(); exoPlayer.release()
            } catch (e: Exception) { }
        }
    }

    fun goFullscreen() {
        if (detailState is UiState.Success) {
            val d = (detailState as UiState.Success).data
            val encodedName = URLEncoder.encode(d.bookName, StandardCharsets.UTF_8.toString())
            val pos = exoPlayer.currentPosition
            historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, pos))
            exoPlayer.pause()
            val encodedSource = URLEncoder.encode(source, StandardCharsets.UTF_8.toString())
            nav.navigate("player_full/$bookId/$currentEpIndex/$encodedName?source=$encodedSource")
        }
    }

    fun retryVideo() {
        playerError = false; isPlayerReady = false
        val d = (detailState as? UiState.Success)?.data
        if (d != null) { playerVM.loadVideo(bookId, currentEpIndex, d.bookName, source, preferredQuality = appSettings.preferredQuality, dramaCover = d.cover) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black)) {
            if (playerError) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Refresh, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Gagal memuat video", color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (playerErrorMsg.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(playerErrorMsg, color = Color.Red.copy(0.7f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { retryVideo() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Coba Lagi")
                    }
                }
            } else if (isPlayerReady) {
                AndroidView(
                    factory = { PlayerView(it).apply {
                        player = exoPlayer; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; keepScreenOn = true
                        subtitleView?.setStyle(CaptionStyleCompat(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null))
                        subtitleView?.setFractionalTextSize(0.04f)
                    } },
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                IconButton(onClick = { goFullscreen() }, modifier = Modifier.align(Alignment.Center).size(72.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Row(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasSubtitles) {
                        IconButton(onClick = { subtitlesEnabled = !subtitlesEnabled; trackSelector.setParameters(trackSelector.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)) }, modifier = Modifier.size(36.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(if (subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled, contentDescription = if (subtitlesEnabled) "Matikan Subtitle" else "Nyalakan Subtitle", tint = if (subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = { goFullscreen() }, modifier = Modifier.size(36.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                        Icon(Icons.Rounded.Fullscreen, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Box(Modifier.align(Alignment.BottomStart).padding(12.dp).background(Color.Black.copy(0.65f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("${formatDuration(currentTime)} / ${formatDuration(totalTime)}", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) }
            }
            IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        }

        // Gradient divider below video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        if (historyList == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
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
                                    Text(d.bookName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextWhite, fontSize = 26.sp)
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        InfoChip(text = "${d.chapterList.size} Episode", color = MaterialTheme.colorScheme.primary)
                                        InfoChip(text = "Ongoing", color = TextGray)
                                        ProviderBadge(source = d.source, size = 20.dp)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = { favoriteVM.toggleFavorite(FavoriteDrama(d.bookId, d.bookName, d.cover, d.source, d.chapterList.size, System.currentTimeMillis(), d.introduction)) }, colors = ButtonDefaults.buttonColors(containerColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp)) {
                                        Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFavorite) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(if (isFavorite) "Sudah Disukai" else "Tambah ke Favorit", color = if (isFavorite) Color.White else TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { showDownloadDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                                        Text("Unduh Episode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Spacer(Modifier.height(18.dp))
                                    d.tags?.let { tags ->
                                        if (tags.isNotEmpty()) {
                                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                tags.forEach { tag ->
                                                    Box(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(0.6f), RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(0.1f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                                        Text(tag, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(16.dp))
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp)).padding(14.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp)
                                            Text(d.introduction ?: "", style = MaterialTheme.typography.bodySmall, color = TextGray, lineHeight = 20.sp, fontSize = 13.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text("Daftar Episode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TextWhite, fontSize = 15.sp)
                                        Spacer(Modifier.weight(1f))
                                        Text("${currentEpIndex + 1} / ${d.chapterList.size}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            items(d.chapterList) { chapter ->
                                val isSelected = chapter.chapterIndex == currentEpIndex
                                val isWatched = chapter.chapterIndex <= lastWatchedIndex && !isSelected
                                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                val textColor = if (isSelected) MaterialTheme.colorScheme.primary else TextWhite
                                val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else TextGray
                                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(12.dp)).border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                isUserSwitchingEpisode = true
                                                if (isPlayerReady) { historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, currentEpIndex, d.cover, System.currentTimeMillis(), d.source, exoPlayer.currentPosition, d.introduction)) }
                                                initialSeekPosition = 0; isPlayerReady = false; playerError = false; currentQuality = null
                                                val targetIndex = chapter.chapterIndex
                                                if (isSelected) { reloadToken++ } else { currentEpIndex = targetIndex }
                                                historyVM.saveToHistory(LastWatched(d.bookId, d.bookName, targetIndex, d.cover, System.currentTimeMillis(), d.source, 0L, d.introduction))
                                                isUserSwitchingEpisode = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (isSelected) Icons.Rounded.PlayArrow else Icons.Rounded.PlayCircleOutline, null, tint = iconTint, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Episode ${chapter.chapterIndex + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, color = textColor, fontSize = 14.sp)
                                            // Per-episode progress bar
                                            if (isWatched || isSelected) {
                                                val epHistory = historyList.find { it.bookId == bookId && it.chapterIndex == chapter.chapterIndex }
                                                val watchProgress = if (epHistory != null && epHistory.position > 0 && totalTime > 0) {
                                                    (epHistory.position.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
                                                } else if (isWatched) {
                                                    1f // Assume fully watched
                                                } else {
                                                    0f
                                                }
                                                if (watchProgress > 0f) {
                                                    Spacer(Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { watchProgress },
                                                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = Color.DarkGray,
                                                    )
                                                }
                                            }
                                        }
                                        if (isWatched) Text("✓", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
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
                        Button(onClick = { detailVM.loadDetail(bookId, source, passedBookName, passedCover, passedIntro) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Coba Lagi", color = Color.White) }
                    }
                }
                else -> {}
            }
        }
        
        if (showDownloadDialog && detailState is UiState.Success) {
            val d = (detailState as UiState.Success).data
            DownloadEpisodeDialog(dramaId = d.bookId, dramaSource = d.source, dramaNa = d.bookName, totalEpisodes = d.chapterList.size, onDismiss = { showDownloadDialog = false })
        }
    }
}
