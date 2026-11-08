package com.ramguard.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramguard.app.R
import com.ramguard.app.data.PowerMode
import com.ramguard.app.ui.MainViewModel

@Composable
fun ModesScreen(vm: MainViewModel) {
    val mode by vm.mode.collectAsState()
    val auto by vm.autoClean.collectAsState()
    val unlockUntil by vm.unlockUntil.collectAsState()
    // Re-evaluate "unlocked" once per second so the button state is live
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) } }
    val unlocked = now < unlockUntil

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 40.dp, 20.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(stringResource(R.string.tab_modes), fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { ModeCard(Icons.Rounded.BatteryFull, R.string.mode_normal, R.string.mode_normal_desc, mode == PowerMode.NORMAL) { vm.setMode(PowerMode.NORMAL) } }
        item { ModeCard(Icons.Rounded.Eco, R.string.mode_eco, R.string.mode_eco_desc, mode == PowerMode.ECO) { vm.setMode(PowerMode.ECO) } }
        item { ModeCard(Icons.Rounded.BatteryAlert, R.string.mode_ultra, R.string.mode_ultra_desc, mode == PowerMode.ULTRA) { vm.setMode(PowerMode.ULTRA) } }

        if (mode != PowerMode.NORMAL) item {
            // "Allow internet/communication manually" window for Ultra (and Eco)
            Button(
                onClick = { if (unlocked) vm.lockNow() else vm.unlockFiveMinutes() },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.large
            ) {
                val left = ((unlockUntil - now) / 1000).coerceAtLeast(0)
                Text(if (unlocked) stringResource(R.string.lock_now, left) else stringResource(R.string.unlock_5min))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.auto_clean), Modifier.weight(1f))
                    Switch(auto, { vm.setAutoClean(it) })
                }
            }
        }
        item { FirewallCard(vm) }
        item { PermissionsCard() }
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: Int, desc: Int, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(bg), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(stringResource(desc), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            RadioButton(selected, onClick)
        }
    }
}
