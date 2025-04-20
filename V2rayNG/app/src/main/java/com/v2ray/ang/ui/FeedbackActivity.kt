package com.v2ray.ang.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
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
import com.v2ray.ang.databinding.ActivityFeedbackBinding
import com.v2ray.ang.utilx.ToastUtils

class FeedbackActivity : BaseActivity() {

    private val binding by lazy { ActivityFeedbackBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        title = getString(R.string.feedback_title)

        binding.feedbackMore.setOnClickListener {
            feedbackMore()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun feedbackMore() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"  // 邮件MIME类型
            putExtra(Intent.EXTRA_EMAIL, arrayOf("doingli@foxmail.com"))  // 收件人
            putExtra(Intent.EXTRA_SUBJECT, "SafeBit feedback more")  // 主题
            putExtra(Intent.EXTRA_TEXT, "here is content")  // 正文
        }

        try {
            startActivity(Intent.createChooser(intent, "Choose email client"))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "not found email client", Toast.LENGTH_SHORT).show()
        }
    }

}