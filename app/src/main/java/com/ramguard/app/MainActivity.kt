package com.ramguard.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.ramguard.app.service.CleanerService
import com.ramguard.app.ui.RamGuardRoot
import com.ramguard.app.ui.theme.RamGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                // Start (or restart) the service once the user answered
                ContextCompat.startForegroundService(this, Intent(this, CleanerService::class.java))
            }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, CleanerService::class.java))
            }
            RamGuardTheme { RamGuardRoot() }
        }
    }
}
