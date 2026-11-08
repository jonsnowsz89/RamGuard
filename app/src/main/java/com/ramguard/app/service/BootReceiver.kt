package com.ramguard.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** يشغّل الخدمة بعد إقلاع الجهاز / Starts the cleaner service after reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            ContextCompat.startForegroundService(context, Intent(context, CleanerService::class.java))
        }
    }
}
