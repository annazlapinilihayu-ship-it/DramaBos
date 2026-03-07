package com.sonzaix.shortxrama.data

import kotlinx.coroutines.flow.*
import java.net.URLEncoder

object DramaRepository {
    private val api = RetrofitClient.api

    fun pickPreferredQuality(
        available: List<VideoQuality>,
        preferred: Int,
        preferredCodec: String? = null
    ): VideoQuality? {
        if (available.isEmpty()) return null
        val target = if (preferred <= 0) 720 else preferred
        val sorted = available.sortedBy { it.quality }
        val candidateQuality = sorted.lastOrNull { it.quality <= target }?.quality
            ?: sorted.firstOrNull { it.quality >= target }?.quality
            ?: sorted.firstOrNull()?.quality
        if (candidateQuality == null) return null
        val candidates = available.filter { it.quality == candidateQuality }
        if (!preferredCodec.isNullOrEmpty()) {
            val codecMatch = candidates.find { it.codec.equals(preferredCodec, ignoreCase = true) }
            if (codecMatch != null) return codecMatch
        }
        return candidates.sortedWith(
            compareByDescending<VideoQuality> { it.codec?.equals("H264", ignoreCase = true) == true }
        ).firstOrNull()
    }

    private fun proxyNetshortCover(url: String?): String? {
        if (url.isNullOrEmpty()) return url
        return "https://wsrv.nl/?url=${java.net.URLEncoder.encode(url, "UTF-8")}&w=540&h=720&fit=cover&output=webp"
    }

    private fun mapBookToDramaItem(book: MeloloBook, source: String): DramaItem {
        val tags = book.tags
        val coverUrl = if (isNetshort(source)) proxyNetshortCover(book.thumbUrl) else book.thumbUrl
        return DramaItem(
            bookId = book.dramaId,
            bookName = book.dramaName,
            cover = coverUrl,
            introduction = book.description,
            playCount = book.watchValue,
            chapterCount = book.episodeCount?.toIntOrNull() ?: 0,
            tags = tags,
            timestamp = System.currentTimeMillis(),
            source = source,
            isNew = source == "melolo" && book.isNewBook == "1"
        )
    }

    private fun isDramabox(source: String) = source.lowercase() == "dramabox"
    private fun isReelshort(source: String) = source.lowercase() == "reelshort"
    private fun isFreereels(source: String) = source.lowercase() == "freereels"
    private fun isNetshort(source: String) = source.lowercase() == "netshort"
    private fun isMeloshort(source: String) = source.lowercase() == "meloshort"
    private fun isGoodshort(source: String) = source.lowercase() == "goodshort"
    private fun isDramawave(source: String) = source.lowercase() == "dramawave"

    fun getHome(page: Int, source: String = "melolo") = flow {
        try {
            val res = when {
                isDramabox(source) -> api.getDramaboxHome(page = page)
                isReelshort(source) -> api.getReelshortHome()
                isFreereels(source) -> api.getFreereelsHome(page = page)
                isNetshort(source) -> api.getNetshortHome(page = page)
                isMeloshort(source) -> api.getMeloshortHome(page = page)
                isGoodshort(source) -> api.getGoodshortHome(page = page)
                isDramawave(source) -> api.getDramawaveHome(page = page)
                else -> api.getHome(page = page)
            }
            val list = res.data?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it, source) } ?: emptyList()
            val supportsPage = !isReelshort(source)
            emit(Result.success(DramaListContainer(list, isMore = supportsPage && list.isNotEmpty(), total = list.size)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getPopuler(page: Int, source: String = "melolo") = flow {
        try {
            val res = when {
                isDramabox(source) -> api.getDramaboxPopuler(page = page)
                isReelshort(source) -> api.getReelshortPopuler()
                isFreereels(source) -> api.getFreereelsPopuler(page = page)
                isNetshort(source) -> api.getNetshortPopuler(page = page)
                isMeloshort(source) -> api.getMeloshortPopuler()
                isGoodshort(source) -> api.getGoodshortPopuler(page = page)
                isDramawave(source) -> api.getDramawavePopuler(page = page)
                else -> api.getPopuler(page = page)
            }
            val list = res.data?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it, source) } ?: emptyList()
            val supportsPage = !isReelshort(source)
            emit(Result.success(DramaListContainer(list, isMore = supportsPage && list.isNotEmpty(), total = list.size)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getNew(page: Int, source: String = "dramabox") = flow {
        try {
            val res = when {
                isDramabox(source) -> api.getDramaboxNew(page = page)
                isReelshort(source) -> api.getReelshortNew()
                isFreereels(source) -> api.getFreereelsNew(page = page)
                isNetshort(source) -> api.getNetshortNew()
                isMeloshort(source) -> api.getMeloshortNew(page = page)
                isGoodshort(source) -> api.getGoodshortNew(page = page)
                isDramawave(source) -> api.getDramawaveNew(page = page)
                else -> api.getPopuler(page = page)
            }
            val newList = res.data?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it, source) } ?: emptyList()
            val list = if (newList.isNotEmpty()) newList else emptyList()
            val supportsPage = !isReelshort(source) && !isNetshort(source)
            emit(Result.success(DramaListContainer(list, isMore = supportsPage && list.isNotEmpty(), total = list.size)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun search(query: String, page: Int, result: Int = 30, source: String = "melolo") = flow {
        try {
            val res = when {
                isDramabox(source) -> api.searchDramabox(query, result = result, page = page)
                isReelshort(source) -> api.searchReelshort(query, page = page)
                isFreereels(source) -> api.searchFreereels(query, page = page)
                isNetshort(source) -> {
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    api.searchNetshort(encodedQuery, page = page)
                }
                isMeloshort(source) -> api.searchMeloshort(query)
                isGoodshort(source) -> api.searchGoodshort(query, page = page)
                isDramawave(source) -> api.searchDramawave(query, page = page)
                else -> api.search(query, result = result, page = page)
            }
            val list = res.data?.flatMap { it.books ?: emptyList() }?.map { mapBookToDramaItem(it, source) } ?: emptyList()
            val hasMore = if (isFreereels(source)) false else list.isNotEmpty()
            emit(Result.success(DramaListContainer(list, isMore = hasMore)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getDetail(id: String, source: String = "melolo", bookName: String = "", cover: String = "", intro: String = "") = flow {
        try {
            if (isDramabox(source)) {
                val res = api.getDramaboxDetail(id)
                val detailData = res.data
                val chapters = detailData?.chapterList?.map { ep ->
                    Chapter(
                        chapterId = ep.chapterId,
                        chapterIndex = ep.chapterIndex,
                        chapterName = "Episode ${ep.chapterIndex + 1}",
                        isCharge = 0,
                        vid = ep.chapterId,
                        durationSeconds = 0
                    )
                } ?: emptyList()

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: id,
                    bookName = detailData?.dramaName ?: bookName,
                    cover = cover,
                    introduction = detailData?.description ?: intro,
                    tags = detailData?.tags,
                    chapterList = chapters,
                    source = "dramabox"
                )
                emit(Result.success(detail))
                return@flow
            }

            if (isReelshort(source)) {
                val res = api.getReelshortDetail(id)
                val detailData = res.data
                val chapters = detailData?.videoList?.map { ep ->
                    Chapter(
                        chapterId = ep.chapterId ?: "",
                        chapterIndex = (ep.index ?: ep.serialNumber ?: 1) - 1,
                        chapterName = ep.title ?: "Episode ${ep.serialNumber ?: ep.index ?: 1}",
                        isCharge = if (ep.isLocked == true) 1 else 0,
                        vid = ep.chapterId,
                        durationSeconds = 0
                    )
                } ?: emptyList()

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: id,
                    bookName = detailData?.dramaName ?: bookName,
                    cover = detailData?.thumbUrl ?: cover,
                    introduction = detailData?.description ?: intro,
                    tags = detailData?.tags,
                    chapterList = chapters,
                    source = "reelshort"
                )
                emit(Result.success(detail))
                return@flow
            }

            if (isFreereels(source)) {
                val res = api.getFreereelsDetail(id)
                val detailData = res.data
                val chapters = detailData?.episodeList?.map { ep ->
                    Chapter(
                        chapterId = ep.episodeId ?: "",
                        chapterIndex = (ep.episode ?: 1) - 1,
                        chapterName = ep.name ?: "Episode ${ep.episode ?: 1}",
                        isCharge = 0,
                        vid = ep.episodeId,
                        durationSeconds = 0
                    )
                } ?: emptyList()

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: id,
                    bookName = detailData?.dramaName ?: bookName,
                    cover = detailData?.thumbUrl ?: cover,
                    introduction = detailData?.description ?: intro,
                    tags = detailData?.tags,
                    chapterList = chapters,
                    source = "freereels"
                )
                emit(Result.success(detail))
                return@flow
            }

            if (isNetshort(source)) {
                val res = api.getNetshortDetail(id)
                val detailData = res.data
                val chapters = detailData?.videoList?.map { ep ->
                    Chapter(
                        chapterId = ep.episodeId ?: "",
                        chapterIndex = (ep.episode ?: 1) - 1,
                        chapterName = "Episode ${ep.episode ?: 1}",
                        isCharge = if (ep.isLocked == true) 1 else 0,
                        vid = ep.episodeId,
                        durationSeconds = 0
                    )
                } ?: emptyList()

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: id,
                    bookName = detailData?.dramaName ?: bookName,
                    cover = proxyNetshortCover(detailData?.thumbUrl ?: cover) ?: cover,
                    introduction = detailData?.description ?: intro,
                    tags = detailData?.tags,
                    chapterList = chapters,
                    source = "netshort"
                )
                emit(Result.success(detail))
                return@flow
            }

            if (isMeloshort(source)) {
                val res = api.getMeloshortDetail(id)
                val detailData = res.data
                val chapters = detailData?.videoList?.map { ep ->
                    Chapter(
                        chapterId = ep.episodeId ?: "",
                        chapterIndex = (ep.episode ?: 1) - 1,
                        chapterName = "Episode ${ep.episode ?: 1}",
                        isCharge = if (ep.isLocked == true) 1 else 0,
                        vid = ep.episodeId,
                        durationSeconds = 0
                    )
                } ?: emptyList()

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: id,
                    bookName = detailData?.dramaName ?: bookName,
                    cover = detailData?.thumbUrl ?: cover,
                    introduction = detailData?.description ?: intro,
                    tags = detailData?.tags,
                    chapterList = chapters,
                    source = "meloshort"
                )
                emit(Result.success(detail))
                return@flow
            }

            if (isGoodshort(source)) {
                val detailRes = api.getGoodshortDetail(id)
                val detailData = detailRes.data
                val streamRes = api.getGoodshortStream(id)
                val streamData = streamRes.data

                val chaptersFromStream = streamData?.downloadList
                    ?.mapNotNull { ep ->
                        val epIndex = ep.index ?: return@mapNotNull null
                        Chapter(
                            chapterId = ep.id?.toString() ?: "${id}_$epIndex",
                            chapterIndex = epIndex,
                            chapterName = ep.chapterName ?: "Episode ${epIndex + 1}",
                            isCharge = if ((ep.price ?: 0) > 0) 1 else 0,
                            vid = ep.id?.toString(),
                            durationSeconds = 0
                        )
                    }
                    ?.sortedBy { it.chapterIndex }
                    ?: emptyList()

                val fallbackCount = detailData?.episodeCount ?: 0
                val fallbackChapters = if (chaptersFromStream.isEmpty() && fallbackCount > 0) {
                    (0 until fallbackCount).map { idx ->
                        Chapter(
                            chapterId = "${id}_$idx",
                            chapterIndex = idx,
                            chapterName = "Episode ${idx + 1}",
                            isCharge = 0,
                            vid = null,
                            durationSeconds = 0
                        )
                    }
                } else {
                    emptyList()
                }

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: streamData?.bookId ?: id,
                    bookName = detailData?.dramaName ?: streamData?.bookName ?: bookName,
                    cover = detailData?.thumbUrl ?: streamData?.bookCover ?: cover,
                    introduction = detailData?.description ?: streamData?.introduction ?: intro,
                    tags = detailData?.tags,
                    chapterList = if (chaptersFromStream.isNotEmpty()) chaptersFromStream else fallbackChapters,
                    source = "goodshort"
                )
                emit(Result.success(detail))
                return@flow
            }

            if (isDramawave(source)) {
                val res = api.getDramawaveDetail(id)
                val detailData = res.data
                val chapters = detailData?.episodeList?.map { ep ->
                    Chapter(
                        chapterId = ep.episodeId ?: "",
                        chapterIndex = (ep.index ?: 1) - 1,
                        chapterName = "Episode ${ep.index ?: 1}",
                        isCharge = if ((ep.episodePrice ?: 0) > 0) 1 else 0,
                        vid = ep.episodeId,
                        durationSeconds = 0
                    )
                } ?: emptyList()

                val detail = DramaDetail(
                    bookId = detailData?.dramaId ?: id,
                    bookName = detailData?.dramaName ?: bookName,
                    cover = detailData?.thumbUrl ?: cover,
                    introduction = detailData?.description ?: intro,
                    tags = detailData?.tags,
                    chapterList = chapters,
                    source = "dramawave"
                )
                emit(Result.success(detail))
                return@flow
            }
            val res = api.getDetail(id)
            val detailData = res.data
            
            // Prefer metadata from detail response if available
            var finalBookName = detailData?.dramaName ?: bookName
            var finalCover = cover
            var finalIntro = detailData?.description ?: intro

            // Try to fetch metadata from browse endpoint if still missing
            if (finalBookName.isEmpty() || finalCover.isEmpty() || finalIntro.isEmpty()) {
                try {
                    val browseRes = api.getBrowse()
                    val dramaBook = browseRes.books?.find { it.dramaId == id }
                    if (dramaBook != null) {
                        if (finalBookName.isEmpty()) finalBookName = dramaBook.dramaName
                        if (finalCover.isEmpty()) finalCover = dramaBook.thumbUrl ?: ""
                        if (finalIntro.isEmpty()) finalIntro = dramaBook.description ?: ""
                    }
                } catch (e: Exception) {
                    // If browse fails, use passed data
                }
            }
            
            val chapters = detailData?.videoList?.map { ep ->
                Chapter(
                    chapterId = ep.videoId,
                    chapterIndex = ep.episode - 1, // 1-based to 0-based
                    chapterName = "Episode ${ep.episode}",
                    isCharge = 0,
                    vid = ep.videoId,
                    durationSeconds = ep.duration
                )
            } ?: emptyList()

            val detail = DramaDetail(
                bookId = id,
                bookName = finalBookName,
                cover = finalCover,
                introduction = finalIntro,
                tags = detailData?.tags,
                chapterList = chapters,
                source = "melolo"
            )
            emit(Result.success(detail))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getVideo(
        bookId: String,
        index: Int,
        bookName: String,
        source: String = "melolo",
        preferredQuality: Int = 720
    ) = flow {
        try {
            if (isDramabox(source)) {
                val streamRes = api.getDramaboxStream(bookId, index)
                val data = streamRes.data
                val qualities = data?.qualities?.map { q ->
                    VideoQuality(q.quality, q.videoPath, q.isDefault)
                }?.filter { it.videoPath.isNotEmpty() && it.quality > 0 } ?: emptyList()
                val picked = pickPreferredQuality(qualities, preferredQuality)
                val finalUrl = picked?.videoPath ?: data?.videoUrl ?: qualities.firstOrNull()?.videoPath.orEmpty()

                if (finalUrl.isNotEmpty()) {
                    val videoData = VideoData(
                        bookId = bookId,
                        chapterIndex = index,
                        videoUrl = finalUrl,
                        cover = data?.cover,
                        qualities = qualities
                    )
                    emit(Result.success(videoData))
                } else {
                    emit(Result.failure(Exception("Stream URL not found")))
                }
                return@flow
            }

            if (isReelshort(source)) {
                val detailRes = api.getReelshortDetail(bookId)
                val targetEp = detailRes.data?.videoList?.find { ((it.index ?: it.serialNumber ?: 1) - 1) == index }
                val chapterId = targetEp?.chapterId

                if (chapterId != null) {
                    val streamRes = api.getReelshortStream(bookId, chapterId)
                    val videos = streamRes.data?.videoList ?: emptyList()
                    val orderedVideos = videos.sortedWith(compareByDescending<ReelshortVideo> {
                        (it.encode ?: "").equals("H264", ignoreCase = true)
                    }.thenByDescending { it.dpi ?: 0 })
                    val qualities = orderedVideos.mapNotNull { v ->
                        val dpi = v.dpi ?: 0
                        val url = v.playUrl ?: ""
                        if (url.isNotEmpty()) {
                            VideoQuality(dpi, url, 0, v.encode)
                        } else {
                            null
                        }
                    }
                    val fallbackUrl = orderedVideos.firstOrNull { !it.playUrl.isNullOrEmpty() }?.playUrl ?: ""
                    val picked = pickPreferredQuality(qualities, preferredQuality)
                    val finalUrl = picked?.videoPath ?: fallbackUrl

                    if (finalUrl.isNotEmpty()) {
                        emit(Result.success(VideoData(
                            bookId = bookId,
                            chapterIndex = index,
                            videoUrl = finalUrl,
                            cover = null,
                            qualities = qualities
                        )))
                    } else {
                        emit(Result.failure(Exception("Stream URL not found")))
                    }
                } else {
                    emit(Result.failure(Exception("Episode not found")))
                }
                return@flow
            }

            if (isFreereels(source)) {
                val episodeNumber = index + 1
                val streamRes = api.getFreereelsStream(bookId, episodeNumber)
                val data = streamRes.data
                val fallbackUrl = data?.h264M3u8 ?: data?.videoUrl ?: data?.m3u8Url ?: data?.h265M3u8 ?: ""
                val qualities = listOfNotNull(
                    data?.h264M3u8?.takeIf { it.isNotEmpty() }?.let { VideoQuality(720, it, 1, "H264") },
                    data?.h265M3u8?.takeIf { it.isNotEmpty() }?.let { VideoQuality(720, it, 0, "H265") }
                )
                val picked = pickPreferredQuality(qualities, preferredQuality)
                val finalUrl = picked?.videoPath ?: fallbackUrl

                val subtitles = data?.subtitles?.mapNotNull { sub ->
                    val lang = sub.language ?: return@mapNotNull null
                    val url = sub.url ?: return@mapNotNull null
                    val displayName = sub.displayName ?: lang
                    val isDefault = lang.startsWith("id", ignoreCase = true)
                    SubtitleData(lang, url, displayName, isDefault)
                }

                if (finalUrl.isNotEmpty()) {
                    emit(Result.success(VideoData(
                        bookId = bookId,
                        chapterIndex = index,
                        videoUrl = finalUrl,
                        cover = data?.cover,
                        qualities = qualities.ifEmpty {
                            listOf(VideoQuality(720, finalUrl, 1))
                        },
                        subtitles = subtitles
                    )))
                } else {
                    emit(Result.failure(Exception("Stream URL not found")))
                }
                return@flow
            }

            if (isNetshort(source)) {
                val episodeNumber = index + 1
                val streamRes = api.getNetshortStream(bookId)
                val episodes = streamRes.data?.episodes ?: emptyList()
                
                val targetEpisode = episodes.find { it.episode == episodeNumber }
                val finalUrl = targetEpisode?.stream ?: ""
                val subtitleUrl = targetEpisode?.subtitle ?: ""

                if (finalUrl.isNotEmpty()) {
                    val subtitles = if (subtitleUrl.isNotEmpty()) {
                        listOf(SubtitleData("id", subtitleUrl, "Indonesian", true))
                    } else emptyList()
                    
                    emit(Result.success(VideoData(
                        bookId = bookId,
                        chapterIndex = index,
                        videoUrl = finalUrl,
                        cover = targetEpisode?.cover,
                        qualities = listOf(VideoQuality(720, finalUrl, 1)),
                        subtitles = subtitles
                    )))
                } else {
                    emit(Result.failure(Exception("Stream URL not found")))
                }
                return@flow
            }

            if (isMeloshort(source)) {
                // Determine target episode
                val detailRes = api.getMeloshortDetail(bookId)
                val targetEp = detailRes.data?.videoList?.find { ((it.episode ?: 1) - 1) == index }
                val episodeId = targetEp?.episodeId

                if (episodeId != null) {
                    val streamRes = api.getMeloshortStream(bookId, episodeId)
                    val data = streamRes.data
                    val videos = data?.videos ?: emptyList()
                    val qualities = videos.mapNotNull { v ->
                        val url = v.url ?: return@mapNotNull null
                        if (url.isEmpty()) return@mapNotNull null
                        val qInt = v.quality?.filter { it.isDigit() }?.toIntOrNull() ?: 720
                        VideoQuality(qInt, url, 0)
                    }
                    val picked = pickPreferredQuality(qualities, preferredQuality)
                    val finalUrl = picked?.videoPath ?: qualities.firstOrNull()?.videoPath ?: ""

                    val subtitles = data?.subtitles?.mapNotNull { sub ->
                        val lang = sub.language ?: return@mapNotNull null
                        val url = sub.url ?: return@mapNotNull null
                        val displayName = when {
                            lang.startsWith("ind", ignoreCase = true) -> "Indonesian"
                            lang.startsWith("id", ignoreCase = true) -> "Indonesian"
                            lang.startsWith("en", ignoreCase = true) -> "English"
                            else -> lang
                        }
                        val isDefault = lang.startsWith("id", ignoreCase = true) || lang.startsWith("ind", ignoreCase = true)
                        SubtitleData(lang, url, displayName, isDefault)
                    }

                    if (finalUrl.isNotEmpty()) {
                        emit(Result.success(VideoData(
                            bookId = bookId,
                            chapterIndex = index,
                            videoUrl = finalUrl,
                            cover = null,
                            qualities = qualities.ifEmpty { listOf(VideoQuality(720, finalUrl, 1)) },
                            subtitles = subtitles ?: emptyList()
                        )))
                    } else {
                        emit(Result.failure(Exception("Stream URL not found")))
                    }
                } else {
                    emit(Result.failure(Exception("Episode not found")))
                }
                return@flow
            }

            if (isGoodshort(source)) {
                val streamRes = api.getGoodshortStream(bookId)
                val streamData = streamRes.data
                val episodes = streamData?.downloadList ?: emptyList()
                val targetEpisode = episodes.find { it.index == index } ?: episodes.getOrNull(index)

                if (targetEpisode != null) {
                    val qualities = targetEpisode.multiVideos
                        ?.mapNotNull { video ->
                            val url = video.filePath ?: return@mapNotNull null
                            if (url.isEmpty()) return@mapNotNull null
                            val quality = video.type?.filter { it.isDigit() }?.toIntOrNull() ?: 720
                            VideoQuality(quality, url, 0)
                        }
                        ?.sortedByDescending { it.quality }
                        ?: emptyList()

                    val fallbackUrl = qualities.firstOrNull()?.videoPath
                        ?: targetEpisode.multiVideos?.firstOrNull()?.filePath
                        ?: ""
                    val picked = pickPreferredQuality(qualities, preferredQuality)
                    val finalUrl = picked?.videoPath ?: fallbackUrl

                    if (finalUrl.isNotEmpty()) {
                        emit(Result.success(VideoData(
                            bookId = bookId,
                            chapterIndex = index,
                            videoUrl = finalUrl,
                            cover = targetEpisode.image ?: streamData?.bookCover,
                            qualities = qualities.ifEmpty { listOf(VideoQuality(720, finalUrl, 1)) }
                        )))
                    } else {
                        emit(Result.failure(Exception("Stream URL not found")))
                    }
                } else {
                    emit(Result.failure(Exception("Episode not found")))
                }
                return@flow
            }

            if (isDramawave(source)) {
                val episodeNumber = index + 1
                val streamRes = api.getDramawaveStream(bookId, episodeNumber)
                val data = streamRes.data
                val fallbackUrl = data?.h264M3u8 ?: data?.videoUrl ?: data?.m3u8Url ?: data?.h265M3u8 ?: ""
                val qualities = listOfNotNull(
                    data?.h264M3u8?.takeIf { it.isNotEmpty() }?.let { VideoQuality(720, it, 1, "H264") },
                    data?.h265M3u8?.takeIf { it.isNotEmpty() }?.let { VideoQuality(720, it, 0, "H265") }
                )
                val picked = pickPreferredQuality(qualities, preferredQuality)
                val finalUrl = picked?.videoPath ?: fallbackUrl

                val subtitles = data?.subtitleList?.mapNotNull { sub ->
                    val lang = sub.language ?: return@mapNotNull null
                    val url = sub.subtitle ?: sub.vtt ?: return@mapNotNull null
                    val displayName = sub.displayName ?: lang
                    val isDefault = lang.startsWith("id", ignoreCase = true)
                    SubtitleData(lang, url, displayName, isDefault)
                }

                if (finalUrl.isNotEmpty()) {
                    emit(Result.success(VideoData(
                        bookId = bookId,
                        chapterIndex = index,
                        videoUrl = finalUrl,
                        cover = data?.cover,
                        qualities = qualities.ifEmpty {
                            listOf(VideoQuality(720, finalUrl, 1))
                        },
                        subtitles = subtitles
                    )))
                } else {
                    emit(Result.failure(Exception("Stream URL not found")))
                }
                return@flow
            }
            // Melolo: fetch detail to get video ID, then stream
            val detailRes = api.getDetail(bookId)
            val targetEp = detailRes.data?.videoList?.find { (it.episode - 1) == index }

            if (targetEp != null) {
                val streamRes = api.getStream(targetEp.videoId)

                val qualities = streamRes.data?.qualities?.map { q ->
                    val qInt = q.label.filter { it.isDigit() }.toIntOrNull() ?: 480
                    VideoQuality(qInt, q.url, 0, q.codec)
                }?.filter { it.videoPath.isNotEmpty() && it.quality > 0 } ?: emptyList()

                val picked = pickPreferredQuality(qualities, preferredQuality)
                val finalUrl = picked?.videoPath ?: qualities.firstOrNull()?.videoPath ?: ""

                if (finalUrl.isNotEmpty()) {
                     val videoData = VideoData(
                        bookId = bookId,
                        chapterIndex = index,
                        videoUrl = finalUrl,
                    cover = streamRes.data?.poster,
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
