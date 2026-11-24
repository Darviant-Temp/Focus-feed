package com.darshan.focusfeed

import android.app.AppOpsManager
import android.content.Context

object Util {
    fun usageGranted(c: Context): Boolean {
        val a = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val m = a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), c.packageName)
        return m == AppOpsManager.MODE_ALLOWED
    }
}
