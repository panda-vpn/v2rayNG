package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.multiprocess.RemoteWorkManager
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAboutBinding
import com.v2ray.ang.databinding.ActivitySettingsBinding
import com.v2ray.ang.extension.toLongEx
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.service.SubscriptionUpdater
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.SettingsViewModel
import java.util.concurrent.TimeUnit

class SettingsActivity : BaseActivity() {

    private val binding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        title = getString(R.string.settings_title)

        binding.actionAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.settingsActionCollectLogs.setOnClickListener {
            startActivity(Intent(this, LogcatActivity::class.java))
        }
    }
}
