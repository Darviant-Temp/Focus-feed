package com.darshan.focusfeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.Calendar

class WatchService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private var unlockTask: Runnable? = null
    private var screenOn = true
    private var fg = ""
    private var ytSince = 0L
    private var ytDone = false
    private var lastNudge = 0L

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_USER_PRESENT -> { screenOn = true; scheduleUnlock() }
                Intent.ACTION_SCREEN_ON -> screenOn = true
                Intent.ACTION_SCREEN_OFF -> { screenOn = false; cancelUnlock(); ytSince = 0L; ytDone = false }
            }
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            if (screenOn) checkYoutube()
            h.postDelayed(this, 10000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("watch", "Background watcher", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "watch")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Focus Feed is on")
            .setContentText("Watching for study nudges")
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(2, n)
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(rx, f)
        h.post(poll)
    }

    private fun scheduleUnlock() {
        cancelUnlock()
        val mins = Config.UNLOCK_DELAY_MIN + (-2..2).random()
        val t = Runnable { nudge("Ready for 15 minutes?", "You have been on your phone a while. Learn something now.", 3) }
        unlockTask = t
        h.postDelayed(t, mins * 60000L)
    }

    private fun cancelUnlock() {
        unlockTask?.let { h.removeCallbacks(it) }
        unlockTask = null
    }

    private fun nudge(title: String, text: String, id: Int) {
        val now = System.currentTimeMillis()
        if (!screenOn || fg == packageName || !Config.awake(Calendar.getInstance()) || now - lastNudge < 10 * 60000L) return
        lastNudge = now
        Notifier.show(this, title, text, id)
    }

    private fun checkYoutube() {
        if (!Util.usageGranted(this)) return
        val um = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val ev = um.queryEvents(now - 30000, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) fg = e.packageName
        }
        if (fg == "com.google.android.youtube") {
            if (ytSince == 0L) ytSince = now
            else if (!ytDone && now - ytSince >= Config.YOUTUBE_DELAY_MIN * 60000L) {
                ytDone = true
                nudge("Pause YouTube?", "Read one card first. Your future self will thank you.", 4)
            }
        } else {
            ytSince = 0L
            ytDone = false
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        try { unregisterReceiver(rx) } catch (e: Exception) { }
        super.onDestroy()
    }
}
