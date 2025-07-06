package com.v2ray.ang.utilx

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.v2ray.ang.model.Conf
import com.v2ray.ang.model.Latency
import com.v2ray.ang.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SFAppObserver: DefaultLifecycleObserver {

    private const val TAG = "SFAppObserver"
    private var isForeground = false

    private lateinit var scopeForUser: CoroutineScope
    private lateinit var scopeForNodes: CoroutineScope
    private lateinit var scopeForLatency: CoroutineScope

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.d(TAG, "SFApp onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "SFApp onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(TAG, "SFApp onResume")

        isForeground = true

        scopeForUser = CoroutineScope(Dispatchers.IO)
        scopeForLatency = CoroutineScope(Dispatchers.IO)
        scopeForNodes = CoroutineScope(Dispatchers.IO)

        scopeForUser.launch{ periodicReqUser() }
        scopeForNodes.launch{ periodicReqNodes() }
        scopeForLatency.launch{ periodicReqLatency() }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(TAG, "SFApp onPause")

        isForeground = false

        scopeForUser.cancel()
        scopeForNodes.cancel()
        scopeForLatency.cancel()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "SFApp onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "SFApp onDestroy")
    }

    private suspend fun periodicReqUser() {
        while (true) {
            Log.d(TAG, "periodic req user")
            UserProfile.sync()
            if (UserProfile.isSyncComplete()) {
                break
            } else {
                delay(10000)
            }
        }
    }

    private suspend fun periodicReqNodes() {
        while (true) {
            Log.d(TAG, "periodic req nodes")
            UserProfile.reqNodes()
            delay(Conf.nodeListExpireTimeInMillis)
        }
    }

    private suspend fun periodicReqLatency() {
        while (true) {
            Log.d(TAG, "periodic req latency")
            Latency.tick()
            delay(100000)
        }
    }
}