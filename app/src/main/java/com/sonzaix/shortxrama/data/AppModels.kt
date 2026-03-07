package com.sonzaix.shortxrama.data

// --- App Internal Data Classes (Mapped from API responses) ---

data class DramaListContainer(
    val list: List<DramaItem> = emptyList(),
    val isMore: Boolean = false,
    val total: Int = 0
)

data class DramaItem(
    val bookId: String,
    val bookName: String,
    val cover: String?,
    val introduction: String?,
    val playCount: String?,
    val chapterCount: Int? = 0,
    val tags: List<String>?,
    val timestamp: Long?,
    val source: String = "melolo",
    val isNew: Boolean = false
)

data class DramaDetail(
    val bookId: String,
    val bookName: String,
    val cover: String?,
    val introduction: String?,
    val tags: List<String>?,
    val chapterList: List<Chapter>,
    val source: String = "melolo"
)

data class Chapter(
    val chapterId: String,
    val chapterIndex: Int,
    val chapterName: String?,
    val isCharge: Int,
    val vid: String?,
    val durationSeconds: Int = 0
)

data class VideoData(
    val bookId: String,
    val chapterIndex: Int,
    val videoUrl: String,
    val cover: String?,
    val qualities: List<VideoQuality>?,
    val subtitles: List<SubtitleData>? = null
)

data class SubtitleData(
    val language: String,
    val url: String,
    val displayName: String,
    val isDefault: Boolean = false
)

data class VideoQuality(
    val quality: Int,
    val videoPath: String,
    val isDefault: Int = 0,
    val codec: String? = null
)

data class QualityPreference(
    val quality: Int,
    val codec: String? = null
)

object DramaQualityMemory {
    private val memory = mutableMapOf<String, QualityPreference>()

    fun save(bookId: String, quality: Int, codec: String?) {
        memory[bookId] = QualityPreference(quality, codec)
    }

    fun get(bookId: String): QualityPreference? = memory[bookId]

    fun clear(bookId: String) { memory.remove(bookId) }
}

data class LastWatched(
    val bookId: String,
    val bookName: String,
    val chapterIndex: Int,
    val cover: String?,
    val timestamp: Long,
    val source: String = "melolo",
    val position: Long = 0L,
    val introduction: String? = null
)

data class FavoriteDrama(
    val bookId: String,
    val bookName: String,
    val cover: String?,
    val source: String = "melolo",
    val totalEpisodes: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val introduction: String? = null
)

data class AppSettings(
    val accentColor: String = "#FF2965",
    val amoledMode: Boolean = false,
    val releaseNotifications: Boolean = false,
    val releaseSchedule: Boolean = false,
    val keepAliveEnabled: Boolean = false,
    val preferredQuality: Int = 720,
    val hideWatchedDramas: Boolean = false
)

data class BackupPayload(
    val version: Int = 1,
    val history: List<LastWatched> = emptyList(),
    val favorites: List<FavoriteDrama> = emptyList()
)
