package com.v2ray.ang.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.model.Conf
import com.v2ray.ang.model.FREE_TYPE_VIP0
import com.v2ray.ang.model.Latency
import com.v2ray.ang.model.NODE_ID_AUTO_SELECT
import com.v2ray.ang.model.NODE_TYPE_LOCATION
import com.v2ray.ang.model.Node
import com.v2ray.ang.model.UserProfile
import com.v2ray.ang.utilx.NetworkUtils
import com.v2ray.ang.utilx.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class NodeListFragment(private val nodeType: Int) : Fragment() {

    companion object {
        private const val TAG = "NodeListFragment"
    }

    private var refreshGo = AtomicBoolean(false)
    private var displayNodes = mutableListOf<Node>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.layout_node_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.node_list)
        val noAvailableText = view.findViewById<TextView>(R.id.node_list_no_available)
        val refreshButton = view.findViewById<MaterialButton>(R.id.node_list_refresh)
        val refreshIndicator = view.findViewById<CircularProgressIndicator>(R.id.node_refresh_indicator)

        refreshIndicator.isIndeterminate = true

        if (UserProfile.isSyncComplete()) {
            buildDisplayNodes()

            val adapter = NodeListAdapter(requireContext(), displayNodes)
            NodeSelected.nodeId.observe(viewLifecycleOwner) {
                adapter.notifyDataSetChanged()
                listView.invalidateViews()
            }

            listView.adapter = adapter
            listView.divider?.alpha = 38        // 255 * 0.15
            listView.divider?.alpha = 96        // 255 * 0.38

            listView.visibility = View.VISIBLE
            noAvailableText.visibility = View.GONE
            refreshButton.visibility = View.GONE
            refreshIndicator.visibility = View.GONE
        } else {
            listView.visibility = View.GONE
            noAvailableText.visibility = View.VISIBLE
            refreshButton.visibility = View.VISIBLE
            refreshIndicator.visibility = View.GONE
        }

        refreshButton.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                ToastUtils.showShort(requireContext(), R.string.toast_network_unavailable)
                return@setOnClickListener
            }

            if (refreshGo.compareAndSet(false, true)) {
                refreshIndicator.visibility = View.VISIBLE
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // delay(5000)
                        UserProfile.sync()
                    } finally {
                        refreshGo.set(false)
                        lifecycleScope.launch(Dispatchers.Main) {
                            refreshIndicator.visibility = View.GONE
                            if (UserProfile.isSyncComplete()) {
                                buildDisplayNodes()

                                val adapter = NodeListAdapter(requireContext(), displayNodes)
                                listView.adapter = adapter
                                listView.divider?.alpha = 38  // 255 * 0.15

                                NodeSelected.nodeId.observe(viewLifecycleOwner) {
                                    adapter.notifyDataSetChanged()
                                    listView.invalidateViews()
                                }

                                listView.visibility = View.VISIBLE
                                noAvailableText.visibility = View.GONE
                                refreshButton.visibility = View.GONE
                                refreshIndicator.visibility = View.GONE
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "refresh go!!!")
            }
        }
    }

    class NodeListAdapter(
        private val context: Context,
        nodes: List<Node>
    ): ArrayAdapter<Node>(context, R.layout.item_node, nodes) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_node, parent, false)
            val item = getItem(position)

            val nodeIcon = view.findViewById<ImageView>(R.id.node_item_icon)
            val nodeRtt = view.findViewById<TextView>(R.id.node_item_rtt)
            val vipIcon = view.findViewById<ImageView>(R.id.node_item_vip)
            val freeIcon = view.findViewById<TextView>(R.id.node_item_free)

            var rtt : Long? = null

            if (item?.id != NODE_ID_AUTO_SELECT) {
                nodeIcon.setBackgroundColor(Color.TRANSPARENT)
                val iconUrl = "${Conf.assetsUrl}/flags/${item!!.icon}.png"
                Glide.with(context)
                    .load(iconUrl)
                    .placeholder(R.drawable.location_on_24px)
                    .error(R.drawable.error_24px)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(nodeIcon)

                if (item.freeType == FREE_TYPE_VIP0) {
                    vipIcon.visibility = View.GONE
                    freeIcon.visibility = View.VISIBLE
                } else {
                    vipIcon.visibility = View.VISIBLE
                    freeIcon.visibility = View.GONE
                }

                rtt = Latency.getRttByHostIp(item.host)
            } else {
                nodeIcon.setBackgroundColor(com.google.android.material.R.attr.colorOnBackground)
                nodeIcon.setImageResource(R.drawable.location_on_24px)

                vipIcon.visibility = View.GONE
                freeIcon.visibility = View.GONE

                rtt = Latency.getMinRtt()
            }

            val stt = if (rtt != null) "${rtt /2}ms" else "--"
            nodeRtt.text = stt

            when {
                rtt == null -> nodeRtt.setTextColor(Color.RED)
                rtt <= Conf.nodeLatencyGoodInMillis -> nodeRtt.setTextColor(0xFF008B00.toInt())
                rtt <= Conf.nodeLatencyAvgInMillis -> nodeRtt.setTextColor(0xFFFF7F24.toInt())
                else -> nodeRtt.setTextColor(Color.RED)
            }

            view.findViewById<TextView>(R.id.node_item_name).text = item.name

            val radioButton = view.findViewById<MaterialRadioButton>(R.id.node_item_select)
            radioButton.isChecked = item.id == NodeSelected.nodeId.value
            radioButton.setOnClickListener{
                NodeSelected.nodeId.value = item.id
                UserProfile.setChoiceNodeId(item.id)
            }

            return view
        }
    }

    private fun buildDisplayNodes()  {
        displayNodes.clear()

        var notFound = true

        if (nodeType == NODE_TYPE_LOCATION) {
            val autoEnt = Node(NODE_ID_AUTO_SELECT)
            autoEnt.name = getString(R.string.node_auto_select)
            displayNodes.add(autoEnt)
        }

        for (item in UserProfile.nodes.get()) {
            if (item.nodeType == nodeType) {
                displayNodes.add(item)
            }
            if (item.id == NodeSelected.nodeId.value) {
                notFound = false
            }
        }

        if (notFound) {
            NodeSelected.nodeId.value = NODE_ID_AUTO_SELECT
        }

        displayNodes.sortWith(Comparator { lhs, rhs ->
            when {
                lhs.id == NODE_ID_AUTO_SELECT -> -1
                rhs.id == NODE_ID_AUTO_SELECT -> 1
                lhs.freeType == FREE_TYPE_VIP0 -> -1
                rhs.freeType == FREE_TYPE_VIP0 -> 1
                lhs.id == NodeSelected.nodeId.value -> -1
                rhs.id == NodeSelected.nodeId.value -> 1
                else ->  {
                    val lhsRtt = Latency.getRttByHostIp(lhs.host)
                    val rhsRtt = Latency.getRttByHostIp(rhs.host)
                    when {
                        lhsRtt == null -> -1
                        rhsRtt == null -> 1
                        lhsRtt < rhsRtt -> 1
                        else -> -1
                    }
                }
            }
        })
    }
}