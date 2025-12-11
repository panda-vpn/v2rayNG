package com.v2ray.ang.utilx.update

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.NonNull
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.Okio
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.io.FileOutputStream

class OkHttpUpdateHttpServiceImpl : IUpdateHttpService {

    companion object {
        private const val TAG = "XUpdateHttpServiceImpl"
    }

    private fun getClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            @Throws(java.security.cert.CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                // 信任所有客户端证书（一般用不到）
            }

            @SuppressLint("TrustAllX509TrustManager")
            @Throws(java.security.cert.CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // 信任所有服务器证书，不做任何验证
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf() // 返回空数组
            }
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val cli = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // 绕过主机名验证
            .build()

        return cli
    }

    // 存储正在下载的 Call，用于取消下载（可选）
    private val downloadCalls: MutableMap<String, Call> = mutableMapOf()

    override fun asyncGet(url: String, params: Map<String, Any>, callBack: IUpdateHttpService.Callback) {
        val fullUrl = buildUrlWithParams(url, params)
        val request = Request.Builder().url(fullUrl).get().build()

        Log.d(TAG, "xupdate async get,url=${fullUrl}")

        getClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callBack.onError(ApiException("GET request failure: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    callBack.onSuccess(responseBody)
                } else {
                    callBack.onError(ApiException("HTTP error: ${response.code} - ${response.message}"))
                }
            }
        })
    }

    override fun asyncPost(url: String, params: Map<String, Any>, callBack: IUpdateHttpService.Callback) {
        val json = JSONObject(params).toString()
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()

        getClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callBack.onError(ApiException("POST request failure: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    callBack.onSuccess(responseBody)
                } else {
                    callBack.onError(ApiException("HTTP error: ${response.code} - ${response.message}"))
                }
            }
        })
    }

    override fun download(url: String, path: String, fileName: String, callback: IUpdateHttpService.DownloadCallback) {
        val file = File(path, fileName)
        val request = Request.Builder().url(url).build()

        val call = getClient().newCall(request)
        downloadCalls[url] = call // 存储 Call 以便取消

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                downloadCalls.remove(url)
                callback.onError(ApiException("download failure: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body ?: run {
                        downloadCalls.remove(url)
                        callback.onError(ApiException("response empty"))
                        return
                    }

                    try {
                        val inputStream = body.byteStream()
                        val outputStream = FileOutputStream(file)
                        val buffer = ByteArray(8 * 1024) // 8KB 缓冲区
                        var totalBytesRead: Long = 0
                        val contentLength: Long = body.contentLength()

                        while (true) {
                            val bytesRead: Int = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // 计算进度（避免除零错误）
                            if (contentLength > 0) {
                                val progress = totalBytesRead.toFloat() / contentLength
                                callback.onProgress(progress, contentLength)
                            }
                        }

                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()

                        if (file.exists()) {
                            downloadCalls.remove(url)
                            callback.onSuccess(file)
                        } else {
                            downloadCalls.remove(url)
                            callback.onError(ApiException("download save failure"))
                        }
                    } catch (e: Exception) {
                        downloadCalls.remove(url)
                        callback.onError(ApiException("download exception: ${e.message}"))
                    }
                } else {
                    downloadCalls.remove(url)
                    callback.onError(ApiException("HTTP error: ${response.code} - ${response.message}"))
                }
            }
        })
    }

    override fun cancelDownload(url: String) {
        val call = downloadCalls[url]
        if (call != null) {
            call.cancel()
            downloadCalls.remove(url)
            println("已取消下载: $url")
        } else {
            println("未找到对应的下载任务: $url")
        }
    }

    private fun buildUrlWithParams(url: String, params: Map<String, Any>): String {
        if (params.isEmpty()) return url

        val httpUrlBuilder = url.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("无效的 URL: $url")

        params.forEach { (key, value) ->
            httpUrlBuilder.addQueryParameter(key, value.toString())
        }

        return httpUrlBuilder.build().toString()
    }

    class ApiException(message: String) : Exception(message)
}