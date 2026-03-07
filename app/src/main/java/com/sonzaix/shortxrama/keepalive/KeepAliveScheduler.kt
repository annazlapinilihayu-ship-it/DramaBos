package com.sonzaix.shortxrama.keepalive

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.os.Build
import java.util.concurrent.TimeUnit

object KeepAliveScheduler {
    private const val WORK_NAME = "shortxrama_keep_alive"

    fun schedule(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return
        }
        val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
