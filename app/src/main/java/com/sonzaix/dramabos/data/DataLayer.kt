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


data class MaintenanceResponse(val isMT: String)
data class BaseResponse<T>(val success: Boolean, val data: T?, val meta: Meta? = null)
data class Meta(val source: String?)

data class MeloloBaseResponse<T>(
    val code: Int,
    val data: T?,
    val hasmore: Boolean?
)

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
    @SerializedName(value = "chapterCount", alternate = ["chapter_count", "total_chapter", "episodes", "items_count"])
    val chapterCount: Int? = 0,
    val tags: List<String>?,
    val timestamp: Long?,
    val source: String = "dramabox"
)

data class MeloloItem(
    val id: String,
    val name: String,
    val cover: String,
    val author: String,
    val episodes: Int,
    val intro: String
)

data class DramaDetail(
    val bookId: String,
    val bookName: String,
    val cover: String,
    val introduction: String,
    val tags: List<String>?,
    val chapterList: List<Chapter>,
    val source: String = "dramabox"
)

data class ChapterListResponse(val success: Boolean, val data: ChapterDataWrapper)
data class ChapterDataWrapper(val chapterList: List<Chapter>)

data class Chapter(
    val chapterId: String,
    val chapterIndex: Int,
    val chapterName: String?,
    val isCharge: Int,
    val vid: String? = null,
    val durationSeconds: Int = 0
)

data class MeloloDetailRoot(
    val code: Int,
    val id: String?,
    val title: String?,
    val episodes: Int?,
    val cover: String?,
    val intro: String?,
    val videos: List<MeloloVideoInfo>?
)

data class MeloloVideoInfo(
    val vid: String,
    val episode: Int,
    val duration: Int
)

data class VideoData(
    val bookId: String,
    val chapterIndex: Int,
    val videoUrl: String,
    val cover: String?,
    val qualities: List<VideoQuality>?
)

data class VideoQuality(val quality: Int, val videoPath: String, val isDefault: Int = 0)

data class MeloloVideoResponse(
    val url: String?,
    val backup: String?,
    val list: List<MeloloQuality>?
)
data class MeloloQuality(val definition: String, val url: String)

data class LastWatched(
    val bookId: String,
    val bookName: String,
    val chapterIndex: Int,
    val cover: String?,
    val timestamp: Long,
    val source: String = "dramabox",
    val position: Long = 0L
)

data class FavoriteDrama(
    val bookId: String,
    val bookName: String,
    val cover: String?,
    val source: String = "dramabox",
    val totalEpisodes: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

data class MonitorResponse(
    val status: String,
    val sessions: Int?,
    val timestamp: Long?
)

data class MeloloHealthResponse(
    val status: String
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
    @GET("dramabox/api/foryou/{page}?lang=in&pageSize=30") suspend fun getForYou(@Path("page") p: Int): BaseResponse<DramaListContainer>
    @GET("dramabox/api/new/{page}?lang=in") suspend fun getNew(@Path("page") p: Int): BaseResponse<DramaListContainer>
    @GET("dramabox/api/rank/{page}?lang=in") suspend fun getRank(@Path("page") p: Int): BaseResponse<DramaListContainer>

    @GET("dramabox/api/search/{keyword}/{page}?lang=in&pageSize=30") suspend fun searchDrama(@Path("keyword") k: String, @Path("page") p: Int): BaseResponse<DramaListContainer>

    @GET("dramabox/api/suggest/{keyword}?lang=in") suspend fun getSuggestions(@Path("keyword") k: String): BaseResponse<DramaListContainer>
    @GET("dramabox/api/drama/{bookId}?lang=in") suspend fun getDetail(@Path("bookId") id: String): BaseResponse<DramaDetail>
    @GET("dramabox/api/chapters/{bookId}?lang=in") suspend fun getChapters(@Path("bookId") id: String): ChapterListResponse
    @GET("dramabox/api/watch/player?lang=in") suspend fun getVideoUrl(@Query("bookId") id: String, @Query("index") idx: Int): BaseResponse<VideoData>
    @GET("dramabox/api/monitor") suspend fun checkMonitor(): MonitorResponse
    @GET suspend fun checkMaintenance(@Url url: String): MaintenanceResponse

    @GET("melolo/api/v1/home")
    suspend fun getMeloloHome(
        @Query("offset") offset: Int,
        @Query("count") count: Int = 30,
        @Query("lang") lang: String = "id"
    ): MeloloBaseResponse<List<MeloloItem>>

    @GET("melolo/api/v1/search")
    suspend fun searchMelolo(
        @Query("q") q: String,
        @Query("offset") offset: Int,
        @Query("count") count: Int = 30,
        @Query("lang") lang: String = "id"
    ): MeloloBaseResponse<List<MeloloItem>>

    @GET("melolo/api/v1/detail/{id}?lang=id")
    suspend fun getMeloloDetail(@Path("id") id: String): MeloloDetailRoot

    @GET("melolo/api/v1/video/{vid}?lang=id")
    suspend fun getMeloloVideo(@Path("vid") vid: String): MeloloVideoResponse

    @GET("melolo/health") suspend fun checkMeloloHealth(): MeloloHealthResponse
}

object RetrofitClient {
    val api: DramaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://dramabos.asia/api/")
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

    fun checkAppMaintenance() = flow {
        try {
            val url = "https://raw.githubusercontent.com/sonzaiekkusu/sonzaix-files/refs/heads/main/drama.json"
            val res = api.checkMaintenance(url)
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun mapMeloloToDramaItem(m: MeloloItem): DramaItem {
        return DramaItem(
            bookId = m.id,
            bookName = m.name,
            cover = m.cover,
            introduction = m.intro,
            playCount = "${m.episodes} Eps",
            chapterCount = m.episodes,
            tags = listOf(m.author),
            timestamp = System.currentTimeMillis(),
            source = "melolo"
        )
    }

    fun getForYouCombined(page: Int, source: String) = flow {
        try {
            val resultList = mutableListOf<DramaItem>()

            if (source == "dramabox" || source == "all") {
                try {
                    val dbList = api.getForYou(page).data?.list ?: emptyList()
                    resultList.addAll(dbList)
                } catch (e: Exception) { e.printStackTrace() }
            }

            if (source == "melolo" || source == "all") {
                try {
                    val meloloOffset = (page - 1) * 30
                    val mList = api.getMeloloHome(meloloOffset).data?.map { mapMeloloToDramaItem(it) } ?: emptyList()
                    resultList.addAll(mList)
                } catch (e: Exception) { e.printStackTrace() }
            }

            if (page == 1 && source == "all") resultList.shuffle()

            emit(Result.success(DramaListContainer(resultList, isMore = true, total = resultList.size)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getNew(p: Int) = flow { try { emit(Result.success(api.getNew(p).data)) } catch(e:Exception){ emit(Result.failure(e)) } }
    fun getRank(p: Int) = flow { try { emit(Result.success(api.getRank(p).data)) } catch(e:Exception){ emit(Result.failure(e)) } }

    fun searchCombined(query: String, page: Int, useDramabox: Boolean, useMelolo: Boolean) = flow {
        val finalResults = mutableListOf<DramaItem>()
        var error: Throwable? = null

        if (useDramabox) {
            try {
                val res = api.searchDrama(query, page).data
                res?.list?.let { finalResults.addAll(it) }
            } catch (e: Exception) { error = e }
        }

        if (useMelolo) {
            try {
                val offset = (page - 1) * 30
                val res = api.searchMelolo(query, offset)
                val mList = res.data?.map { mapMeloloToDramaItem(it) } ?: emptyList()
                finalResults.addAll(mList)
            } catch (e: Exception) { error = e }
        }

        if (finalResults.isNotEmpty()) {
            emit(Result.success(DramaListContainer(finalResults, isMore = true)))
        } else {
            if (page == 1 && error != null && finalResults.isEmpty()) {
                emit(Result.failure(error!!))
            } else {
                emit(Result.success(DramaListContainer(emptyList(), isMore = false)))
            }
        }
    }

    fun getSuggestions(k: String) = flow { try { emit(Result.success(api.getSuggestions(k).data?.list)) } catch(e:Exception){ emit(Result.failure(e)) } }

    fun getDetail(id: String, source: String) = flow {
        try {
            if (source == "melolo") {
                val mData = api.getMeloloDetail(id)
                if (!mData.id.isNullOrEmpty()) {
                    val chapters = mData.videos?.map { v ->
                        Chapter(
                            chapterId = v.vid,
                            chapterIndex = v.episode - 1,
                            chapterName = "Episode ${v.episode}",
                            isCharge = 0,
                            vid = v.vid,
                            durationSeconds = v.duration
                        )
                    } ?: emptyList()

                    val detail = DramaDetail(
                        bookId = mData.id,
                        bookName = mData.title ?: "",
                        cover = mData.cover ?: "",
                        introduction = mData.intro ?: "",
                        tags = listOf("Melolo"),
                        chapterList = chapters,
                        source = "melolo"
                    )
                    emit(Result.success(detail))
                } else {
                    emit(Result.failure(Exception("Data Melolo kosong/tidak valid")))
                }
            } else {
                val detailRes = api.getDetail(id).data
                if (detailRes != null) {
                    val chapterRes = try { api.getChapters(id).data.chapterList } catch(_:Exception) { detailRes.chapterList }
                    emit(Result.success(detailRes.copy(chapterList = chapterRes, source = "dramabox")))
                } else {
                    emit(Result.failure(Exception("Data kosong")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getVideo(bookId: String, index: Int, source: String) = flow {
        try {
            if (source == "melolo") {
                val detailRes = api.getMeloloDetail(bookId)
                val targetVideo = detailRes.videos?.find { (it.episode - 1) == index }

                if (targetVideo != null) {
                    val vidRes = api.getMeloloVideo(targetVideo.vid)
                    val qualities = mutableListOf<VideoQuality>()

                    if (!vidRes.url.isNullOrBlank()) {
                        qualities.add(VideoQuality(1080, vidRes.url, 1))
                    }
                    if (!vidRes.backup.isNullOrBlank()) {
                        qualities.add(VideoQuality(720, vidRes.backup, 0))
                    }
                    vidRes.list?.forEach { q ->
                        val qInt = q.definition.filter { it.isDigit() }.toIntOrNull() ?: 480
                        if (q.url.isNotBlank()) {
                            qualities.add(VideoQuality(qInt, q.url, 0))
                        }
                    }

                    val finalUrl = when {
                        !vidRes.url.isNullOrBlank() -> vidRes.url
                        !vidRes.backup.isNullOrBlank() -> vidRes.backup
                        qualities.isNotEmpty() -> qualities.first().videoPath
                        else -> ""
                    }

                    val uniqueQualities = qualities.distinctBy { it.quality }

                    if (finalUrl.isNotEmpty()) {
                        val videoData = VideoData(
                            bookId = bookId,
                            chapterIndex = index,
                            videoUrl = finalUrl,
                            cover = detailRes.cover,
                            qualities = uniqueQualities
                        )
                        emit(Result.success(videoData))
                    } else {
                        emit(Result.failure(Exception("URL Video tidak tersedia/kosong")))
                    }
                } else {
                    emit(Result.failure(Exception("Episode tidak ditemukan")))
                }
            } else {
                val res = api.getVideoUrl(bookId, index).data
                if (res != null) {
                    if (res.videoUrl.isNotBlank() || !res.qualities.isNullOrEmpty()) {
                        emit(Result.success(res))
                    } else {
                        emit(Result.failure(Exception("Link video dramabox kosong")))
                    }
                }
                else emit(Result.failure(Exception("Gagal load video")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun checkMonitorStatus() = flow {
        try {
            val res = api.checkMonitor()
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun checkMeloloStatus() = flow {
        try {
            val res = api.checkMeloloHealth()
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}