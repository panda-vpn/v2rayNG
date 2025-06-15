package com.v2ray.ang.model

object Conf {
    var latencyExpireTime: Long = 1 * 60 * 1000         // 节点延迟过期时间，过期后会重启PING，单位毫秒
    var latencyTickInterval: Long = 1 * 60 * 1000       // 节点延迟Tick的间隔时间，单位毫秒
    var nodesExpireTime: Long = 1 * 60 * 1000           // 节点过期时间，过期后会重新拉取节点列表，单位毫秒

    var latencyGood: Long = 300                         // 节点网络延迟，单位毫秒
    var latencyAvg: Long = 500                          // 节点网络延迟，单 位毫秒

}