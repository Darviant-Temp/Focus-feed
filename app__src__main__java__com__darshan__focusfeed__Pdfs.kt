package com.darshan.focusfeed

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object Pdfs {
    private fun dir(c: Context): File = File(c.filesDir, "pdfs").apply { mkdirs() }
    private fun prefs(c: Context) = c.getSharedPreferences("ff", Context.MODE_PRIVATE)

    fun file(c: Context, id: String): File = File(dir(c), "$id.pdf")

    fun list(c: Context): JSONArray =
        try { JSONArray(prefs(c).getString("pdfs", "[]")) } catch (e: Exception) { JSONArray() }

    fun add(c: Context, uri: Uri) {
        var name = "document"
        c.contentResolver.query(uri, null, null, null, null)?.use { cur ->
            val i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cur.moveToFirst() && i >= 0) name = cur.getString(i)
        }
        val id = System.currentTimeMillis().toString()
        c.contentResolver.openInputStream(uri)!!.use { input ->
            file(c, id).outputStream().use { out -> input.copyTo(out) }
        }
        val l = list(c)
        l.put(JSONObject().put("id", id).put("n", name.removeSuffix(".pdf")))
        prefs(c).edit().putString("pdfs", l.toString()).apply()
    }

    fun delete(c: Context, id: String) {
        file(c, id).delete()
        val l = list(c)
        val n = JSONArray()
        for (i in 0 until l.length()) {
            if (l.getJSONObject(i).getString("id") != id) n.put(l.getJSONObject(i))
        }
        prefs(c).edit().putString("pdfs", n.toString()).apply()
    }
}
