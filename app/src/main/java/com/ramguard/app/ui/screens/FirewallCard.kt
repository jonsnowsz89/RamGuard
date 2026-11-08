package com.ramguard.app.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramguard.app.R
import com.ramguard.app.service.FirewallVpnService
import com.ramguard.app.ui.MainViewModel

/** Opt-in switch for the VPN firewall; asks for the system VPN consent dialog the first time. */
@Composable
fun FirewallCard(vm: MainViewModel) {
    val ctx = LocalContext.current
    val enabled by vm.blockData.collectAsState()
    val running by FirewallVpnService.running.collectAsState()
    val blocked by FirewallVpnService.blockedCount.collectAsState()

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) vm.setBlockData(true)
    }

    Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.firewall_title), Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Switch(checked = enabled, onCheckedChange = { on ->
                    if (!on) vm.setBlockData(false)
                    else {
                        val intent = VpnService.prepare(ctx)          // null = consent already granted
                        if (intent == null) vm.setBlockData(true) else consent.launch(intent)
                    }
                })
            }
            Text(stringResource(R.string.firewall_desc), fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (enabled) {
                Text(
                    if (running) stringResource(R.string.firewall_active, blocked) else stringResource(R.string.firewall_idle),
                    fontWeight = FontWeight.Medium,
                    color = if (running) RamGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
