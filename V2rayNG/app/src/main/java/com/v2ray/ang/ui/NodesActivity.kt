package com.v2ray.ang.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityNodeBinding
import com.v2ray.ang.model.NODE_ID_AUTO_SELECT
import com.v2ray.ang.model.NODE_TYPE_LOCATION
import com.v2ray.ang.model.NODE_TYPE_STREAMING
import com.v2ray.ang.model.UserProfile

object NodeSelected {
    val nodeId = MutableLiveData<Int>(NODE_ID_AUTO_SELECT)
}

class NodesActivity : BaseActivity() {

    companion object {
        private const val TAG = "NodesActivity"
    }

    private val binding by lazy { ActivityNodeBinding.inflate(layoutInflater) }

    private var fragmentList = mutableListOf<NodeListFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        title = getString(R.string.node_title)

        Log.d(TAG, "onCreate")

        NodeSelected.nodeId.value = UserProfile.getSelectedNodeId()

        fragmentList.add(NodeListFragment(NODE_TYPE_LOCATION))
        fragmentList.add(NodeListFragment(NODE_TYPE_STREAMING))

        var currentTab = 0
        for (item in UserProfile.nodes.get()) {
            if (item.nodeType == NODE_TYPE_STREAMING && item.id == NodeSelected.nodeId.value) {
                currentTab = 1
            }
        }

        binding.layoutNodeVp2.adapter = ViewPagerAdapter(fragmentList, this)
        binding.layoutNodeVp2.setCurrentItem(currentTab, false)
        TabLayoutMediator(binding.layoutNodeTab, binding.layoutNodeVp2) { tab, position ->
            when(position) {
                0 -> tab.setText(R.string.node_location)
                1-> tab.setText(R.string.node_streaming)
            }
        }.attach()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    class ViewPagerAdapter(
        private val fragments: List<NodeListFragment>,
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int {
            return fragments.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }
}