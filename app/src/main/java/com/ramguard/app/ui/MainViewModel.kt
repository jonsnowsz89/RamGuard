package com.ramguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ramguard.app.CleanerApp
import com.ramguard.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as CleanerApp).container
    private fun <T> Flow<T>.state(init: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    // ---- Settings ----
    val mode = c.settings.mode.state(PowerMode.NORMAL)
    val whitelist = c.settings.whitelist.state(emptySet())
    val autoClean = c.settings.autoClean.state(true)
    val unlockUntil = c.settings.unlockUntil.state(0L)
    val blockData = c.settings.blockData.state(false)

    // ---- Live RAM ----
    val mem = flow { while (true) { emit(c.engine.memInfo()); delay(1000) } }.state(c.engine.memInfo())

    // ---- Running processes + search ----
    private val _running = MutableStateFlow<List<AppItem>>(emptyList())
    val query = MutableStateFlow("")
    val runningFiltered = combine(_running, query) { list, q -> filter(list, q) }.state(emptyList())

    // ---- White list picker + search ----
    private val _installed = MutableStateFlow<List<AppItem>>(emptyList())
    val wlQuery = MutableStateFlow("")
    val installedFiltered = combine(_installed, wlQuery, whitelist) { list, q, wl ->
        // protected apps first, then alphabetical
        filter(list, q).sortedWith(compareByDescending<AppItem> { it.packageName in wl }.thenBy { it.label.lowercase() })
    }.state(emptyList())

    // ---- Quick clean state ----
    private val _cleaning = MutableStateFlow(false)
    val cleaning = _cleaning.asStateFlow()
    private val _lastResult = MutableStateFlow<CleanResult?>(null)
    val lastResult = _lastResult.asStateFlow()

    init { refresh(); viewModelScope.launch { _installed.value = c.apps.getLaunchableApps() } }

    private fun filter(list: List<AppItem>, q: String) =
        if (q.isBlank()) list else list.filter {
            it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }

    fun refresh() = viewModelScope.launch { _running.value = c.apps.getRunningApps() }

    fun quickClean() {
        if (_cleaning.value) return
        viewModelScope.launch {
            _cleaning.value = true
            _lastResult.value = c.engine.sweep()
            _cleaning.value = false
            refresh()
        }
    }

    fun killOne(pkg: String) = viewModelScope.launch { c.engine.killOne(pkg); delay(300); refresh() }
    fun toggleWhitelist(pkg: String) = viewModelScope.launch { c.settings.toggleWhitelist(pkg) }
    fun setMode(m: PowerMode) = viewModelScope.launch { c.settings.setMode(m); c.engine.syncDnd() }
    fun syncDnd() = viewModelScope.launch { c.engine.syncDnd() }
    fun setBlockData(v: Boolean) = viewModelScope.launch { c.settings.setBlockData(v) }
    fun setAutoClean(v: Boolean) = viewModelScope.launch { c.settings.setAutoClean(v) }
    fun unlockFiveMinutes() = viewModelScope.launch { c.settings.unlockFor(5); c.engine.syncDnd() }
    fun lockNow() = viewModelScope.launch { c.settings.clearUnlock(); c.engine.syncDnd() }
    fun consumeResult() { _lastResult.value = null }
}
