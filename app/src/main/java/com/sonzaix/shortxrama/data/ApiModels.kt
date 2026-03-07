package com.sonzaix.shortxrama.data

import com.google.gson.annotations.SerializedName

// --- Generic API Response ---

data class ApiResponse<T>(
    val author: String?,
    val message: String?,
    val type: String?,
    val data: T?
)

// --- Type Aliases ---

typealias MeloloListResponse = ApiResponse<List<MeloloBookContainer>>
typealias MeloloDetailResponse = ApiResponse<MeloloDetailData>
typealias MeloloStreamResponse = ApiResponse<MeloloStreamData>
typealias DramaboxListResponse = ApiResponse<List<MeloloBookContainer>>
typealias DramaboxDetailResponse = ApiResponse<DramaboxDetailData>
typealias DramaboxStreamResponse = ApiResponse<DramaboxStreamData>

// ReelShort & FreeReels reuse the same list format
typealias ReelshortListResponse = ApiResponse<List<MeloloBookContainer>>
typealias ReelshortDetailResponse = ApiResponse<ReelshortDetailData>
typealias ReelshortStreamResponse = ApiResponse<ReelshortStreamData>
typealias FreereelsListResponse = ApiResponse<List<MeloloBookContainer>>
typealias FreereelsDetailResponse = ApiResponse<FreereelsDetailData>
typealias FreereelsStreamResponse = ApiResponse<FreereelsStreamData>
typealias NetshortListResponse = ApiResponse<List<MeloloBookContainer>>
typealias NetshortDetailResponse = ApiResponse<NetshortDetailData>
typealias NetshortStreamResponse = ApiResponse<NetshortStreamData>
typealias MeloshortListResponse = ApiResponse<List<MeloloBookContainer>>
typealias MeloshortDetailResponse = ApiResponse<MeloshortDetailData>
typealias MeloshortStreamResponse = ApiResponse<MeloshortStreamData>
typealias GoodshortListResponse = ApiResponse<List<MeloloBookContainer>>
typealias GoodshortDetailResponse = ApiResponse<GoodshortDetailData>
typealias GoodshortStreamResponse = ApiResponse<GoodshortStreamData>
typealias DramawaveListResponse = ApiResponse<List<MeloloBookContainer>>
typealias DramawaveDetailResponse = ApiResponse<DramawaveDetailData>
typealias DramawaveStreamResponse = ApiResponse<DramawaveStreamData>

// --- Melolo Models ---

data class MeloloBrowseResponse(
    val author: String?,
    val message: String?,
    val books: List<MeloloBook>?
)

data class MeloloBookContainer(
    val books: List<MeloloBook>?
)

data class MeloloBook(
    @SerializedName("drama_name") val dramaName: String,
    @SerializedName("drama_id") val dramaId: String,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: String?,
    @SerializedName("watch_value") val watchValue: String?,
    @SerializedName("is_new_book") val isNewBook: String?,
    val language: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?
)

data class MeloloDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("video_list") val videoList: List<MeloloEpisode>?,
    val tags: List<String>?
)

data class MeloloStreamData(
    @SerializedName("video_id") val videoId: String?,
    val duration: Double?,
    val poster: String?,
    @SerializedName("expire_time") val expireTime: Long?,
    val qualities: List<MeloloStreamQuality>?
)

data class MeloloEpisode(
    val episode: Int,
    @SerializedName("video_id") val videoId: String,
    val duration: Int,
    val cover: String?
)

data class MeloloStreamQuality(
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val codec: String,
    val url: String
)

// --- Dramabox Models ---

data class DramaboxDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    val chapterList: List<DramaboxChapter>?,
    val tags: List<String>?
)

data class DramaboxChapter(
    val chapterId: String,
    val chapterIndex: Int
)

data class DramaboxStreamData(
    val bookId: String?,
    val chapterIndex: Int?,
    val videoUrl: String?,
    val cover: String?,
    val qualities: List<DramaboxStreamQuality>?
)

data class DramaboxStreamQuality(
    val quality: Int,
    val videoPath: String,
    val isDefault: Int = 0
)

// --- ReelShort Models ---

data class ReelshortDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?,
    @SerializedName("watch_value") val watchValue: String?,
    @SerializedName("video_list") val videoList: List<ReelshortEpisode>?
)

data class ReelshortEpisode(
    val index: Int?,
    val chapterId: String?,
    val title: String?,
    val isLocked: Boolean?,
    val serialNumber: Int?
)

data class ReelshortStreamData(
    val isLocked: Boolean?,
    val videoList: List<ReelshortVideo>?
)

data class ReelshortVideo(
    @SerializedName("playUrl") val playUrl: String?,
    val encode: String?,
    val dpi: Int?,
    val bitrate: String?
)

// --- FreeReels Models ---

data class FreereelsDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("watch_value") val watchValue: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?,
    val free: Boolean?,
    @SerializedName("episode_list") val episodeList: List<FreereelsEpisode>?
)

data class FreereelsEpisode(
    val episode: Int?,
    @SerializedName("episode_id") val episodeId: String?,
    val name: String?
)

data class FreereelsStreamData(
    @SerializedName("episode_id") val episodeId: String?,
    val name: String?,
    val cover: String?,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("m3u8_url") val m3u8Url: String?,
    @SerializedName("h264_m3u8") val h264M3u8: String?,
    @SerializedName("h265_m3u8") val h265M3u8: String?,
    val subtitles: List<FreereelsSubtitle>?
)

data class FreereelsSubtitle(
    val language: String?,
    val type: String?,
    val url: String?,
    @SerializedName("display_name") val displayName: String?
)

// --- NetShort Models ---

data class NetshortDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?,
    @SerializedName("video_list") val videoList: List<NetshortEpisode>?
)

data class NetshortEpisode(
    val episode: Int?,
    @SerializedName("episode_id") val episodeId: String?,
    val cover: String?,
    val isLocked: Boolean?
)

data class NetshortStreamData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?,
    @SerializedName("total_episodes") val totalEpisodes: Int?,
    @SerializedName("is_finished") val isFinished: Boolean?,
    val episodes: List<NetshortStreamEpisode>?
)

data class NetshortStreamEpisode(
    val episode: Int?,
    val stream: String?,
    val cover: String?,
    val isLocked: Boolean?,
    val subtitle: String?
)

// --- Meloshort Models ---

data class MeloshortDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("is_finished") val isFinished: Boolean?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?,
    @SerializedName("video_list") val videoList: List<MeloshortEpisode>?
)

data class MeloshortEpisode(
    val episode: Int?,
    @SerializedName("episode_id") val episodeId: String?,
    val cover: String?,
    val isLocked: Boolean?
)

data class MeloshortStreamData(
    @SerializedName("episode_id") val episodeId: String?,
    val videos: List<MeloshortStreamVideo>?,
    val subtitles: List<MeloshortSubtitle>?
)

data class MeloshortStreamVideo(
    val quality: String?,
    val url: String?
)

data class MeloshortSubtitle(
    val language: String?,
    val url: String?,
    val format: String?
)


// --- Goodshort Models ---

data class GoodshortDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("watch_value") val watchValue: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?
)

data class GoodshortStreamData(
    val bookId: String?,
    val bookName: String?,
    val introduction: String?,
    val bookCover: String?,
    val downloadList: List<GoodshortDownloadItem>?
)

data class GoodshortDownloadItem(
    val id: Long?,
    val index: Int?,
    val chapterName: String?,
    val image: String?,
    val bookId: String?,
    val price: Int?,
    val multiVideos: List<GoodshortStreamVideo>?
)

data class GoodshortStreamVideo(
    val type: String?,
    val filePath: String?,
    val fileSize: Long?
)

// --- DramaWave Models ---

data class DramawaveDetailData(
    @SerializedName("drama_id") val dramaId: String?,
    @SerializedName("drama_name") val dramaName: String?,
    val description: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    val tags: List<String>?,
    val episodeList: List<DramawaveEpisode>?
)

data class DramawaveEpisode(
    @SerializedName("episode_id") val episodeId: String?,
    val index: Int?,
    @SerializedName("episode_price") val episodePrice: Int?
)

data class DramawaveStreamData(
    @SerializedName("episode_id") val episodeId: String?,
    val name: String?,
    val cover: String?,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("m3u8_url") val m3u8Url: String?,
    @SerializedName("external_audio_h264_m3u8") val h264M3u8: String?,
    @SerializedName("external_audio_h265_m3u8") val h265M3u8: String?,
    @SerializedName("subtitle_list") val subtitleList: List<DramawaveSubtitle>?
)

data class DramawaveSubtitle(
    val language: String?,
    val type: String?,
    val subtitle: String?,
    val vtt: String?,
    @SerializedName("display_name") val displayName: String?
)

