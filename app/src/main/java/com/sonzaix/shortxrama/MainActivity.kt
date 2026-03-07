package com.sonzaix.shortxrama

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.sonzaix.shortxrama.ui.navigation.DramaApp
import com.sonzaix.shortxrama.keepalive.KeepAliveService
import com.sonzaix.shortxrama.data.AppSettingsStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                KeepAliveService.start(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            DramaApp()
        }

        lifecycleScope.launch {
            val settingsStore = AppSettingsStore(this@MainActivity)
            val enabled = settingsStore.settingsFlow.first().keepAliveEnabled
            if (enabled) {
                ensureKeepAlive()
            } else {
                KeepAliveService.stop(this@MainActivity)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Ensure cleanup on destroy
        if (!isChangingConfigurations) {
            // App is being destroyed, cleanup will be handled by composables
        }
    }

    private fun ensureKeepAlive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        KeepAliveService.start(this)
    }
}