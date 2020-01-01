package app.provider

import app.core.await
import app.model.Provider
import app.model.Torrent
import app.provider.Transmission.TransmissionRequest.TorrentSetLocation
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class Transmission(private val provider: Provider, private val gson: Gson) : Interceptor, Authenticator {

    private var sessionToken: String? = null

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(this)
        .addInterceptor(logging)
        .authenticator(this)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getTorrents(): List<Torrent> {
        val responseBody = call(TransmissionRequest.TorrentGet).body
        val torrents = responseBody?.use {
            gson.fromJson(it.string(), TorrentGetResponse::class.java)
        }
        return torrents?.arguments?.torrents ?: listOf()
    }

    suspend fun setLocation(torrent: Torrent, remoteSharePath: String) {
        val response = call(TorrentSetLocation(torrent.id, remoteSharePath))
    }

    private suspend fun call(request: TransmissionRequest): Response {
        val requestBody = when (request) {
            is TransmissionRequest.TorrentGet -> """{
                                          "method": "torrent-get",
                                          "arguments": {
                                            "fields": [
                                              "id", 
                                              "name", 
                                              "percentDone", 
                                              "downloadDir", 
                                              "files"
                                            ]
                                          }
                                        }""".trimIndent()
            is TorrentSetLocation -> """{
                                          "method": "torrent-set-location",
                                          "arguments": { "ids": [${request.id}], "location": "${request.path}", "move": true }
                                        }
                                    """.trimIndent()
        }

        val newCall = okHttpClient.newCall(
            Request.Builder()
                .url(provider.url)
                .method("POST", requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
        )

        return newCall.await()
    }

    sealed class TransmissionRequest {
        object TorrentGet : TransmissionRequest()
        data class TorrentSetLocation(val id: Long, val path: String) : TransmissionRequest()
    }

    data class TorrentGetArguments(val torrents: List<Torrent>)
    data class TorrentGetResponse(val arguments: TorrentGetArguments, val success: Boolean)


    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Transmission-Session-Id", sessionToken ?: "")
            .build()
        val response = chain.proceed(request)

        if (response.code == 409) {
            val token = response.headers("X-Transmission-Session-Id").first()
            sessionToken = token
            response.close()

            return chain.proceed(
                request.newBuilder()
                    .header("X-Transmission-Session-Id", token)
                    .build()
            )
        }

        return response
    }

    override fun authenticate(route: Route?, response: Response): okhttp3.Request? {
        return response.request.newBuilder()
            .header(
                "Authorization",
                Credentials.basic(
                    provider.username,
                    provider.password,
                    Charset.forName("UTF-8")
                )
            )
            .build()
    }

    fun clean() {
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }
}
