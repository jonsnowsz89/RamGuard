package com.ramguard.app.service

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.ramguard.app.CleanerApp
import com.ramguard.app.data.CleanerEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileInputStream
import java.io.IOException
import kotlin.concurrent.thread

/**
 * جدار ناري محلي (Black-hole VPN).
 *
 * How it works: only the apps we want to block are added with addAllowedApplication(), so ONLY their
 * traffic is routed into our TUN interface. We read every packet and throw it away, so those apps
 * have no internet. All other apps (white list, protected apps, Google Play services for push) never
 * touch the VPN and keep working normally. No data ever leaves the device through this VPN.
 *
 * Which apps are blocked is decided by [CleanerEngine.firewallTargets] (ULTRA mode, opt-in switch on,
 * manual communication window closed).
 */
class FirewallVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private var tun: ParcelFileDescriptor? = null
    private val engine: CleanerEngine by lazy { (application as CleanerApp).container.engine }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch { lock.withLock { teardown() }; stopSelf() }
            return START_NOT_STICKY
        }
        // Also reached with a null intent when the system restarts us: targets are recomputed.
        scope.launch { establish() }
        return START_STICKY
    }

    private suspend fun establish() = lock.withLock {
        val targets = engine.firewallTargets()
        // Not allowed to run: no targets, or VPN consent was never given / was revoked
        if (targets.isEmpty() || VpnService.prepare(this) != null) {
            teardown(); stopSelf(); return
        }
        val builder = Builder()
            .setSession("RamGuard")
            .addAddress("10.8.0.2", 32)
            .addAddress("fd00:8::2", 128)
            .addRoute("0.0.0.0", 0)      // everything from the selected apps goes into the TUN...
            .addRoute("::", 0)
            .setBlocking(true)
        if (Build.VERSION.SDK_INT >= 29) builder.setMetered(false)
        var added = 0
        targets.forEach {
            try { builder.addAllowedApplication(it); added++ } catch (_: PackageManager.NameNotFoundException) {}
        }
        if (added == 0) { teardown(); stopSelf(); return }   // never route ALL apps by accident

        val newTun = builder.establish() ?: return           // null = consent revoked meanwhile
        val old = tun
        tun = newTun
        drain(newTun)                                        // ...where every packet is dropped
        old?.close()                                         // closing the old fd ends its drain thread
        blockedCount.value = added
        _running.value = true
    }

    /** Read and discard packets so the TUN queue never backs up. */
    private fun drain(fd: ParcelFileDescriptor) {
        thread(name = "ramguard-drain", isDaemon = true) {
            val input = FileInputStream(fd.fileDescriptor)
            val buf = ByteArray(32 * 1024)
            try { while (input.read(buf) >= 0) { /* drop */ } } catch (_: IOException) {}
        }
    }

    private fun teardown() {
        runCatching { tun?.close() }
        tun = null
        blockedCount.value = 0
        _running.value = false
    }

    /** User removed our VPN permission from system settings: turn the feature off cleanly. */
    override fun onRevoke() {
        scope.launch {
            engine.settings.setBlockData(false)
            lock.withLock { teardown() }
            stopSelf()
        }
        super.onRevoke()
    }

    override fun onDestroy() {
        teardown()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.ramguard.app.FIREWALL_STOP"
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()
        val blockedCount = MutableStateFlow(0)
    }
}
