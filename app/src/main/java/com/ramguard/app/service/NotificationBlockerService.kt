package com.ramguard.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ramguard.app.CleanerApp
import com.ramguard.app.data.CleanerEngine
import com.ramguard.app.data.PowerMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Eco   : cancels non-essential notifications of non-whitelisted apps. Calls, alarms, navigation,
 *         MESSAGES, media controls and ongoing notifications are kept.
 * Ultra : cancels everything of non-whitelisted apps except calls and alarms.
 * Needs "Notification access". Note: the notification is removed right after it is posted,
 * so a very brief sound/vibration may still occur.
 */
class NotificationBlockerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var engine: CleanerEngine
    @Volatile private var mode = PowerMode.NORMAL
    @Volatile private var whitelist: Set<String> = emptySet()
    @Volatile private var unlockUntil = 0L

    override fun onListenerConnected() {
        val c = (application as CleanerApp).container
        engine = c.engine
        scope.launch { c.settings.mode.collect { mode = it } }
        scope.launch { c.settings.whitelist.collect { whitelist = it } }
        scope.launch { c.settings.unlockUntil.collect { unlockUntil = it } }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!::engine.isInitialized || mode == PowerMode.NORMAL) return
        if (System.currentTimeMillis() < unlockUntil) return
        val pkg = sbn.packageName
        // protectedPackages() is cached (60 s), so a changed keyboard/SMS app is picked up
        if (pkg in whitelist || pkg in engine.protectedPackages()) return

        val cat = sbn.notification.category
        if (cat == Notification.CATEGORY_CALL || cat == Notification.CATEGORY_ALARM ||
            cat == Notification.CATEGORY_NAVIGATION) return

        when (mode) {
            PowerMode.ECO -> if (!sbn.isOngoing &&
                cat != Notification.CATEGORY_TRANSPORT && cat != Notification.CATEGORY_MESSAGE) {
                cancelNotification(sbn.key)
            }
            PowerMode.ULTRA -> cancelNotification(sbn.key)
            else -> Unit
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
