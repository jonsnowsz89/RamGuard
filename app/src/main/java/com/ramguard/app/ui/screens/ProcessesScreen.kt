package com.ramguard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramguard.app.R
import com.ramguard.app.ui.MainViewModel

@Composable
fun ProcessesScreen(vm: MainViewModel) {
    val list by vm.runningFiltered.collectAsState()
    val query by vm.query.collectAsState()
    val whitelist by vm.whitelist.collectAsState()
    val mem by vm.mem.collectAsState()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_processes), Modifier.weight(1f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            RamGauge(mem, size = 56.dp, stroke = 6.dp, compact = true)
        }
        Spacer(Modifier.height(12.dp))
        SearchField(query, { vm.query.value = it }, stringResource(R.string.search_hint), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.count_apps, list.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.packageName }) { app ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(app.packageName)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.packageName, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // زر البحث عن التطبيق في المتصفح / web-search button
                    IconButton({ searchWeb(ctx, app.packageName) }) {
                        Icon(Icons.Rounded.Public, stringResource(R.string.search_web), tint = MaterialTheme.colorScheme.primary)
                    }
                    // Kill this app (disabled for whitelisted ones)
                    IconButton({ vm.killOne(app.packageName) }, enabled = app.packageName !in whitelist) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.kill))
                    }
                }
            }
        }
    }
}
