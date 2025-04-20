package com.v2ray.ang.ui

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.os.Build
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityAboutBinding
import com.v2ray.ang.databinding.ActivityVipBinding
import com.v2ray.ang.dto.CheckUpdateResult
import com.v2ray.ang.extension.toast
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.handler.UpdateCheckerManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat

class VipActivity : BaseActivity() {

    private val binding by lazy { ActivityVipBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 设置状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        }

        // 让内容延伸到状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)

        supportActionBar?.hide()

        binding.vipClose.setOnClickListener {
            finish()
        }

        binding.vipYearlyPlanOriginalPrice.paint.flags = Paint. STRIKE_THRU_TEXT_FLAG; //中划线
    }

    override fun onDestroy() {
        Log.d(AppConfig.TAG, "onDestroy")
        super.onDestroy()
    }
}