package com.test.weareconnected.utils


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.test.weareconnected.notification.RideNotificationService

class RideBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "RESTART_SERVICE" || intent?.action == Intent.ACTION_BOOT_COMPLETED) {

            val serviceIntent = Intent(context, RideNotificationService::class.java)
            context?.startForegroundService(serviceIntent)
        }
    }
}
