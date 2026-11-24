package com.darshan.focusfeed

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val prefs by lazy { getSharedPreferences("ff", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        Scheduler.start(this)
        startForegroundService(Intent(this, WatchService::class.java))
    }

    inner class Bridge {
        @JavascriptInterface fun hasKey(): Boolean = (prefs.getString("key", "") ?: "").isNotEmpty()
        @JavascriptInterface fun setKey(k: String) { prefs.edit().putString("key", k.trim()).apply() }

        @JavascriptInterface fun ask(id: String, prompt: String) {
            val key = prefs.getString("key", "") ?: ""
            Thread {
                val r = try { Ai.ask(key, prompt) } catch (e: Exception) { "ERR:" + (e.message ?: "network error") }
                runOnUiThread {
                    web.evaluateJavascript("window.__ai(" + JSONObject.quote(id) + "," + JSONObject.quote(r) + ")", null)
                }
            }.start()
        }

        @JavascriptInterface fun share(name: String, text: String) {
            runOnUiThread {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, name).putExtra(Intent.EXTRA_TEXT, text)
                startActivity(Intent.createChooser(i, name))
            }
        }

        @JavascriptInterface fun openUsage() { runOnUiThread { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } }
        @JavascriptInterface fun openBattery() { runOnUiThread { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
    }
}
