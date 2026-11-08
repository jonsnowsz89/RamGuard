package com.ramguard.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramguard.app.R
import com.ramguard.app.ui.screens.HomeScreen
import com.ramguard.app.ui.screens.ModesScreen
import com.ramguard.app.ui.screens.ProcessesScreen
import com.ramguard.app.ui.screens.WhiteListScreen

private class NavItem(val titleRes: Int, val icon: ImageVector)

private val navItems = listOf(
    NavItem(R.string.tab_home, Icons.Rounded.Home),
    NavItem(R.string.tab_processes, Icons.Rounded.Memory),
    NavItem(R.string.tab_whitelist, Icons.Rounded.Shield),
    NavItem(R.string.tab_modes, Icons.Rounded.Eco)
)

@Composable
fun RamGuardRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val cleaning by vm.cleaning.collectAsState()
    val result by vm.lastResult.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }

    // Re-sync Do-Not-Disturb whenever the app resumes (e.g. right after granting DND access)
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.syncDnd()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
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
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "scale"
    )

    Scaffold(
        containerColor = Color.Black,
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
                    if (cleaning) {
                        CircularProgressIndicator(
                            Modifier.size(22.dp), strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Bolt, null)
                    }
                },
                text = { Text(stringResource(if (cleaning) R.string.cleaning else R.string.quick_clean)) }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, null) },
                        label = { Text(stringResource(item.titleRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(vm)
                1 -> ProcessesScreen(vm)
                2 -> WhiteListScreen(vm)
                else -> ModesScreen(vm)
            }
        }
    }
}
