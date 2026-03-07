package com.sonzaix.shortxrama.data

import retrofit2.http.*

interface DramaApiService {
    @GET("melolo/home")
    suspend fun getHome(
        @Query("page") page: Int = 1
    ): MeloloListResponse

    @GET("dramabox/home")
    suspend fun getDramaboxHome(
        @Query("page") page: Int = 1
    ): DramaboxListResponse

    @GET("melolo/populer")
    suspend fun getPopuler(
        @Query("page") page: Int = 1
    ): MeloloListResponse

    @GET("dramabox/populer")
    suspend fun getDramaboxPopuler(
        @Query("page") page: Int = 1
    ): DramaboxListResponse

    @GET("dramabox/new")
    suspend fun getDramaboxNew(
        @Query("page") page: Int = 1
    ): DramaboxListResponse

    @GET("melolo/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("result") result: Int = 30,
        @Query("page") page: Int
    ): MeloloListResponse

    @GET("dramabox/search")
    suspend fun searchDramabox(
        @Query("q") q: String,
        @Query("result") result: Int = 30,
        @Query("page") page: Int
    ): DramaboxListResponse

    @GET("melolo/detail/{id}")
    suspend fun getDetail(@Path("id") id: String): MeloloDetailResponse

    @GET("dramabox/detail/{id}")
    suspend fun getDramaboxDetail(@Path("id") id: String): DramaboxDetailResponse

    @GET("melolo/stream/{id}")
    suspend fun getStream(@Path("id") id: String): MeloloStreamResponse

    @GET("dramabox/stream")
    suspend fun getDramaboxStream(
        @Query("dramaId") dramaId: String,
        @Query("episodeIndex") episodeIndex: Int
    ): DramaboxStreamResponse

    @GET("melolo/browse")
    suspend fun getBrowse(
        @Query("result") result: Int = 200
    ): MeloloBrowseResponse

    // --- ReelShort ---
    @GET("reelshort/home")
    suspend fun getReelshortHome(): ReelshortListResponse

    @GET("reelshort/populer")
    suspend fun getReelshortPopuler(): ReelshortListResponse

    @GET("reelshort/new")
    suspend fun getReelshortNew(): ReelshortListResponse

    @GET("reelshort/search")
    suspend fun searchReelshort(
        @Query("q") q: String,
        @Query("page") page: Int = 1
    ): ReelshortListResponse

    @GET("reelshort/detail")
    suspend fun getReelshortDetail(
        @Query("bookId") bookId: String
    ): ReelshortDetailResponse

    @GET("reelshort/stream")
    suspend fun getReelshortStream(
        @Query("bookId") bookId: String,
        @Query("chapterId") chapterId: String
    ): ReelshortStreamResponse

    // --- FreeReels ---
    @GET("freereels/home")
    suspend fun getFreereelsHome(
        @Query("page") page: Int = 1
    ): FreereelsListResponse

    @GET("freereels/populer")
    suspend fun getFreereelsPopuler(
        @Query("page") page: Int = 1
    ): FreereelsListResponse

    @GET("freereels/new")
    suspend fun getFreereelsNew(
        @Query("page") page: Int = 1
    ): FreereelsListResponse

    @GET("freereels/search")
    suspend fun searchFreereels(
        @Query("q") q: String,
        @Query("page") page: Int = 1
    ): FreereelsListResponse

    @GET("freereels/detail")
    suspend fun getFreereelsDetail(
        @Query("dramaId") dramaId: String
    ): FreereelsDetailResponse

    @GET("freereels/stream")
    suspend fun getFreereelsStream(
        @Query("dramaId") dramaId: String,
        @Query("episode") episode: Int
    ): FreereelsStreamResponse

    // --- NetShort ---
    @GET("netshort/home")
    suspend fun getNetshortHome(
        @Query("page") page: Int = 1
    ): NetshortListResponse

    @GET("netshort/populer")
    suspend fun getNetshortPopuler(
        @Query("page") page: Int = 1
    ): NetshortListResponse

    @GET("netshort/new")
    suspend fun getNetshortNew(): NetshortListResponse

    @GET("netshort/search")
    suspend fun searchNetshort(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): NetshortListResponse

    @GET("netshort/detail")
    suspend fun getNetshortDetail(
        @Query("dramaId") dramaId: String
    ): NetshortDetailResponse

    @GET("netshort/stream")
    suspend fun getNetshortStream(
        @Query("dramaId") dramaId: String
    ): NetshortStreamResponse

    // --- MeloShort Endpoints ---

    @GET("meloshort/home")
    suspend fun getMeloshortHome(
        @Query("page") page: Int
    ): MeloshortListResponse

    @GET("meloshort/populer")
    suspend fun getMeloshortPopuler(): MeloshortListResponse

    @GET("meloshort/new")
    suspend fun getMeloshortNew(
        @Query("page") page: Int
    ): MeloshortListResponse

    @GET("meloshort/search")
    suspend fun searchMeloshort(
        @Query("query") query: String
    ): MeloshortListResponse

    @GET("meloshort/detail")
    suspend fun getMeloshortDetail(
        @Query("dramaId") dramaId: String
    ): MeloshortDetailResponse

    @GET("meloshort/stream")
    suspend fun getMeloshortStream(
        @Query("dramaId") dramaId: String,
        @Query("episodeId") episodeId: String
    ): MeloshortStreamResponse

    // --- GoodShort Endpoints ---

    @GET("goodshort/home")
    suspend fun getGoodshortHome(
        @Query("page") page: Int = 1
    ): GoodshortListResponse

    @GET("goodshort/populer")
    suspend fun getGoodshortPopuler(
        @Query("page") page: Int = 1
    ): GoodshortListResponse

    @GET("goodshort/new")
    suspend fun getGoodshortNew(
        @Query("page") page: Int = 1
    ): GoodshortListResponse

    @GET("goodshort/search")
    suspend fun searchGoodshort(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): GoodshortListResponse

    @GET("goodshort/detail")
    suspend fun getGoodshortDetail(
        @Query("bookId") bookId: String
    ): GoodshortDetailResponse

    @GET("goodshort/stream")
    suspend fun getGoodshortStream(
        @Query("bookId") bookId: String
    ): GoodshortStreamResponse

    // --- DramaWave Endpoints ---

    @GET("dramawave/home")
    suspend fun getDramawaveHome(
        @Query("page") page: Int = 1
    ): DramawaveListResponse

    @GET("dramawave/populer")
    suspend fun getDramawavePopuler(
        @Query("page") page: Int = 1
    ): DramawaveListResponse

    @GET("dramawave/new")
    suspend fun getDramawaveNew(
        @Query("page") page: Int = 1
    ): DramawaveListResponse

    @GET("dramawave/search")
    suspend fun searchDramawave(
        @Query("q") q: String,
        @Query("page") page: Int = 1
    ): DramawaveListResponse

    @GET("dramawave/detail")
    suspend fun getDramawaveDetail(
        @Query("id") id: String
    ): DramawaveDetailResponse

    @GET("dramawave/stream")
    suspend fun getDramawaveStream(
        @Query("dramaId") dramaId: String,
        @Query("episode") episode: Int
    ): DramawaveStreamResponse
}
