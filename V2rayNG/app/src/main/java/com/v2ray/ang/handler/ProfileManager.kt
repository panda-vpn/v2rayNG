package com.v2ray.ang.handler

import android.util.Log
import com.v2ray.ang.AppConfig.HY2
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import com.v2ray.ang.model.Latency
import com.v2ray.ang.model.NODE_ID_AUTO_SELECT
import com.v2ray.ang.model.Node
import com.v2ray.ang.model.UserProfile
import kotlinx.coroutines.selects.select

object ProfileManager {

    private const val TAG = "ProfileManager"
    private val SELECTED_UUID = "00000000-0000-0000-0000-00000000GOOD".replace("-", "")

    // TODO
    // 先不考虑VIP逻辑
    // 后面需要判断vip过期，付费节点自动切换至免费节点

    fun setSelectedServer(): Boolean{
        var selectedNode: Node? = null
        val selectedNodeId = UserProfile.getSelectedNodeId()
        val nodes = UserProfile.nodes.get()

        if (selectedNodeId != NODE_ID_AUTO_SELECT) {
            for (nd in nodes) {
                if (nd.id == selectedNodeId) {
                    selectedNode = nd
                    break
                }
            }
        } else {
            var minRtt: Long? = null
            for (nd in nodes) {
                var rtt = Latency.getRttByHostIp(nd.host)
                if (minRtt == null || (rtt != null && rtt < minRtt)) {
                    minRtt = rtt
                    selectedNode = nd
                }
            }
        }

        if (selectedNode == null) {
            return false
        }

        Log.d("TAG", "selected node ${selectedNode.id},host ${selectedNode.host}")

        val profileItem = parseSingleConfig(selectedNode.link)
        if (profileItem == null) {
            Log.e("TAG", "parse config except,node link: ${selectedNode.link}")
            return false
        }

        MmkvManager.encodeServerConfig(SELECTED_UUID, profileItem)
        MmkvManager.setSelectServer(SELECTED_UUID)

        return true
    }

    private fun parseSingleConfig(str: String): ProfileItem? {
        val config = if (str.startsWith(EConfigType.VMESS.protocolScheme)) {
            VmessFmt.parse(str)
        } else if (str.startsWith(EConfigType.SHADOWSOCKS.protocolScheme)) {
            ShadowsocksFmt.parse(str)
        } else if (str.startsWith(EConfigType.SOCKS.protocolScheme)) {
            SocksFmt.parse(str)
        } else if (str.startsWith(EConfigType.TROJAN.protocolScheme)) {
            TrojanFmt.parse(str)
        } else if (str.startsWith(EConfigType.VLESS.protocolScheme)) {
            VlessFmt.parse(str)
        } else if (str.startsWith(EConfigType.WIREGUARD.protocolScheme)) {
            WireguardFmt.parse(str)
        } else if (str.startsWith(EConfigType.HYSTERIA2.protocolScheme) || str.startsWith(HY2)) {
            Hysteria2Fmt.parse(str)
        } else {
            null
        }
        return config
    }
}