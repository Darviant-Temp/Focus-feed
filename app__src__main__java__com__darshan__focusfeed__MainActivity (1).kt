package com.darshan.focusfeed

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    private val pickPdf = 7

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickPdf || resultCode != RESULT_OK || data == null) return
        val uris = ArrayList<Uri>()
        val clip = data.clipData
        if (clip != null) {
            for (k in 0 until clip.itemCount) uris.add(clip.getItemAt(k).uri)
        } else {
            data.data?.let { uris.add(it) }
        }
        val act = this
        Thread {
            for (u in uris) {
                try { Pdfs.add(act, u) } catch (e: Exception) { }
            }
            runOnUiThread { web.evaluateJavascript("window.__pdfAdded&&window.__pdfAdded()", null) }
        }.start()
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

        @JavascriptInterface fun pdfPick() {
            runOnUiThread {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                startActivityForResult(i, pickPdf)
            }
        }

        @JavascriptInterface fun pdfList(): String = Pdfs.list(this@MainActivity).toString()
        @JavascriptInterface fun pdfDelete(id: String) { Pdfs.delete(this@MainActivity, id) }
        @JavascriptInterface fun pdfOpen(id: String) {
            runOnUiThread { startActivity(Intent(this@MainActivity, PdfActivity::class.java).putExtra("id", id)) }
        }
    }
}
