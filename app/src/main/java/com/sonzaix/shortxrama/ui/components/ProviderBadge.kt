package com.sonzaix.shortxrama.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonzaix.shortxrama.ui.theme.sourceLogoFile

@Composable
fun ProviderBadge(source: String, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logoFile = sourceLogoFile(source)
    val bitmap = remember(logoFile) {
        try {
            context.assets.open(logoFile).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = source,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
