package com.sonzaix.shortxrama.keepalive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class KeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        KeepAliveService.start(applicationContext)
        return Result.success()
    }
}
