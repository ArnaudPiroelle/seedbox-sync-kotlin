package app.core

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun Call.await(): Response = suspendCoroutine { continuation ->
    val e = Exception()
    e.stackTrace = Thread.currentThread().stackTrace

    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            continuation.resumeWithException(e)
        }
    })
}