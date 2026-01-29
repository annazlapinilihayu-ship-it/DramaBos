package com.sonzaix.dramabos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- New API Data Classes ---

data class MeloloResponse(
    val author: String?,
    val message: String?,
    @SerializedName("search_data") val searchData: List<MeloloBookContainer>?,
    @SerializedName("populer_data") val populerData: List<MeloloBookContainer>?,
    @SerializedName("home_data") val homeData: List<MeloloBookContainer>?,

    // Detail fields
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("video_list") val videoList: List<MeloloEpisode>?,

    // Stream fields
    @SerializedName("video_id") val videoId: String?,
    val duration: Double?,
    val poster: String?,
    @SerializedName("expire_time") val expireTime: Long?,
    val qualities: List<MeloloStreamQuality>?
)

data class MeloloBookContainer(
    val books: List<MeloloBook>?
)

data class MeloloBook(
    @SerializedName("drama_name") val dramaName: String,
    @SerializedName("drama_id") val dramaId: String,
    val description: String?,
    @SerializedName("create_time") val createTime: String?,
    @SerializedName("episode_count") val episodeCount: String?,
    @SerializedName("watch_value") val watchValue: String?,
    @SerializedName("is_new_book") val isNewBook: String?,
    val language: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    @SerializedName("stat_infos") val statInfos: List<String>?
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

// --- App Internal Data Classes (Mapped) ---

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
    val bookName: String, // Detail API doesn't return name, need to pass or fetch
    val cover: String?, // Detail API doesn't return cover (only episodes cover), might need to pass
    val introduction: String?, // Detail API doesn't return intro
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
    val qualities: List<VideoQuality>?
)

data class VideoQuality(val quality: Int, val videoPath: String, val isDefault: Int = 0)

data class LastWatched(
    val bookId: String,
    val bookName: String,
    val chapterIndex: Int,
    val cover: String?,
    val timestamp: Long,
    val source: String = "melolo",
    val position: Long = 0L
)

data class FavoriteDrama(
    val bookId: String,
    val bookName: String,
    val cover: String?,
    val source: String = "melolo",
    val totalEpisodes: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DramaDataStore(private val context: Context) {
    private val historykey = stringPreferencesKey("watch_history_list_v7")
    private val favoritesKey = stringPreferencesKey("favorite_list_v1")
    private val gson = Gson()

    suspend fun addToHistory(item: LastWatched) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[historykey] ?: "[]"
            val type = object : TypeToken<MutableList<LastWatched>>() {}.type
            val list: MutableList<LastWatched> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }

            val existingIndex = list.indexOfFirst { it.bookId == item.bookId }
            var itemToSave = item

            if (existingIndex != -1) {
                val oldItem = list[existingIndex]
                if (!oldItem.cover.isNullOrEmpty()) {
                    itemToSave = itemToSave.copy(cover = oldItem.cover)
                }
                list.removeAt(existingIndex)
            }

            list.add(0, itemToSave)

            if (list.size > 50) {
                list.subList(50, list.size).clear()
            }
            prefs[historykey] = gson.toJson(list)
        }
    }

    suspend fun removeHistoryItems(idsToRemove: List<String>) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[historykey] ?: "[]"
            val type = object : TypeToken<MutableList<LastWatched>>() {}.type
            val list: MutableList<LastWatched> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }

            list.removeAll { it.bookId in idsToRemove }
            prefs[historykey] = gson.toJson(list)
        }
    }

    val historyListFlow: Flow<List<LastWatched>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[historykey] ?: "[]"
        val type = object : TypeToken<List<LastWatched>>() {}.type
        try {
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(item: FavoriteDrama) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[favoritesKey] ?: "[]"
            val type = object : TypeToken<MutableList<FavoriteDrama>>() {}.type
            val list: MutableList<FavoriteDrama> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }

            val existingIndex = list.indexOfFirst { it.bookId == item.bookId }

            if (existingIndex != -1) {
                list.removeAt(existingIndex)
            } else {
                list.add(0, item)
            }

            prefs[favoritesKey] = gson.toJson(list)
        }
    }

    suspend fun removeFavoriteItems(idsToRemove: List<String>) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[favoritesKey] ?: "[]"
            val type = object : TypeToken<MutableList<FavoriteDrama>>() {}.type
            val list: MutableList<FavoriteDrama> = try {
                gson.fromJson(jsonString, type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }

            list.removeAll { it.bookId in idsToRemove }
            prefs[favoritesKey] = gson.toJson(list)
        }
    }

    val favoritesListFlow: Flow<List<FavoriteDrama>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[favoritesKey] ?: "[]"
        val type = object : TypeToken<List<FavoriteDrama>>() {}.type
        try {
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

interface DramaApiService {
    @GET("melolo/home")
    suspend fun getHome(): MeloloResponse

    @GET("melolo/populer")
    suspend fun getPopuler(): MeloloResponse

    @GET("melolo/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("result") result: Int = 30,
        @Query("page") page: Int
    ): MeloloResponse

    @GET("melolo/detail/{id}")
    suspend fun getDetail(@Path("id") id: String): MeloloResponse

    @GET("melolo/stream/{id}")
    suspend fun getStream(@Path("id") id: String): MeloloResponse
}

object RetrofitClient {
    val api: DramaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.sonzaix.indevs.in/")
            .client(OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DramaApiService::class.java)
    }
}

class DramaRepository {
    private val api = RetrofitClient.api

    private fun mapBookToDramaItem(book: MeloloBook): DramaItem {
        return DramaItem(
            bookId = book.dramaId,
            bookName = book.dramaName,
            cover = book.thumbUrl,
            introduction = book.description,
            playCount = book.watchValue,
            chapterCount = book.episodeCount?.toIntOrNull() ?: 0,
            tags = book.statInfos,
            timestamp = System.currentTimeMillis(),
            source = "melolo",
            isNew = book.isNewBook == "1"
        )
    }

    fun getHome() = flow {
        try {
            val res = api.getHome()
            val list = res.homeData?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it) } ?: emptyList()
            emit(Result.success(DramaListContainer(list, isMore = false, total = list.size)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getPopuler() = flow {
        try {
            val res = api.getPopuler()
            val list = res.populerData?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it) } ?: emptyList()
            emit(Result.success(DramaListContainer(list, isMore = false, total = list.size)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun search(query: String, page: Int) = flow {
        try {
            val res = api.search(query, page = page)
            val list = res.searchData?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it) } ?: emptyList()
            // Assume if list is not empty, there might be more pages, although API doesn't explicitly say total pages.
            // But usually search APIs behave this way.
            emit(Result.success(DramaListContainer(list, isMore = list.isNotEmpty())))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Detail API doesn't return info like name, cover, intro. We need to handle this.
    // UI might already have this info from the list.
    // Ideally we should cache or pass it. But here we just fetch chapters.
    fun getDetail(id: String) = flow {
        try {
            val res = api.getDetail(id)
            val chapters = res.videoList?.map { ep ->
                Chapter(
                    chapterId = ep.videoId,
                    chapterIndex = ep.episode - 1, // 1-based to 0-based
                    chapterName = "Episode ${ep.episode}",
                    isCharge = 0,
                    vid = ep.videoId,
                    durationSeconds = ep.duration
                )
            } ?: emptyList()

            // Since API detail doesn't return book metadata, we return placeholder or what we have.
            // In the ViewModel/UI we might need to rely on passed arguments or cached data for Name/Cover.
            // But let's check if we can get it from somewhere else? No.
            // We will return empty strings for metadata and hope UI handles it or uses passed args.
            val detail = DramaDetail(
                bookId = id,
                bookName = "", // Missing in API
                cover = "", // Missing in API
                introduction = "", // Missing in API
                tags = emptyList(),
                chapterList = chapters,
                source = "melolo"
            )
            emit(Result.success(detail))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getVideo(bookId: String, index: Int, bookName: String) = flow {
        try {
            // We need to find the video ID for the given index.
            // So we first fetch detail to get the list, then find the video ID.
            val detailRes = api.getDetail(bookId)
            val targetEp = detailRes.videoList?.find { (it.episode - 1) == index }

            if (targetEp != null) {
                val streamRes = api.getStream(targetEp.videoId)

                val qualities = streamRes.qualities?.map { q ->
                    // Extract numeric quality from label (e.g., "720p" -> 720)
                    val qInt = q.label.filter { it.isDigit() }.toIntOrNull() ?: 480
                    VideoQuality(qInt, q.url, 0)
                } ?: emptyList()

                // Try to find a default URL if qualities are empty but we have a url in qualities?
                // The JSON shows `qualities` list with `url`.
                // There is no top-level `url` in stream response example, only `video_id`, `duration`, `poster`.
                // Wait, look at JSON example for stream:
                /*
                {
                  ...
                  "qualities": [ ... ]
                }
                */
                // So we rely on qualities.

                val finalUrl = qualities.firstOrNull()?.videoPath ?: ""

                if (finalUrl.isNotEmpty()) {
                     val videoData = VideoData(
                        bookId = bookId,
                        chapterIndex = index,
                        videoUrl = finalUrl,
                        cover = streamRes.poster,
                        qualities = qualities
                    )
                    emit(Result.success(videoData))
                } else {
                    emit(Result.failure(Exception("Stream URL not found")))
                }

            } else {
                emit(Result.failure(Exception("Episode not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
