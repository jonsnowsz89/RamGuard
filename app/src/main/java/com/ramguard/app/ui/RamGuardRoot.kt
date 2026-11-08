package com.ramguard.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramguard.app.R
import com.ramguard.app.ui.screens.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private enum class Tab(val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME(R.string.tab_home, Icons.Rounded.Home),
    PROCESSES(R.string.tab_processes, Icons.Rounded.Memory),
    WHITELIST(R.string.tab_whitelist, Icons.Rounded.Shield),
    MODES(R.string.tab_modes, Icons.Rounded.Eco)
}

@Composable
fun RamGuardRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val cleaning by vm.cleaning.collectAsState()
    val result by vm.lastResult.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.syncDnd() }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }

    // Satisfying finish: second haptic + snackbar with the numbers
    val msg = result?.let { stringResource(R.string.cleaned_result, it.killed, it.freedMb) }
    LaunchedEffect(result) {
        if (result != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbar.showSnackbar(msg ?: "")
            vm.consumeResult()
        }
    }

    // Pulsing animation while cleaning
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "scale"
    )

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Black,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // الزر العائم / Prominent floating Quick Clean button
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.quickClean()
                },
                modifier = Modifier.scale(if (cleaning) pulse else 1f),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    if (cleaning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Rounded.Bolt, null)
                },
                text = { Text(stringResource(if (cleaning) R.string.cleaning else R.string.quick_clean)) }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, null) }, label = { Text(stringResource(t.titleRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> HomeScreen(vm)
                Tab.PROCESSES -> ProcessesScreen(vm)
                Tab.WHITELIST -> WhiteListScreen(vm)
                Tab.MODES -> ModesScreen(vm)
            }
        }
    }
}
