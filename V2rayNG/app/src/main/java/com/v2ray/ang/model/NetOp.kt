package com.v2ray.ang.model

import android.util.Log
import com.v2ray.ang.BuildConfig
import io.nekohasekai.sfa.utils.OkHttpUtils
import org.json.JSONObject

object NetOp {
    private const val TAG = "NetOp"

    fun post(path: String, params: JSONObject): Pair<Int, JSONObject?> {
        try {
            val url = "${BuildConfig.API_BASE_URL}/$path"
            val body = OkHttpUtils.postJson(url, params.toString())

            Log.d(TAG, "path:$path,req ${params.toString()},rsp:$body")

            if (body.isEmpty()) {
                Log.e(TAG, "post response empty,path $path,params ${params.toString()}")
                return Pair(-1, null)
            }
            val jo = JSONObject(body)

            val code = jo.getInt("code")
            if (code != 0) {
                Log.e(TAG, "post response code,path $path,params ${params.toString()},code $code")
                return Pair(code, null)
            }

            var joData = jo.getJSONObject("data")
            return Pair(0, joData)

        } catch (e : Exception) {
            Log.e(TAG, "path $path,params ${params.toString()},exception:$e")
        }

        return Pair(-1, null)
    }

    fun feedback(
        deviceId: String,
        option1: String,
        option2: String,
        option3: String,
        option4: String,
        option5: String,
        option6: String,
        option7: String,
        option8: String,
        others: String) : Int {
        val params = JSONObject().apply {
            put("deviceId", deviceId)
            put("option1", option1)
            put("option2", option2)
            put("option3", option3)
            put("option4", option4)
            put("option5", option5)
            put("option6", option6)
            put("option7", option7)
            put("option8", option8)
            put("others", others)
        }
        val (opCode, _) = post("feedback", params)
        return opCode
    }

    fun user(deviceId: String): Pair<Int, JSONObject?> {
        val params = JSONObject().apply {
            put("deviceId", deviceId)
            put("plat", "android")
            put("channel", BuildConfig.CHANNEL)
        }
        return post("user", params)
    }

    fun nodes(ver: Long): Pair<Int, JSONObject?> {
        val params = JSONObject().apply {
            put("ver", ver)
        }
        return post("nodes", params)
    }
}