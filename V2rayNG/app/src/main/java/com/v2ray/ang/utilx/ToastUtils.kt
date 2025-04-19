package com.v2ray.ang.utilx

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

object ToastUtils {

    private var toast: Toast? = null

    fun showShort(context: Context, message: String) {
        show(context, message, Toast.LENGTH_SHORT)
    }

    fun showShort(context: Context, @StringRes resId: Int) {
        show(context, context.getString(resId), Toast.LENGTH_SHORT)
    }

    fun showLong(context: Context, message: String) {
        show(context, message, Toast.LENGTH_LONG)
    }

    fun showLong(context: Context, @StringRes resId: Int) {
        show(context, context.getString(resId), Toast.LENGTH_LONG)
    }

    private fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        toast?.cancel() // 取消之前的Toast
        toast = Toast.makeText(context.applicationContext, message, duration)
        toast?.show()
    }
}