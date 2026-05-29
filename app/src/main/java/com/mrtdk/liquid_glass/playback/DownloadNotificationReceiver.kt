package com.mrtdk.liquid_glass.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mrtdk.liquid_glass.MainActivity

class DownloadNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to_downloads", true)
            }
            context.startActivity(launchIntent)
        }
    }
}
