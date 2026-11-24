package com.darshan.focusfeed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Scheduler.start(c)
        try { c.startForegroundService(Intent(c, WatchService::class.java)) } catch (e: Exception) { }
    }
}
