package com.v2ray.ang.model

object Conf {
    var nodeListExpireTimeInMillis: Long = 60 * 1000 + 1                // 节点列表过期时间
    var nodeLatencyExpireTimeInMillis: Long = 60 * 1000 + 1             // 节点延迟过期时间，过期后重新PING
    var nodeLatencyGoodInMillis: Long = 300 + 1                         // 节点网络延迟，单位毫秒
    var nodeLatencyAvgInMillis: Long = 500 + 1                          // 节点网络延迟，单 位毫秒

}