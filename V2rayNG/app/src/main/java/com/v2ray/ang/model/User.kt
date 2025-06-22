package com.v2ray.ang.model

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings.Secure
import android.util.Log
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.utilx.NetworkUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

const val NODE_TYPE_LOCATION = 1
const val NODE_TYPE_STREAMING = 2
const val NODE_ID_AUTO_SELECT = Int.MAX_VALUE

@Serializable
@JsonIgnoreUnknownKeys
data class User (@SerialName("deviceId") var deviceId: String) {

    @SerialName("createAt")
    var createAt: Int = 0
}

@Serializable
@JsonIgnoreUnknownKeys
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

    @SerialName("vip")
    var vip: Int = 0

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

    private var nodesUpdateAt = AtomicLong(0)
    private var nodesVer = AtomicLong(0)
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
        val choiceNodeId = getChoiceNodeId()
        var isFoundNodeId = false

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
            nd.vip = entry.getInt("vip")
            nd.host = entry.getString("host")
            nd.link = entry.getString("link")
            tmpNodes.add(nd)

            if (choiceNodeId == nd.id) {
                isFoundNodeId = true
            }
        }

        this.nodes.set(tmpNodes)
        this.nodesUpdateAt.set(System.currentTimeMillis())

        if (choiceNodeId != NODE_ID_AUTO_SELECT && !isFoundNodeId) {
            Log.i(TAG,"node id not found,and set to auto select")
            setChoiceNodeId(NODE_ID_AUTO_SELECT)
        }
    }

    private fun parseConf(joConf: JSONObject) {
        joConf.getLong("nodeLatencyExpireTimeInMillis").also{ if(it > 0) Conf.nodeLatencyExpireTimeInMillis = it }
        joConf.getLong("nodeListExpireTimeInMillis").also{ if(it > 0) Conf.nodeListExpireTimeInMillis = it }
        joConf.getLong("nodeLatencyGoodInMillis").also{ if(it > 0) Conf.nodeLatencyGoodInMillis = it }
        joConf.getLong("nodeLatencyAvgInMillis").also{ if(it > 0) Conf.nodeLatencyAvgInMillis = it }
        joConf.getString("assets").also{ if(it.isNotEmpty()) Conf.assetsUrl = it }

        Log.d(TAG, "Conf nodeLatencyExpireTimeInMillis ${Conf.nodeLatencyExpireTimeInMillis}")
        Log.d(TAG, "Conf nodeListExpireTimeInMillis ${Conf.nodeListExpireTimeInMillis}")
        Log.d(TAG, "Conf nodeLatencyGoodInMillis ${Conf.nodeLatencyGoodInMillis}")
        Log.d(TAG, "Conf nodeLatencyAvgInMillis ${Conf.nodeLatencyAvgInMillis}")
        Log.d(TAG, "Conf assets ${Conf.assetsUrl}")
    }

    fun isNodesExpired(): Boolean {
        val liveTime = System.currentTimeMillis() - nodesUpdateAt.get()
        return liveTime > Conf.nodeListExpireTimeInMillis
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
        val (opCode, joData) = NetOp.user(this.user.deviceId)
        if (opCode != 0 || joData == null) {
            Log.e(TAG, "reqUser except,opCode $opCode")
            return
        }

        // 读账号数据

        val joUser = joData.getJSONObject("user")
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
        val (opCode, joData) = NetOp.nodes(this.nodesVer.get())
        if (opCode != 0 || joData == null) {
            Log.e(TAG, "reqNodes except,opCode $opCode")
            return
        }

        if (joData.has("ver")) {
            val ver = joData.getLong("ver")
            if (ver != this.nodesVer.get()) {
                this.nodesVer.set(ver)
                if (joData.has("nodes")) {
                    val joNodes = joData.getJSONArray("nodes")
                    this.parseNodes(joNodes)
                    Log.d(TAG, "remote nodes,ver ${this.nodesVer.get()},${Json.encodeToString(this.nodes.get())}")
                }
            }
        }
    }

    fun sync() {
        if (isSyncGo.compareAndSet(false, true)) {
            try {
                this.reqUser()
            } finally {
                isSyncGo.set(false)
            }
        }
    }

    fun isSyncComplete(): Boolean {
        return isSyncOk.get()
    }
}