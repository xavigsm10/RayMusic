package com.echo.innertube

import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.models.response.PlayerResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.Proxy

class NewPipeDownloaderImpl(private val proxy: Proxy? = null, private val proxyAuth: String? = null) : Downloader() {
    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .proxyAuthenticator { _, response ->
            proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .fastFallback(true)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body?.string() ?: ""
        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }
}

object NewPipeExtractor {
    private var isInitialized = false

    fun init() {
        if (!isInitialized) {
            NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy, YouTube.proxyAuth))
            isInitialized = true
        }
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> {
        init()
        return runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        init()
        val signatureCipher = format.signatureCipher
        return if (!signatureCipher.isNullOrEmpty()) {
            YouTubeExtractor.decryptUrl(signatureCipher)
        } else if (!format.url.isNullOrEmpty()) {
            YouTubeExtractor.deobfuscateUrlNParam(format.url)
        } else {
            null
        }
    }

    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        init()
        return try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

object NewPipeUtils {
    fun getSignatureTimestamp(videoId: String): Result<Int> =
        NewPipeExtractor.getSignatureTimestamp(videoId)

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            NewPipeExtractor.getStreamUrl(format, videoId) ?: throw Exception("Could not find stream URL")
        }

    fun newPipePlayer(videoId: String): List<Pair<Int, String>> =
        NewPipeExtractor.newPipePlayer(videoId)
}