package com.sonzaix.shortxrama

import android.app.Application
import android.util.Log
import com.sonzaix.shortxrama.data.AppSettingsStore
import com.sonzaix.shortxrama.keepalive.KeepAliveScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShortXramaApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                val settings = AppSettingsStore(this@ShortXramaApp).settingsFlow.first()
                if (settings.keepAliveEnabled) {
                    KeepAliveScheduler.schedule(this@ShortXramaApp)
                } else {
                    KeepAliveScheduler.cancel(this@ShortXramaApp)
                }
            } catch (e: Exception) {
                Log.e("ShortXramaApp", "Failed to initialize keep-alive", e)
            }
        }
    }
}
