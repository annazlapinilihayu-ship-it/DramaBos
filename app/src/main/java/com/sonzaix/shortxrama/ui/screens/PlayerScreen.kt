package com.sonzaix.shortxrama.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.sonzaix.shortxrama.data.*
import com.sonzaix.shortxrama.ui.theme.*
import com.sonzaix.shortxrama.ui.util.findActivity
import com.sonzaix.shortxrama.ui.util.formatDuration
import com.sonzaix.shortxrama.viewmodel.*
import kotlinx.coroutines.delay
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
    val settingsStore = remember { AppSettingsStore(context) }
    val appSettings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val videoState by vm.videoState.collectAsState()
    val detailState by detailVM.detailState.collectAsState()
    val historyList by historyVM.historyList.collectAsState(initial = emptyList())

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var initialSeekPosition by remember { mutableLongStateOf(0L) }
    var seekDone by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastControlsInteraction by remember { mutableLongStateOf(0L) }

    LaunchedEffect(bookId) {
        detailVM.loadDetail(bookId, source, bookName, "", "")
        val item = historyList.find { it.bookId == bookId && it.chapterIndex == currentIndex }
        if (item != null) initialSeekPosition = item.position
    }

    LaunchedEffect(controlsVisible, lastControlsInteraction) {
        if (controlsVisible) {
            val interactionStamp = lastControlsInteraction
            delay(3500)
            if (controlsVisible && interactionStamp == lastControlsInteraction) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(activity) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlaying by remember { mutableStateOf(true) }
    var wasPlayingBeforePause by remember { mutableStateOf(false) }
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
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var showSubtitleSync by remember { mutableStateOf(false) }
    val hasSubtitles = videoData?.subtitles?.isNotEmpty() == true

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
        val dramaCover = (detailState as? UiState.Success)?.data?.cover
        val coverToSave = dramaCover.takeIf { !it.isNullOrEmpty() } ?: existingHistory?.cover
        val intro = (detailState as? UiState.Success)?.data?.introduction
        val introToSave = intro?.takeIf { it.isNotEmpty() } ?: existingHistory?.introduction
        historyVM.saveToHistory(LastWatched(bookId, bookName, currentIndex, coverToSave, System.currentTimeMillis(), source, finalPos, introToSave))
    }

    val httpDataSourceFactory2 = remember {
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
    val mediaSourceFactory2 = remember { DefaultMediaSourceFactory(httpDataSourceFactory2) }

    fun buildReferer2(url: String): String? {
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        return "$scheme://$host"
    }

    fun buildHlsMediaItem2(url: String, subtitles: List<SubtitleData>? = null): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (url.contains(".m3u8", ignoreCase = true)) { builder.setMimeType(MimeTypes.APPLICATION_M3U8) }
        else if (url.contains("mime_type=video_mp4", ignoreCase = true) || url.contains("awscdn.netshort.com", ignoreCase = true)) { builder.setMimeType(MimeTypes.VIDEO_MP4) }
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

    val trackSelector2 = remember {
        DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setPreferredVideoMimeType("video/avc").setPreferredTextLanguages("id", "ind", "ind-ID").setSelectUndeterminedTextLanguage(true))
        }
    }

    var subtitleViewRef by remember { mutableStateOf<androidx.media3.ui.SubtitleView?>(null) }

    // Parsed subtitle cues for manual rendering with offset
    data class ParsedCue(val startMs: Long, val endMs: Long, val text: String)
    var parsedCues by remember { mutableStateOf<List<ParsedCue>>(emptyList()) }

    // Parse VTT subtitle when video data changes (only re-download when URL changes)
    LaunchedEffect(videoData?.subtitles) {
        val subs = videoData?.subtitles
        if (subs.isNullOrEmpty()) {
            parsedCues = emptyList()
            return@LaunchedEffect
        }
        // Find the best subtitle (prefer Indonesian)
        val bestSub = subs.find { it.isDefault } ?: subs.firstOrNull() ?: return@LaunchedEffect
        try {
            val subtitleText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.URL(bestSub.url).readText()
            }
            // Parse VTT/SRT timestamps
            val cueList = mutableListOf<ParsedCue>()
            val timePattern = Regex("""(\d{1,2}):(\d{2}):(\d{2})[.,](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[.,](\d{3})""")
            val lines = subtitleText.lines()
            var i = 0
            while (i < lines.size) {
                val match = timePattern.find(lines[i])
                if (match != null) {
                    val (h1, m1, s1, ms1, h2, m2, s2, ms2) = match.destructured
                    val startMs = h1.toLong() * 3600000 + m1.toLong() * 60000 + s1.toLong() * 1000 + ms1.toLong()
                    val endMs = h2.toLong() * 3600000 + m2.toLong() * 60000 + s2.toLong() * 1000 + ms2.toLong()
                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].isNotBlank()) {
                        textLines.add(lines[i].replace(Regex("<[^>]*>"), "")) // Strip HTML tags
                        i++
                    }
                    if (textLines.isNotEmpty()) {
                        cueList.add(ParsedCue(startMs, endMs, textLines.joinToString("\n")))
                    }
                }
                i++
            }
            parsedCues = cueList
        } catch (e: Exception) {
            parsedCues = emptyList()
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector2)
            .setMediaSourceFactory(mediaSourceFactory2)
            .build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (videoData != null && currentQuality != null) {
                        val fallbackUrl = videoData?.videoUrl
                        if (!fallbackUrl.isNullOrEmpty() && fallbackUrl != currentQuality?.videoPath) {
                            val ref = buildReferer2(fallbackUrl)
                            val hdr = mutableMapOf<String, String>()
                            if (ref != null) { hdr["Referer"] = ref; hdr["Origin"] = ref }
                            httpDataSourceFactory2.setDefaultRequestProperties(hdr)
                            currentQuality = null; setMediaItem(buildHlsMediaItem2(fallbackUrl, videoData?.subtitles)); prepare(); play(); return
                        }
                    }
                    playerError = true
                }
                override fun onIsPlayingChanged(_isPlaying: Boolean) { isPlaying = _isPlaying }
                override fun onPlaybackStateChanged(s: Int) {
                    if (s == Player.STATE_ENDED) {
                        saveCurrentProgress(0)
                        val totalEps = (detailState as? UiState.Success)?.data?.chapterList?.size ?: 0
                        if (totalEps > 0 && currentIndex >= totalEps - 1) {
                            nav.popBackStack()
                        } else {
                            currentIndex++; initialSeekPosition = 0; seekDone = false; subtitleOffsetMs = 0
                        }
                    }
                    if (s == Player.STATE_READY) { playerError = false; videoDuration = duration }
                }
            })
        }
    }

    // When offset is non-zero, disable ExoPlayer's subtitle track and render manually
    LaunchedEffect(subtitleOffsetMs) {
        if (subtitleOffsetMs != 0L) {
            trackSelector2.setParameters(trackSelector2.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true))
        } else if (subtitlesEnabled) {
            trackSelector2.setParameters(trackSelector2.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false))
        }
    }

    // Custom subtitle rendering with offset
    LaunchedEffect(exoPlayer, subtitleOffsetMs, subtitlesEnabled, parsedCues) {
        if (subtitleOffsetMs == 0L || parsedCues.isEmpty() || !subtitlesEnabled) return@LaunchedEffect
        while (true) {
            val sv = subtitleViewRef
            if (sv != null) {
                val effectivePos = exoPlayer.currentPosition + subtitleOffsetMs
                val activeCue = parsedCues.find { effectivePos in it.startMs..it.endMs }
                if (activeCue != null) {
                    sv.setCues(listOf(androidx.media3.common.text.Cue.Builder().setText(activeCue.text).build()))
                } else {
                    sv.setCues(emptyList())
                }
            }
            delay(50)
        }
    }

    // Clear manual cues when offset returns to zero or subtitles disabled
    LaunchedEffect(subtitleOffsetMs, subtitlesEnabled) {
        if (subtitleOffsetMs == 0L || !subtitlesEnabled) {
            subtitleViewRef?.setCues(emptyList())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    wasPlayingBeforePause = exoPlayer.isPlaying
                    if (videoState is UiState.Success) saveCurrentProgress(exoPlayer.currentPosition)
                    try { exoPlayer.pause() } catch (e: Exception) { }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (wasPlayingBeforePause && videoState is UiState.Success && exoPlayer.currentMediaItem != null) {
                        try { exoPlayer.play() } catch (e: Exception) { }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try { exoPlayer.stop(); exoPlayer.release() } catch (e: Exception) { }
                }
                else -> {}
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
            try {
                if (videoState is UiState.Success) saveCurrentProgress(exoPlayer.currentPosition)
                activity?.window?.let { window -> WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars()) }
                originalBrightness?.let { saved -> activity?.window?.attributes?.let { lp -> lp.screenBrightness = saved; activity.window.attributes = lp } }
                exoPlayer.stop(); exoPlayer.clearMediaItems(); exoPlayer.release()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(bookId, currentIndex) {
        try {
            exoPlayer.stop(); exoPlayer.clearMediaItems(); playerError = false; currentQuality = null
            val intro = (detailState as? UiState.Success)?.data?.introduction
            val dCover = (detailState as? UiState.Success)?.data?.cover
            vm.loadVideo(bookId, currentIndex, bookName, source, intro, appSettings.preferredQuality, dramaCover = dCover)
        } catch (e: Exception) { playerError = true }
    }

    LaunchedEffect(videoState) {
        if (videoState is UiState.Success) {
            val data = (videoState as UiState.Success).data
            if (data.bookId != bookId || data.chapterIndex != currentIndex) return@LaunchedEffect
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
                    val referer = buildReferer2(url)
                    val headers = mutableMapOf<String, String>()
                    if (referer != null) { headers["Referer"] = referer; headers["Origin"] = referer }
                    httpDataSourceFactory2.setDefaultRequestProperties(headers)
                    exoPlayer.setMediaItem(buildHlsMediaItem2(url, data.subtitles))
                    var finalSeekPos = initialSeekPosition
                    if (finalSeekPos == 0L) { val freshHistory = historyList.find { it.bookId == bookId && it.chapterIndex == currentIndex }; if (freshHistory != null) finalSeekPos = freshHistory.position }
                    if (finalSeekPos > 0 && !seekDone) exoPlayer.seekTo(finalSeekPos)
                    exoPlayer.prepare()
                } else { playerError = true }
            } catch (e: Exception) { playerError = true }
        }
    }

    fun changeQuality(quality: VideoQuality) {
        try {
            currentQuality = quality; showQualityDialog = false
            DramaQualityMemory.save(bookId, quality.quality, quality.codec)
            val pos = exoPlayer.currentPosition; val p = exoPlayer.isPlaying
            val qRef = buildReferer2(quality.videoPath)
            val qHeaders = mutableMapOf<String, String>()
            if (qRef != null) { qHeaders["Referer"] = qRef; qHeaders["Origin"] = qRef }
            httpDataSourceFactory2.setDefaultRequestProperties(qHeaders)
            exoPlayer.setMediaItem(buildHlsMediaItem2(quality.videoPath, videoData?.subtitles)); exoPlayer.seekTo(pos); exoPlayer.prepare(); if (p) exoPlayer.play()
        } catch (e: Exception) { playerError = true }
    }

    fun retryVideo() {
        playerError = false
        val intro = (detailState as? UiState.Success)?.data?.introduction
        val dCover = (detailState as? UiState.Success)?.data?.cover
        vm.loadVideo(bookId, currentIndex, bookName, source, intro, appSettings.preferredQuality, dramaCover = dCover)
    }
    fun seekByMs(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
        exoPlayer.seekTo(target)
        gestureIcon = if (deltaMs < 0) Icons.Rounded.FastRewind else Icons.Rounded.FastForward
        gestureText = if (deltaMs < 0) "-5s" else "+5s"; showGestureOverlay = true; gestureInteractionCount++
    }

    fun adjustVolume(delta: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumeChange = -delta / 2000f; currentVolumeFloat = (currentVolumeFloat + volumeChange).coerceIn(0f, 1f)
        val newStep = (currentVolumeFloat * maxVolume).toInt(); val currentStep = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
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
                Spacer(Modifier.height(16.dp)); Button(onClick = { retryVideo() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Coba Lagi") }
                Spacer(Modifier.height(32.dp)); TextButton(onClick = { nav.popBackStack() }) { Text("Kembali", color = TextGray) }
            }
        } else {
            AndroidView(factory = { PlayerView(it).apply {
                player = exoPlayer; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; keepScreenOn = true
                // Disable auto subtitle rendering — we render manually with offset
                subtitleView?.setStyle(CaptionStyleCompat(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null))
                subtitleView?.setFractionalTextSize(0.04f)
                subtitleViewRef = subtitleView
            } }, modifier = Modifier.fillMaxSize())

            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        controlsVisible = true; lastControlsInteraction = System.currentTimeMillis()
                        val width = size.width
                        when { offset.x <= width * 0.2f -> seekByMs(-5000); offset.x >= width * 0.8f -> seekByMs(5000) }
                    },
                    onTap = { controlsVisible = !controlsVisible; lastControlsInteraction = System.currentTimeMillis() }
                )
            }.pointerInput(Unit) {
                detectVerticalDragGestures(onDragStart = { }, onDragEnd = { gestureInteractionCount++ }, onDragCancel = { gestureInteractionCount++ }) { change, dragAmount ->
                    val width = size.width; val x = change.position.x
                    when { x <= width * 0.2f -> adjustBrightness(dragAmount); x >= width * 0.8f -> adjustVolume(dragAmount) }
                }
            })

            AnimatedVisibility(visible = showGestureOverlay, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                Box(Modifier.background(Color.Black.copy(0.75f), RoundedCornerShape(20.dp)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        gestureIcon?.let { Icon(it, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) }
                        Spacer(Modifier.height(12.dp))
                        Text(gestureText, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(0.8f), Color.Transparent), startY = 0f, endY = 120f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val totalEpisodes = (detailState as? UiState.Success)?.data?.chapterList?.size ?: 0
                        val episodeTitle = if (totalEpisodes > 0) "Episode ${currentIndex + 1} / $totalEpisodes" else "Episode ${currentIndex + 1}"
                        Text(episodeTitle, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
                    }
                    Box {
                        IconButton(onClick = { controlsVisible = true; lastControlsInteraction = System.currentTimeMillis(); showEpisodeMenu = true }) {
                            Icon(Icons.Rounded.FormatListNumbered, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        DropdownMenu(
                            expanded = showEpisodeMenu, onDismissRequest = { showEpisodeMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface).heightIn(max = 320.dp).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                        ) {
                            val episodes = (detailState as? UiState.Success)?.data?.chapterList.orEmpty()
                            episodes.sortedBy { it.chapterIndex }.forEach { chapter ->
                                val isSelected = chapter.chapterIndex == currentIndex
                                DropdownMenuItem(
                                    text = { Text("Episode ${chapter.chapterIndex + 1}", color = if (isSelected) MaterialTheme.colorScheme.primary else TextWhite, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { showEpisodeMenu = false; if (chapter.chapterIndex != currentIndex) { saveCurrentProgress(exoPlayer.currentPosition); currentIndex = chapter.chapterIndex; initialSeekPosition = 0; seekDone = false } }
                                )
                            }
                        }
                    }
                    if (hasSubtitles) {
                        IconButton(onClick = { controlsVisible = true; lastControlsInteraction = System.currentTimeMillis(); subtitlesEnabled = !subtitlesEnabled; trackSelector2.setParameters(trackSelector2.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)) }) {
                            Icon(if (subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled, contentDescription = if (subtitlesEnabled) "Matikan Subtitle" else "Nyalakan Subtitle", tint = if (subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    if (hasSubtitles && subtitlesEnabled) {
                        Box {
                            IconButton(onClick = { controlsVisible = true; lastControlsInteraction = System.currentTimeMillis(); showSubtitleSync = !showSubtitleSync }) {
                                Icon(Icons.Rounded.Tune, contentDescription = "Subtitle Sync", tint = if (subtitleOffsetMs != 0L) MaterialTheme.colorScheme.primary else Color.White, modifier = Modifier.size(24.dp))
                            }
                            DropdownMenu(
                                expanded = showSubtitleSync, onDismissRequest = { showSubtitleSync = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text("Sinkronisasi Subtitle", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (subtitleOffsetMs == 0L) "0ms (Default)" else if (subtitleOffsetMs > 0) "+${subtitleOffsetMs}ms (Lebih Cepat)" else "${subtitleOffsetMs}ms (Lebih Lambat)",
                                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(-500L to "-500", -100L to "-100", 0L to "Reset", 100L to "+100", 500L to "+500").forEach { (offset, label) ->
                                            val isReset = offset == 0L
                                            OutlinedButton(
                                                onClick = {
                                                    lastControlsInteraction = System.currentTimeMillis()
                                                    subtitleOffsetMs = if (isReset) 0L else subtitleOffsetMs + offset
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isReset) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isReset) MaterialTheme.colorScheme.primary else TextWhite)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box {
                        TextButton(onClick = { controlsVisible = true; lastControlsInteraction = System.currentTimeMillis(); showQualityDialog = true }) {
                            Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp))
                            val qualityLabel = currentQuality?.let { q -> val codecLabel = q.codec?.takeIf { it.equals("H264", ignoreCase = true) || it.equals("H265", ignoreCase = true) }; if (!codecLabel.isNullOrBlank()) "${q.quality}p $codecLabel" else "${q.quality}p" } ?: "Auto"
                            Text(qualityLabel, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        DropdownMenu(
                            expanded = showQualityDialog, onDismissRequest = { showQualityDialog = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                        ) {
                            videoData?.qualities?.sortedByDescending { it.quality }?.forEach { q ->
                                DropdownMenuItem(
                                    text = {
                                        val codecLabel = q.codec?.takeIf { it.equals("H264", ignoreCase = true) || it.equals("H265", ignoreCase = true) }
                                        val label = if (!codecLabel.isNullOrBlank()) "${q.quality}p $codecLabel" else "${q.quality}p"
                                        val isSelected = q.videoPath == currentQuality?.videoPath
                                        Text(label, color = if (isSelected) MaterialTheme.colorScheme.primary else TextWhite, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    onClick = { changeQuality(q) }
                                )
                            }
                        }
                    }
                }

                if (controlsVisible) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(0.55f), Color.Transparent, Color.Black.copy(0.65f))))) { }
                }

                AnimatedVisibility(visible = controlsVisible, enter = fadeIn(animationSpec = tween(200)), exit = fadeOut(animationSpec = tween(200)), modifier = Modifier.align(Alignment.Center)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { if(currentIndex > 0) { saveCurrentProgress(exoPlayer.currentPosition); currentIndex--; initialSeekPosition = 0; seekDone = false } }, enabled = currentIndex > 0, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.SkipPrevious, null, tint = if(currentIndex > 0) Color.White else Color.Gray, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(24.dp))
                        IconButton(onClick = { seekByMs(-5000) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.Replay5, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                        Spacer(Modifier.width(20.dp))
                        IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }, modifier = Modifier.size(80.dp)) {
                            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                        Spacer(Modifier.width(20.dp))
                        IconButton(onClick = { seekByMs(5000) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.Forward5, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                        Spacer(Modifier.width(24.dp))
                        val totalEpisodes2 = (detailState as? UiState.Success)?.data?.chapterList?.size ?: 0
                        val hasNextEpisode = totalEpisodes2 == 0 || currentIndex < totalEpisodes2 - 1
                        IconButton(onClick = { if (hasNextEpisode) { saveCurrentProgress(exoPlayer.currentPosition); currentIndex++; initialSeekPosition = 0; seekDone = false } }, enabled = hasNextEpisode, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.SkipNext, null, tint = if (hasNextEpisode) Color.White else Color.Gray, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(0.8f)), startY = 0f, endY = 120f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(formatDuration(if (isDragging) sliderValue.toLong() else currentPos), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(12.dp))
                        Slider(
                            value = sliderValue, onValueChange = { isDragging = true; sliderValue = it },
                            onValueChangeFinished = { exoPlayer.seekTo(sliderValue.toLong()); isDragging = false },
                            valueRange = 0f..(videoDuration.toFloat().coerceAtLeast(0f)),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(0.2f)),
                            modifier = Modifier.weight(1f).height(14.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(formatDuration(videoDuration), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (videoState is UiState.Loading && !playerError) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
    }
}
