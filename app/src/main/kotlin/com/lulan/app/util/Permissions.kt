package com.lulan.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object Permissions {
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        val pkg = activity.packageName
        if (!pm.isIgnoringBatteryOptimizations(pkg)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
            }
            activity.startActivity(intent)
        }
    }
}
