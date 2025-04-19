package com.v2ray.ang.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAboutBinding
import com.v2ray.ang.databinding.ActivityAccountBinding
import com.v2ray.ang.utilx.ToastUtils


class AccountActivity : BaseActivity() {

    companion object {
        private const val TAG = "AccountActivity"
    }

    private val binding by lazy { ActivityAccountBinding.inflate(layoutInflater) }

    private lateinit var deviceId: String

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        title = getString(R.string.account_title)

        this.deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
        binding.accountUserIdV.text = this.deviceId

        binding.layoutAccountType.setOnClickListener {

        }

        binding.layoutAccountUserId.setOnClickListener {
            onCopyDeviceId()
        }

        binding.accountActionUpgrade.setOnClickListener{
            /*
            val intent = Intent(this, VipActivity::class.java)
            startActivity(intent)
             */
        }

        binding.accountUserIdCopy.setOnClickListener{
            onCopyDeviceId()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun onCopyDeviceId() {
        val clipboard = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", this.deviceId)
        clipboard.setPrimaryClip(clip)
        ToastUtils.showShort(this, R.string.toast_copied)
    }
}