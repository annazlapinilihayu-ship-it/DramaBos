package com.sonzaix.shortxrama.ui.theme

import androidx.compose.ui.graphics.Color

val SurfaceDark = Color(0xFF181818)
val SurfaceVariantDark = Color(0xFF232323)
val TextWhite = Color(0xFFF5F5F5)
val TextGray = Color(0xFFB0B0B0)
val TextDarkGray = Color(0xFF808080)
val WatchedColor = Color(0xFF333333)
val SearchBarColor = Color(0xFF252525)
val BadgeBackground = Color.Black.copy(alpha = 0.7f)
val MeloloColor = Color(0xFF6C63FF)
val DramaboxColor = Color(0xFFFF6F61)
val ReelShortColor = Color(0xFF26C6DA)
val FreeReelsColor = Color(0xFF66BB6A)
val NetShortColor = Color(0xFF00BCD4)
val MeloShortColor = Color(0xFFF06292) // A nice pink hue for "melo"
val GoodShortColor = Color(0xFFFF8A65)
val DramaWaveColor = Color(0xFF00C9A7)

fun getFriendlyErrorMessage(msg: String): String {
    return when {
        msg.contains("Unable to resolve host", ignoreCase = true) -> "Tidak ada koneksi internet"
        msg.contains("timeout", ignoreCase = true) -> "Server terlalu lama merespon"
        msg.contains("HTTP 5", ignoreCase = true) -> "Server sedang bermasalah"
        else -> "Terjadi kesalahan: $msg"
    }
}

fun sourceInfo(source: String): Pair<String, Color> {
    return when (source.lowercase()) {
        "dramabox" -> "DramaBox" to DramaboxColor
        "reelshort" -> "ReelShort" to ReelShortColor
        "freereels" -> "FreeReels" to FreeReelsColor
        "netshort" -> "NetShort" to NetShortColor
        "meloshort" -> "MeloShort" to MeloShortColor
        "goodshort" -> "GoodShort" to GoodShortColor
        "dramawave" -> "DramaWave" to DramaWaveColor
        else -> "Melolo" to MeloloColor
    }
}

fun sourceLogoFile(source: String): String {
    return when (source.lowercase()) {
        "dramabox" -> "dramabox.webp"
        "reelshort" -> "reelshort.webp"
        "freereels" -> "freereels.webp"
        "netshort" -> "netshort.webp"
        "meloshort" -> "meloshort.webp"
        "goodshort" -> "goodshort.jpg"
        "dramawave" -> "dramawave.webp"
        else -> "melolo.webp"
    }
}

fun parseColorHex(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
