package io.nekohasekai.sfa.utils

import com.v2ray.ang.AngApplication
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OkHttpUtils {

    /*
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    */

    // 不知为何，go server会有证书问题，matomo却没有问题，可能与matomo用的不是OkHttp3有关系!!!
    // 另外，Chrome浏览器访问，也显示HTTPS是安全的
    
    fun getClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(java.security.cert.CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                // 信任所有客户端证书（一般用不到）
            }

            @Throws(java.security.cert.CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // 信任所有服务器证书，不做任何验证 ✅
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf() // 返回空数组
            }
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        var cli = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // 绕过主机名验证
            .build()

        return cli
    }

    fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        getClient().newCall(req).execute().use { rsp ->
            if (!rsp.isSuccessful) throw IOException("Unexpected code $rsp")
            return rsp.body?.string() ?: ""
        }
    }

    fun asyncGet(url: String, callback: (String?, Throwable?) -> Unit) {
        val req = Request.Builder().url(url).build()
        getClient().newCall(req).enqueue(object : Callback {
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

        getClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("unexpected code: $response")
            return response.body?.string() ?: ""
        }
    }
}