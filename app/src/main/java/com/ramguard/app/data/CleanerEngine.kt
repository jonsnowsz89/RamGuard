package com.ramguard.app.data

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class MemInfo(val totalMb: Long, val availMb: Long) {
    val usedMb: Long get() = totalMb - availMb
    val usedFraction: Float get() = if (totalMb == 0L) 0f else (1f - availMb.toFloat() / totalMb).coerceIn(0f, 1f)
}

data class CleanResult(val killed: Int, val freedMb: Long)

class CleanerEngine(
    private val context: Context,
    val settings: SettingsRepository,
    private val apps: AppRepository
) {
    private val am get() = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val nm get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Volatile private var protectedCache: Set<String> = emptySet()
    @Volatile private var protectedAt = 0L

    fun memInfo(): MemInfo {
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return MemInfo(mi.totalMem / MB, mi.availMem / MB)
    }

    /**
     * حزم محمية دائماً: التطبيق نفسه، الشاشة الرئيسية، لوحة المفاتيح، الاتصال والرسائل.
     * Cached for 60 s so a changed keyboard / default SMS app is picked up without a restart.
     */
    fun protectedPackages(): Set<String> {
        val now = System.currentTimeMillis()
        if (protectedCache.isNotEmpty() && now - protectedAt < PROTECTED_TTL) return protectedCache

        val set = mutableSetOf(context.packageName, "android", "com.android.systemui")
        context.packageManager
            .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName?.let(set::add)
        runCatching {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .enabledInputMethodList.forEach { set.add(it.packageName) }
        }
        runCatching { (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage?.let(set::add) }
        runCatching { Telephony.Sms.getDefaultSmsPackage(context)?.let(set::add) }
        protectedCache = set; protectedAt = now
        return set
    }

    /**
     * مسح الخلفية. [respectRecent] (Eco mode): spare apps used in the last 5 minutes, so only
     * apps that are not actively in use get stopped. Needs Usage Access; without it every
     * app counts as "not recent".
     */
    suspend fun sweep(respectRecent: Boolean = false): CleanResult = withContext(Dispatchers.Default) {
        val before = memInfo().availMb
        val keep = settings.whitelist.first() + protectedPackages()
        val cutoff = System.currentTimeMillis() - RECENT_MS
        val targets = apps.getRunningApps().filter {
            it.packageName !in keep && !(respectRecent && it.lastUsed > cutoff)
        }
        targets.forEach { runCatching { am.killBackgroundProcesses(it.packageName) } }
        System.gc()               // trims our own heap only; Android has no global "free RAM" API
        delay(700)                // let the kernel reclaim pages before measuring
        CleanResult(targets.size, (memInfo().availMb - before).coerceAtLeast(0))
    }

    /**
     * Packages whose internet the firewall must cut. Empty = firewall should be off.
     * Only user-visible (launchable) apps are blocked; background system services stay online,
     * and Google Play services / push services are always exempt so notifications of white-listed
     * messaging apps still arrive.
     */
    suspend fun firewallTargets(): List<String> {
        val active = settings.blockData.first() &&
            settings.mode.first() == PowerMode.ULTRA && !settings.isUnlocked()
        if (!active) return emptyList()
        val keep = settings.whitelist.first() + protectedPackages() + FIREWALL_EXEMPT
        return apps.getLaunchableApps(force = true).map { it.packageName }.filter { it !in keep }
    }

    suspend fun killOne(pkg: String) = withContext(Dispatchers.Default) {
        if (pkg !in protectedPackages()) am.killBackgroundProcesses(pkg)
    }

    /**
     * Single source of truth for Do-Not-Disturb: restricted only in ULTRA mode while the
     * manual communication window is closed. Safe to call repeatedly.
     */
    suspend fun syncDnd() {
        val restricted = settings.mode.first() == PowerMode.ULTRA && !settings.isUnlocked()
        applyDnd(restricted)
    }

    private suspend fun applyDnd(restricted: Boolean) {
        if (!nm.isNotificationPolicyAccessGranted) return
        val saved = settings.savedDnd.first()
        if (restricted) {
            if (saved == 0) {   // remember the user's own setting once, before overriding it
                val cur = nm.currentInterruptionFilter
                settings.setSavedDnd(if (cur == NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
                    NotificationManager.INTERRUPTION_FILTER_ALL else cur)
            }
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        } else if (saved != 0) {
            nm.setInterruptionFilter(saved)   // restore exactly what the user had
            settings.setSavedDnd(0)
        }
    }

    private companion object {
        const val MB = 1024L * 1024L
        const val PROTECTED_TTL = 60_000L
        const val RECENT_MS = 5 * 60_000L
        val FIREWALL_EXEMPT = setOf("com.google.android.gms", "com.google.android.gsf", "com.sec.spp.push")
    }
}
