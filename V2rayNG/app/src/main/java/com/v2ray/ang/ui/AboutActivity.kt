package com.v2ray.ang.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAboutBinding
import com.v2ray.ang.dto.CheckUpdateResult
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.handler.UpdateCheckerManager
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.Utils
import com.v2ray.ang.util.ZipUtil
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class AboutActivity : BaseActivity() {

    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        title = getString(R.string.about_title)

        binding.aboutTermsOfService.setOnClickListener {
            Utils.openUri(this, AppConfig.SAFE_BIT_TERMS_OF_SERVICE)
        }

        binding.aboutPrivacyPolicy.setOnClickListener {
            Utils.openUri(this, AppConfig.SAFE_BIT_PRIVACY_POLICY)
        }

        "v${BuildConfig.VERSION_NAME} (${SpeedtestManager.getLibVersion()})".also {
            binding.tvVersion.text = it
        }
    }

    override fun onDestroy() {
        Log.d(AppConfig.TAG, "onDestroy")
        super.onDestroy()
    }

    private fun checkForUpdates(includePreRelease: Boolean) {
        lifecycleScope.launch {
            val result = UpdateCheckerManager.checkForUpdate(includePreRelease)
            if (result.hasUpdate) {
                showUpdateDialog(result)
            } else {
                toast(R.string.update_already_latest_version)
            }
        }
    }

    private fun showUpdateDialog(result: CheckUpdateResult) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_new_version_found, result.latestVersion))
            .setMessage(result.releaseNotes)
            .setPositiveButton(R.string.update_now) { _, _ ->
                result.downloadUrl?.let {
                    Utils.openUri(this, it)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}