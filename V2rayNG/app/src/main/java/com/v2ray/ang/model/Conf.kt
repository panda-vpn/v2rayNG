package com.v2ray.ang.model

object Conf {
    var LATENCY_EXPIRE_TIME: Long = 1 * 60 * 1000    // milliseconds
    var LATENCY_TICK_INTERVAL: Long = 1 * 60 * 1000
    var NODES_EXPIRE_TIME: Long = 1 * 60 * 1000

    var LATENCY_GOOD: Long = 300
    var LATENCY_AVG: Long = 800
}