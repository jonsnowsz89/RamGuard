package com.ramguard.app.service

import android.app.*
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ramguard.app.CleanerApp
import com.ramguard.app.MainActivity
import com.ramguard.app.R
import com.ramguard.app.data.PowerMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Foreground service that owns the dynamic SCREEN_OFF / SCREEN_ON receiver
 * (these broadcasts are not delivered to manifest-declared receivers).
 */
class CleanerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sweepJob: Job? = null
    private val engine by lazy { (application as CleanerApp).container.engine }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> sweepJob?.cancel()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Firewall follows (mode, unlock window, switch, white list); re-opens/re-closes at window expiry
        scope.launch {
            combine(
                engine.settings.mode, engine.settings.unlockUntil,
                engine.settings.blockData, engine.settings.whitelist
            ) { _, until, _, _ -> until }
                .collectLatest { until ->
                    syncFirewall()
                    val remaining = until - System.currentTimeMillis()
                    if (remaining > 0) { delay(remaining); syncFirewall() }
                }
        }
        // DND follows (mode, unlock window): restricted only in ULTRA while the window is closed.
        // collectLatest + delay re-restricts automatically when the unlock window expires.
        scope.launch {
            combine(engine.settings.mode, engine.settings.unlockUntil) { m, u -> m to u }
                .collectLatest { (_, until) ->
                    engine.syncDnd()
                    val remaining = until - System.currentTimeMillis()
                    if (remaining > 0) { delay(remaining); engine.syncDnd() }
                }
        }
    }

    private suspend fun syncFirewall() {
        val need = engine.firewallTargets().isNotEmpty()
        runCatching {
            if (need) {
                startService(Intent(this, FirewallVpnService::class.java))
            } else if (FirewallVpnService.running.value) {
                startService(Intent(this, FirewallVpnService::class.java).setAction(FirewallVpnService.ACTION_STOP))
            }
        }
        Unit
    }

    private fun onScreenOff() {
        sweepJob?.cancel()
        sweepJob = scope.launch {
            val s = engine.settings
            if (!s.autoClean.first()) return@launch
            delay(3_000)                                   // small grace period
            while (isActive) {
                val mode = s.mode.first()
                // Eco spares recently used apps; Ultra stops everything not white-listed
                if (!s.isUnlocked()) engine.sweep(respectRecent = mode == PowerMode.ECO)
                if (mode == PowerMode.NORMAL) break
                delay(10 * 60_000L)                        // Eco/Ultra: keep sweeping while screen is off
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN)
        )
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "ramguard_service"
        const val NOTIF_ID = 1
    }
}
