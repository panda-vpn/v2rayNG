package io.nekohasekai.sfa.utils

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import java.util.concurrent.TimeUnit

object OkHttpUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { rsp ->
            if (!rsp.isSuccessful) throw IOException("Unexpected code $rsp")
            return rsp.body?.string() ?: ""
        }
    }

    fun asyncGet(url: String, callback: (String?, Throwable?) -> Unit) {
        val req = Request.Builder().url(url).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                callback(body, null)
            }
        })
    }

    fun postJson(url: String, json: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("unexpected code: $response")
            return response.body?.string() ?: ""
        }
    }
}