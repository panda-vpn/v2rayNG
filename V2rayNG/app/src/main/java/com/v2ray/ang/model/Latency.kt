package com.v2ray.ang.model

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object Latency {

    private const val TAG = "Latency"

    class Latency(val hostIp: String) {
        var rtt : Long? = null          // round trip time
        var updateAt: Long = 0          // millisecond
    }

    val latencyTbl = ConcurrentHashMap<String, Latency>()
    val execFlag = AtomicBoolean(false)

    private fun addIfAbsent(hostIp : String) {
        if (!latencyTbl.contains(hostIp)) {
            val ent = Latency(hostIp)
            latencyTbl.putIfAbsent(hostIp, ent)
        }
    }

    fun getRttByHostIp(hostIp: String): Long? {
        val ent = latencyTbl.get(hostIp)
        return ent?.rtt
    }

    fun getMinRtt(): Long? {
        var rtt : Long? = null
        for (item in latencyTbl) {
            val v = item.value
            if ((rtt == null) || (v.rtt != null && v.rtt!! < rtt)) {
                rtt = v.rtt
            }
        }
        return rtt
    }

    fun tick() {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d(TAG, "latency tick")
            val tmpNodes = Client.nodes.get()
            for (item in tmpNodes) {
                addIfAbsent(item.hostIp)
            }
            safeBatchExec()
        }
    }

    private suspend fun safeBatchExec() {
        if (!execFlag.compareAndSet(false, true)) {
            Log.i(TAG, "batch exec reentrancy")
            return
        }
        try {
            batchExec()
        } finally {
            execFlag.set(false)
        }
    }

    suspend fun batchExec() {
        val hosts = mutableListOf<String>()
        val now = System.currentTimeMillis()
        latencyTbl.forEach { entry ->
            val ent = entry.value
            if (ent.rtt == null || ent.updateAt + Conf.LATENCY_EXPIRE_TIME <= now) {
                hosts.add(ent.hostIp)
            }
        }

        val jobs = mutableListOf<Job>()
        for (item in hosts) {
            val j = GlobalScope.launch(Dispatchers.IO) {
                val rtt = execLatency(item)
                // if (rtt != null && rtt > 30) {          // tun模式下icmp会被拦截处理
                val ent = Latency(item)
                ent.rtt = rtt
                ent.updateAt = now
                latencyTbl.put(item, ent)
                Log.i(TAG, "ping $item,latency $rtt ms")
                // }
            }
            jobs.add(j)
        }
        jobs.joinAll()
    }

    fun execLatency(host: String) : Long? {
        for (i in 1..3) {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "////----- startTime: $startTime")
                var process = Runtime.getRuntime().exec("ping -s 56 -c 1 $host")
                ////----
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                Log.d(TAG, "////----- output: $output")
                ////----
                if (process.waitFor() == 0) {
                    val latency = System.currentTimeMillis() - startTime
                    Log.d(TAG, "////----- startTime: $startTime,endTime: ${System.currentTimeMillis()},expend $latency")
                    return latency
                }
            } catch (e: Exception) {
                Log.e(TAG, "ping $host,$e")
            }
        }
        return null
    }

    suspend fun testLatency(no: Int, host: String) {
        while(true) {
            val latency = execLatency(host)
            Log.i(TAG, "/////****** $no --- ping $latency")
            delay(5000)
        }
    }
}