package com.v2ray.ang.utilx.update

import android.util.Log
import com.google.gson.Gson
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.proxy.IUpdateParser
import org.json.JSONObject
import com.google.gson.annotations.SerializedName

data class UpdateRetVal(
    @SerializedName("hasUpdate")
    var hasUpdate: Boolean = false,
    @SerializedName("versionCode")
    var versionCode: Int = 0,
    @SerializedName("versionName")
    var versionName: String = "",
    @SerializedName("isForce")
    var isForce: Boolean = false,
    @SerializedName("isIgnorable")
    var isIgnorable: Boolean = false,
    @SerializedName("updateContent")
    var updateContent: String = "",
    @SerializedName("isSilent")
    var isSilent: Boolean = false,
    @SerializedName("isAutoInstall")
    var isAutoInstall: Boolean = false,
    @SerializedName("apkUrl")
    var apkUrl: String = "",
    @SerializedName("apkMd5")
    var apkMd5: String = "",
    @SerializedName("apkSize")
    var apkSize: Long = 0,   // KB
)

class XUpdateParserImpl : IUpdateParser {
    companion object {
        private const val TAG = "XUpdateParserImpl"
    }

    override fun parseJson(jsData: String): UpdateEntity {
        Log.d(TAG, "parse json:${jsData}")
        val r = Gson().fromJson(jsData, UpdateRetVal::class.java)
        val u = UpdateEntity()
        u.setHasUpdate(r.hasUpdate)
        u.setForce(r.isForce)
        u.setIsIgnorable(r.isIgnorable)
        u.setVersionCode(r.versionCode)
        u.setVersionName(r.versionName)
        u.setUpdateContent(r.updateContent)
        u.setDownloadUrl(r.apkUrl)
        u.setSize(r.apkSize)
        u.setMd5(r.apkMd5)
        return u
    }

    override fun parseJson(json: String, callback: IUpdateParseCallback?) {
        val u = this.parseJson(json)
        callback?.onParseResult(u)
    }

    override fun isAsyncParser(): Boolean {
        return false
    }
}