package com.sonzaix.shortxrama.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import com.sonzaix.shortxrama.data.DramaItem
import com.sonzaix.shortxrama.data.LastWatched
import com.sonzaix.shortxrama.viewmodel.HistoryViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(durationMs: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(durationMs)
    val m = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun playSmart(navController: NavController, item: DramaItem, historyList: List<LastWatched>, vm: HistoryViewModel) {
    val encodedName = URLEncoder.encode(item.bookName, StandardCharsets.UTF_8.toString())
    val encodedCover = URLEncoder.encode(item.cover ?: "", StandardCharsets.UTF_8.toString())
    val encodedIntro = URLEncoder.encode(item.introduction ?: "", StandardCharsets.UTF_8.toString())
    val encodedSource = URLEncoder.encode(item.source, StandardCharsets.UTF_8.toString())
    navController.navigate("detail/${item.bookId}?bookName=$encodedName&cover=$encodedCover&intro=$encodedIntro&source=$encodedSource")
}

fun Modifier.rotate(degrees: Float) = this.then(
    Modifier.graphicsLayer { rotationZ = degrees }
)
