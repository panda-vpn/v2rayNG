package com.v2ray.ang.model

import android.text.TextUtils
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object Latency {

    private const val TAG = "Latency"

    // rtt单位是秒, -1表示未获取延迟，updateAt 时间戳，单位毫秒
    data class LatencyRecord(val hostIp: String, val rtt: Long?, val updateAt: Long)

    val latencyTbl = ConcurrentHashMap<String, LatencyRecord>()
    val isOngoing = AtomicBoolean(false)

    private fun addIfAbsent(hostIp : String) {
        latencyTbl.putIfAbsent(hostIp, LatencyRecord(hostIp, null, 0))
    }

    fun getRttByHostIp(hostIp: String): Long? {
        val ent = latencyTbl.get(hostIp)
        return ent?.rtt
    }

    fun getMinRtt(): Long? {
        var rtt : Long? = null
        for (item in latencyTbl) {
            val v = item.value
            if (v.rtt != null && (rtt == null || v.rtt < rtt)) {
                rtt = v.rtt
            }
        }
        return rtt
    }

    fun tick() {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d(TAG, "latency tick")
            val tmpNodes = UserProfile.nodes.get()
            for (item in tmpNodes) {
                addIfAbsent(item.host)
            }

            if (isOngoing.compareAndSet(false, true)) {
                try {
                    batchPing()
                } finally {
                    isOngoing.set(false)
                }
            }
        }
    }

    suspend fun batchPing() {
        val hosts = mutableListOf<String>()
        val now = System.currentTimeMillis()

        latencyTbl.forEach { entry ->
            val ent = entry.value
            if (ent.updateAt + Conf.latencyExpireTime <= now) {
                hosts.add(ent.hostIp)
            }
        }

        val jobs = mutableListOf<Job>()
        for (hostIp in hosts) {
            val j = GlobalScope.launch(Dispatchers.IO) {
                val rtt = ping(hostIp)
                // if (rtt != null && rtt > 30) {          // tun模式下icmp会被拦截处理
                val record = LatencyRecord(hostIp, rtt, now)
                latencyTbl.put(hostIp, record)
                Log.i(TAG, "ping $hostIp,latency $rtt ms")
                // }
            }
            jobs.add(j)
        }
        jobs.joinAll()
    }

    fun ping(hostIp: String) : Long? {
        for (i in 1..3) {
            try {
                val command = "/system/bin/ping -s 56 -c 1 $hostIp"
                val process = Runtime.getRuntime().exec(command)
                val allText = process.inputStream.bufferedReader().use { it.readText() }
                if (!TextUtils.isEmpty(allText)) {
                    val tempInfo = allText.substring(allText.indexOf("min/avg/max/mdev") + 19)
                    val temps = tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (temps.count() > 0 && temps[0].length < 10) {
                        return temps[0].toFloat().toLong()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ping $hostIp,$e")
            }
        }
        return null
    }

    suspend fun testPing(no: Int, host: String) {
        while(true) {
            val latency = ping(host)
            Log.i(TAG, "/////****** $no --- ping $latency")
            delay(5000)
        }
    }
}