package com.ramguard.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.ramguard.app.R
import com.ramguard.app.ui.MainViewModel
import com.ramguard.app.ui.Permissions

@Composable
fun HomeScreen(vm: MainViewModel) {
    val mem by vm.mem.collectAsState()
    val status = when {
        mem.usedFraction < 0.60f -> R.string.ram_status_good
        mem.usedFraction < 0.80f -> R.string.ram_status_mid
        else -> R.string.ram_status_high
    }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 40.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text(stringResource(R.string.app_name), fontSize = 32.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RamGauge(mem, size = 220.dp, stroke = 20.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(status), fontWeight = FontWeight.SemiBold, color = ramColor(mem.usedFraction))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ram_detail, mem.availMb, mem.totalMb),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { PermissionsCard() }
    }
}

private data class PermItem(val label: Int, val action: String, val uri: String? = null)

/** Shows only the special accesses still missing; re-checks every time the app resumes. */
@Composable
fun PermissionsCard() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    val items = remember(tick) {
        buildList {
            if (!Permissions.usageAccess(ctx)) add(PermItem(R.string.perm_usage, Settings.ACTION_USAGE_ACCESS_SETTINGS))
            if (!Permissions.notificationListener(ctx)) add(PermItem(R.string.perm_listener, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            if (!Permissions.dnd(ctx)) add(PermItem(R.string.perm_dnd, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            if (!Permissions.batteryUnrestricted(ctx)) add(
                PermItem(R.string.perm_battery, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${ctx.packageName}")
            )
        }
    }
    if (items.isEmpty()) return
    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.permissions), fontWeight = FontWeight.SemiBold)
            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(item.label), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton({
                        val i = Intent(item.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        item.uri?.let { i.data = Uri.parse(it) }
                        ctx.startActivity(i)
                    }) { Text(stringResource(R.string.grant)) }
                }
            }
        }
    }
}
