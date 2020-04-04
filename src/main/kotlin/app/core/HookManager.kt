package app.core

import app.model.Hook
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class HookManager(private val hooks: List<Hook>) {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .build()

    fun execute(type: Type) {
        val basicType = when (type) {
            Type.SyncPre -> "sync/pre"
            Type.SyncPost -> "sync/post"
            Type.FolderPre -> "folder/pre"
            Type.FolderPost -> "folder/post"
            Type.DownloadPre -> "download/pre"
            Type.DownloadPost -> "download/post"
        }

        hooks.filter { it.event == basicType }
            .forEach { execute(it) }
    }

    private fun execute(hook: Hook) {
        val request = Request.Builder()
            .url(hook.url)
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Do nothing
            }

            override fun onResponse(call: Call, response: Response) {
                // Do nothing
            }

        })
    }

    fun clean() {
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }

    enum class Type {
        SyncPre,
        SyncPost,
        FolderPre,
        FolderPost,
        DownloadPre,
        DownloadPost
    }
}