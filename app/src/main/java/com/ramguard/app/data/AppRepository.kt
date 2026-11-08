package com.ramguard.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isStopped: Boolean = false,
    val lastUsed: Long = 0L
)

class AppRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    @Volatile private var cache: List<AppItem> = emptyList()
    @Volatile private var cacheTime = 0L

    /**
     * كل التطبيقات القابلة للتشغيل. Flags come straight from the query result (no extra
     * getApplicationInfo per app) and the list is cached for 30 s unless [force] is set.
     */
    suspend fun getLaunchableApps(force: Boolean = false): List<AppItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && cache.isNotEmpty() && now - cacheTime < CACHE_MS) return@withContext cache

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val result = pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { ai ->
                AppItem(
                    packageName = ai.packageName,
                    label = pm.getApplicationLabel(ai).toString(),
                    isSystem = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    isStopped = ai.flags and ApplicationInfo.FLAG_STOPPED != 0
                )
            }
            .sortedBy { it.label.lowercase() }
        cache = result; cacheTime = now
        result
    }

    /**
     * التطبيقات العاملة في الخلفية (تقديرية).
     * getRunningAppProcesses() returns only OUR process on Android 5.1+, so we use the heuristic
     * "launchable app that is not in the stopped state". Note: killBackgroundProcesses() does not set
     * the stopped flag, so a killed app may still be listed until it is force-stopped.
     * Sorted by last-used time when Usage Access is granted.
     */
    suspend fun getRunningApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val lastUsed = lastUsedMap()
        getLaunchableApps(force = true)
            .filter { !it.isStopped }
            .map { it.copy(lastUsed = lastUsed[it.packageName] ?: 0L) }
            .sortedByDescending { it.lastUsed }
    }

    /** Requires PACKAGE_USAGE_STATS special access; empty map otherwise. */
    private fun lastUsedMap(): Map<String, Long> = runCatching {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 24 * 3600_000L, end)
            .groupBy { it.packageName }
            .mapValues { (_, v) -> v.maxOf { it.lastTimeUsed } }
    }.getOrDefault(emptyMap())

    private companion object { const val CACHE_MS = 30_000L }
}
