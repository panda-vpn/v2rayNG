package com.v2ray.ang.handler

import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ProfileItem

object ProfileManager {

    val UUID_SELECTED_SERVER = "6599a420-4beb-4e69-979c-f6ee223c7354".replace("-", "")

    fun encodeVlessProfileItem(
        address: String,     // 10.10.10.10
        port: String,        // 443
        userUUID: String,    // 99a7b512-740c-43bc-904e-c4bbf0f762db
        params: Map<String, String>
    ) {

        val profile = ProfileItem.create(EConfigType.VLESS)

        // Common

        profile.remarks = params.getOrDefault("remarks", "vless_ws_tls")
        profile.server = address
        profile.serverPort = port
        profile.password = userUUID

        profile.method = params.getOrDefault("method", "none")
        profile.flow = params.getOrDefault("flow", "")

        // StreamSettings

        profile.network = params.getOrDefault("network", "ws")
        profile.headerType = params.getOrDefault("headerType", "---")
        profile.host = params.getOrDefault("host", "")
        profile.path = params.getOrDefault("path", "/api")
        profile.seed = params.getOrDefault("seed", "/api")
        profile.quicSecurity = params.getOrDefault("quicSecurity", "")
        profile.quicKey = params.getOrDefault("quicKey", "/api")
        profile.mode = params.getOrDefault("mode", "---")
        profile.serviceName = params.getOrDefault("serviceName", "/api")
        profile.authority = params.getOrDefault("authority", "")
        profile.xhttpMode = params.getOrDefault("xhttpMode", "---")
        profile.xhttpExtra = params.getOrDefault("xhttpExtra", "")

        // TLS

        profile.security = params.getOrDefault("security", "tls")
        profile.insecure = params.getOrDefault("insecure", "true").toBoolean()
        profile.sni = params.getOrDefault("sni", "")
        profile.fingerPrint = params.getOrDefault("fingerPrint", "chrome")
        profile.alpn = params.getOrDefault("alpn", "http/1.1")
        profile.publicKey = params.getOrDefault("publicKey", "")
        profile.shortId = params.getOrDefault("shortId", "")
        profile.spiderX = params.getOrDefault("spiderX", "")

        MmkvManager.encodeServerConfig(UUID_SELECTED_SERVER, profile)
    }
}