package com.darshan.focusfeed

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NudgeWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result {
        if (Config.inWindow(Calendar.getInstance())) {
            Notifier.show(applicationContext, "Time to learn", null, 1)
        }
        Scheduler.next(applicationContext)
        return Result.success()
    }
}

object Scheduler {
    fun start(c: Context) = enqueue(c, ExistingWorkPolicy.KEEP)
    fun next(c: Context) = enqueue(c, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(c: Context, policy: ExistingWorkPolicy) {
        val d = (Config.MIN_GAP..Config.MAX_GAP).random().toLong()
        val r = OneTimeWorkRequestBuilder<NudgeWorker>().setInitialDelay(d, TimeUnit.MINUTES).build()
        WorkManager.getInstance(c).enqueueUniqueWork("nudge", policy, r)
    }
}
