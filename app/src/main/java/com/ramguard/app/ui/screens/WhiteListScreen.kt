package com.ramguard.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramguard.app.R
import com.ramguard.app.ui.MainViewModel

@Composable
fun WhiteListScreen(vm: MainViewModel) {
    val apps by vm.installedFiltered.collectAsState()
    val wl by vm.whitelist.collectAsState()
    val q by vm.wlQuery.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(40.dp))
        Text(stringResource(R.string.whitelist_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.whitelist_hint, wl.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        SearchField(q, { vm.wlQuery.value = it }, stringResource(R.string.search_hint), Modifier.fillMaxWidth())
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.toggleWhitelist(app.packageName) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(app.packageName)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.packageName, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = app.packageName in wl, onCheckedChange = { vm.toggleWhitelist(app.packageName) })
                }
            }
        }
    }
}
