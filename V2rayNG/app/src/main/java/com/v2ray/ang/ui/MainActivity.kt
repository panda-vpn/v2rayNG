package com.v2ray.ang.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityMainBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.ProfileManager
import com.v2ray.ang.model.UserProfile
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.utilx.NetworkUtils
import com.v2ray.ang.utilx.update.OkHttpUpdateHttpServiceImpl
import com.v2ray.ang.utilx.update.XUpdateParserImpl
import com.v2ray.ang.viewmodel.MainViewModel
import com.xuexiang.xupdate.XUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val adapter by lazy { MainRecyclerAdapter(this) }
    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            startV2Ray()
        }
    }

    private val tabGroupListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            val selectId = tab?.tag.toString()
            if (selectId != mainViewModel.subscriptionId) {
                mainViewModel.subscriptionIdChanged(selectId)
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
        }
    }
    private var mItemTouchHelper: ItemTouchHelper? = null
    val mainViewModel: MainViewModel by viewModels()

    // register activity result for requesting permission
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                when (pendingAction) {
                    Action.POST_NOTIFICATIONS -> {}
                    else -> {}
                }
            } else {
                toast(R.string.toast_permission_denied)
            }
            pendingAction = Action.NONE
        }

    private var pendingAction: Action = Action.NONE

    enum class Action {
        NONE,
        IMPORT_QR_CODE_CONFIG,
        READ_CONTENT_FROM_URI,
        POST_NOTIFICATIONS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        title = getString(R.string.app_name)
        setSupportActionBar(binding.toolbar)

        val speed = binding.animationPowerOff.speed
        binding.animationPowerOff.visibility = View.VISIBLE
        binding.animationPowerConnecting.visibility = View.INVISIBLE
        binding.animationPowerOn.visibility = View.INVISIBLE

        binding.animationPowerConnecting.speed = speed * 2
        binding.animationPowerOn.speed = speed / 2

        binding.animationPowerOff.setOnClickListener { onPowerClicked() }
        binding.animationPowerConnecting.setOnClickListener  { onPowerClicked() }
        binding.animationPowerOn.setOnClickListener  { onPowerClicked() }

        /*
        binding.fab.setOnClickListener {
            if (mainViewModel.isRunning.value == true) {
                V2RayServiceManager.stopVService(this)
            } else if ((MmkvManager.decodeSettingsString(AppConfig.PREF_MODE) ?: VPN) == VPN) {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startV2Ray()
                } else {
                    requestVpnPermission.launch(intent)
                }
            } else {
                startV2Ray()
            }
        }
        */

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)

        setupViewModel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                pendingAction = Action.POST_NOTIFICATIONS
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        networkCallback = NetworkUtils.registerNetworkCallback(
            context = this,
            onAvailable = {
                /*
                runOnUiThread {
                    // binding.redHeaderNotificationText.visibility = View.INVISIBLE
                }
                */
                Log.i(TAG, "network available")
                UserProfile.sync()
            },
            onLost = {
                Log.i(TAG, "network lost")
                /*
                runOnUiThread {
                    binding.redHeaderNotificationText.text = getText(R.string.red_header_network_unavailable)
                    binding.redHeaderNotificationText.visibility = View.VISIBLE
                }
                */
            }
        )

        val xUpdateParams: Map<String, Any> = mapOf(
            "deviceId" to UserProfile.user.deviceId,
            "channel" to BuildConfig.CHANNEL,
            "ver" to BuildConfig.VERSION_NAME,
            "lang" to "en"
        )
        XUpdate.get()
            .debug(true)
            .isGet(true)
            .params(xUpdateParams)
            .setIUpdateParser(XUpdateParserImpl())
            .setIUpdateHttpService(OkHttpUpdateHttpServiceImpl())
            .init(AngApplication.application)

        XUpdate.newBuild(this)
            .updateUrl(BuildConfig.VERSION_UPDATER_URL)
            .supportBackgroundUpdate(true)
            .update()
        // updateApp(BuildConfig.VERSION_UPDATER_URL, UpdateAppHttpUtil()).update()

    }

    private fun onPowerClicked() {
        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(this)
        } else if ((MmkvManager.decodeSettingsString(AppConfig.PREF_MODE) ?: VPN) == VPN) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray()
            } else {
                requestVpnPermission.launch(intent)
            }
        } else {
            startV2Ray()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViewModel() {
        mainViewModel.updateListAction.observe(this) { index ->
            if (index >= 0) {
                adapter.notifyItemChanged(index)
            } else {
                adapter.notifyDataSetChanged()
            }
        }
        mainViewModel.isRunning.observe(this) { isRunning ->
            adapter.isRunning = isRunning
            if (isRunning) {
                binding.connectionStatus.text = getString(R.string.home_conn_connected)
                binding.animationPowerOff.visibility = View.INVISIBLE
                binding.animationPowerConnecting.visibility = View.INVISIBLE
                binding.animationPowerOn.visibility = View.VISIBLE
            } else {
                binding.connectionStatus.text = getString(R.string.home_conn_tap_to_open)
                binding.animationPowerOff.visibility = View.VISIBLE
                binding.animationPowerConnecting.visibility = View.INVISIBLE
                binding.animationPowerOn.visibility = View.INVISIBLE
            }
        }
        mainViewModel.startListenBroadcast()
        mainViewModel.initAssets(assets)
    }

    private fun startV2Ray() {
        val ok = ProfileManager.setSelectedServer()
        // if (MmkvManager.getSelectServer().isNullOrEmpty()) {
        if (!ok) {
            toast(R.string.title_file_chooser)
            return
        }
        V2RayServiceManager.startVService(this)
    }

    private fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            V2RayServiceManager.stopVService(this)
        }
        lifecycleScope.launch {
            delay(500)
            startV2Ray()
        }
    }

    public override fun onResume() {
        super.onResume()
        mainViewModel.reloadServerList()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_nodes -> {
            Log.d(AppConfig.TAG, "******* nodes")
            startActivity(Intent(this, NodesActivity::class.java))
            true
        }

        R.id.menu_vip -> {
            startActivity(Intent(this, VipActivity::class.java))
            true
        }

        else -> super.onOptionsItemSelected(item)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.drawer_menu_account -> startActivity(Intent(this, AccountActivity::class.java))
            R.id.drawer_menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.drawer_menu_app_filter -> startActivity(Intent(this, PerAppProxyActivity::class.java))
            R.id.drawer_menu_feedback -> startActivity(Intent(this, FeedbackActivity::class.java))
            R.id.drawer_menu_share -> shareApp()
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true

    }

    private fun shareApp() {
        val text = "SafeBit VPN, come with me!\n" +
                "https://play.google.com/store/apps/details?id=com.safebit.android"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Tell your friends"))
    }

    override fun onDestroy() {
        NetworkUtils.unregisterNetworkCallback(this, networkCallback)

        super.onDestroy()
    }
}