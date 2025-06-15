package com.v2ray.ang.model

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings.Secure
import android.util.Log
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.utilx.NetworkUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

const val NODE_TYPE_LOCATION = 1
const val NODE_TYPE_STREAMING = 2
const val NODE_ID_AUTO_SELECT = Int.MAX_VALUE

const val FREE_TYPE_VIP0 = 0
const val FREE_TYPE_VIP1 = 1

@Serializable
data class User (@SerialName("deviceId") var deviceId: String) {
    @SerialName("userId")
    var userId: Long = 0

    @SerialName("createAt")
    var createAt: Int = 0
}

@Serializable
data class Node(@SerialName("id") val id: Int) {
    @SerialName("hostId")
    var hostId: Int = 0

    @SerialName("groupId")
    var groupId: Int = 0

    @SerialName("sortId")
    var sortId: Int = 0

    @SerialName("nodeType")
    var nodeType: Int = 0

    @SerialName("name")
    var name: String = ""

    @SerialName("icon")
    var icon: String = ""

    @SerialName("freeType")
    var freeType: Int = 0

    @SerialName("host")
    var host: String = ""

    @SerialName("link")
    var link: String = ""
}

object UserProfile {
    private const val TAG = "UserProfile"

    var isSyncOk = AtomicBoolean(false)
    private var isSyncGo = AtomicBoolean(false)

    lateinit var user: User

    var nodesUpdateAt = AtomicLong(0)
    val nodes = AtomicReference<List<Node>>(mutableListOf())

    @SuppressLint("HardwareIds")
    fun readUserFromLocal(resolver: ContentResolver) {
        val profile = MmkvManager.getUserProfile()
        if (profile == null) {
            val androidId = Secure.getString(resolver, Secure.ANDROID_ID) ?: ""
            this.user = User(androidId)
            Log.i(TAG, "read new user:${Json.encodeToString(this.user)}")
            this.writeUserToLocal()
        } else {
            this.user = Json.decodeFromString<User>(profile)
            Log.i(TAG, "read local user:${Json.encodeToString(this.user)}")
        }
    }

    private fun writeUserToLocal() {
        val profile = Json.encodeToString(this.user)
        MmkvManager.setUserProfile(profile)
        Log.i(TAG, "save local user: ${Json.encodeToString(this.user)}")
    }

    private fun parseNodes(nodes: JSONArray) {
        val tmpNodes = mutableListOf<Node>()
        for (i in 0 until nodes.length()) {
            val entry = nodes.getJSONObject(i)
            val nd = Node(entry.getInt("id"))
            nd.hostId = entry.getInt("hostId")
            nd.groupId = entry.getInt("groupId")
            nd.sortId = entry.getInt("sortId")
            nd.nodeType = entry.getInt("nodeType")
            nd.name = entry.getString("name")
            nd.icon = entry.getString("icon")
            nd.freeType = entry.getInt("freeType")
            nd.host = entry.getString("host")
            nd.link = entry.getString("link")
            tmpNodes.add(nd)
        }

        this.nodes.set(tmpNodes)
        this.nodesUpdateAt.set(System.currentTimeMillis())
    }

    private fun parseConf(joConf: JSONObject) {
        Conf.latencyExpireTime = joConf.getLong("latencyExpireTime") * 1000
        Conf.latencyTickInterval = joConf.getLong("latencyTickInterval") * 1000
        Conf.nodesExpireTime = joConf.getLong("nodesExpireTime") * 1000
        Conf.latencyGood = joConf.getLong("latencyGood")
        Conf.latencyAvg = joConf.getLong("latencyAvg")

        Log.d(TAG, "Conf latencyExpireTime ${Conf.latencyExpireTime}")
        Log.d(TAG, "Conf latencyTickInterval ${Conf.latencyTickInterval}")
        Log.d(TAG, "Conf nodesExpireTime ${Conf.nodesExpireTime}")
        Log.d(TAG, "Conf LatencyGood ${Conf.latencyGood}")
        Log.d(TAG, "Conf LatencyAvg ${Conf.latencyAvg}")
    }

    fun isNodesExpired(): Boolean {
        val liveTime = System.currentTimeMillis() - nodesUpdateAt.get()
        return liveTime > Conf.nodesExpireTime
    }

    fun getChoiceNodeId(): Int {
        var nodeId = MmkvManager.getNodeChoice()
        nodeId = if (nodeId != -1) nodeId else NODE_ID_AUTO_SELECT
        Log.i(TAG, "got choice node id $nodeId")
        return nodeId
    }

    fun setChoiceNodeId(nodeId : Int) {
        MmkvManager.setNodeChoice(nodeId)
        Log.i(TAG, "set choice node id $nodeId")
    }

    private fun reqUser() {
        val (opCode, joData) = NetOp.user(this.user.userId, this.user.deviceId)
        if (opCode != 0 || joData == null) {
            Log.e(TAG, "reqUser except,opCode $opCode")
            return
        }

        // 读账号数据

        val joUser = joData.getJSONObject("user")
        this.user.userId = joUser.getLong("userId")
        this.user.createAt = joUser.getInt("createAt")

        this.writeUserToLocal()

        isSyncOk.set(true)

        // 后台下发的配置

        if (joData.has("conf")) {
            val joConf = joData.getJSONObject("conf")
            this.parseConf(joConf)
        }

        // 节点

        if (joData.has("nodes")) {
            val joNodes = joData.getJSONArray("nodes")
            this.parseNodes(joNodes)
            Log.d(TAG, "nodes:${Json.encodeToString(this.nodes.get())}")

            // 立即探测网络延迟
            Latency.tick()
        }
    }

    fun reqNodes() {
        val (opCode, joData) = NetOp.user(this.user.userId, this.user.deviceId)
        if (opCode != 0 || joData == null) {
            Log.e(TAG, "reqNodes except,opCode $opCode")
            return
        }

        if (joData.has("nodes")) {
            val joNodes = joData.getJSONArray("nodes")
            this.parseNodes(joNodes)
            Log.d(TAG, "remote nodes:${Json.encodeToString(this.nodes.get())}")
        }
    }

    fun sync(context: Context) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return
        }

        if (!isSyncOk.get()) {
            if (isSyncGo.compareAndSet(false, true)) {
                try {
                    this.reqUser()
                } finally {
                    isSyncGo.set(false)
                }
            }
        }
    }

    fun isSyncComplete(): Boolean {
        return isSyncOk.get()
    }
}